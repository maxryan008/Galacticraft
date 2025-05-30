package dev.galacticraft.mod.machine.multiblock;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.block.entity.PipeColor;
import dev.galacticraft.mod.api.pipe.Pipe;
import dev.galacticraft.mod.api.pipe.PipeNetwork;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.machine.multiblock.multiblocks.FluidTankMultiblock;
import dev.galacticraft.mod.machine.multiblock.multiblocks.PersistentContainerMultiblock;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

public class ValveBlockEntity extends BlockEntity {
    private ValveMode mode = ValveMode.INPUT;
    private PersistentContainerMultiblock<?> multiblock;

    public ValveBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.VALVE, pos, state);
        this.mode = state.getValue(ValveBlock.MODE);
    }

    public void setMultiblock(PersistentContainerMultiblock<?> multiblock) {
        this.multiblock = multiblock;
    }

    public void tick() {
        if (this.level.isClientSide || this.mode != ValveMode.OUTPUT || multiblock == null) return;

        if (multiblock instanceof FluidTankMultiblock fluidTank) {
            // Ask each connected pipe if it wants fluid — this means
            // this valve behaves like a *source*, not a sink
            FluidTankMultiblock.FluidContent fluid = fluidTank.getStoredData();
            if (fluid.amount() > 0) {
                try (Transaction tx = Transaction.openOuter()) {
                    long inserted = 0;
                    FluidVariant variant = FluidVariant.of(fluid.fluid());

                    for (Direction dir : Constant.Misc.DIRECTIONS) {
                        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
                        if (storage != null && storage.supportsInsertion()) {
                            inserted += storage.insert(variant, fluid.amount(), tx);
                            if (inserted >= fluid.amount()) break;
                        }
                    }

                    if (inserted > 0) {
                        fluidTank.setStoredData(new FluidTankMultiblock.FluidContent(fluid.fluid(), fluid.amount() - (int) inserted));
                        tx.commit();
                    }
                }
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    public Storage<FluidVariant> getFluidStorage(Direction side) {
        if (mode != ValveMode.INPUT || multiblock == null) return null;

        if (multiblock instanceof FluidTankMultiblock fluidTank) {
            return new Storage<>() {
                @Override
                public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return fluidTank.tryInsert(new FluidTankMultiblock.FluidContent(resource.getFluid(), (int) maxAmount), false);
                }

                @Override
                public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                    return 0;
                }

                @Override
                public boolean supportsInsertion() {
                    return true;
                }

                @Override
                public boolean supportsExtraction() {
                    return false;
                }

                @Override
                public Iterator<StorageView<FluidVariant>> iterator() {
                    return Collections.emptyIterator();
                }
            };
        }

        return null;
    }
}