package dev.galacticraft.mod.machine.multiblock;

import com.google.common.base.Predicates;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.machine.multiblock.multiblocks.FluidTankMultiblock;
import dev.galacticraft.mod.machine.multiblock.multiblocks.PersistentContainerMultiblock;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

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

    public void tick(BlockState state) {
        if (this.level.isClientSide || this.mode != ValveMode.OUTPUT || multiblock == null) return;

        if (multiblock instanceof FluidTankMultiblock fluidTank) {
            // Ask each connected pipe if it wants fluid — this means
            // this valve behaves like a *source*, not a sink
            FluidTankMultiblock.FluidContent fluid = fluidTank.getStoredData();
            if (fluid.amount() > 0) {
                for (Direction dir : Constant.Misc.DIRECTIONS) {
                    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
                    if (storage != null && storage.supportsInsertion()) {
                        System.out.println(StorageUtil.move(this.getFluidStorage(), storage, Predicates.alwaysTrue(), FluidConstants.BUCKET, null));
                    }
                }
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    public Storage<FluidVariant> getFluidStorage() {
        if (multiblock == null) return null;

        if (multiblock instanceof FluidTankMultiblock fluidTank) {
            ValveBlockEntity self = this;

            return new Storage<>() {
                final FluidVariant resource = FluidVariant.of(fluidTank.getStored().fluid());
                final long amount = fluidTank.getStored().amount();

                @Override
                public long insert(FluidVariant incoming, long maxAmount, TransactionContext transaction) {
                    if (!resource.equals(incoming) && !resource.getFluid().equals(Fluids.EMPTY)) return 0;
                    if (self.mode != ValveMode.INPUT) return 0;

                    int accepted = fluidTank.tryInsert(new FluidTankMultiblock.FluidContent(incoming.getFluid(), (int) maxAmount), true);

                    if (accepted > 0) {
                        // Delay actual mutation until commit
                        transaction.addCloseCallback((ctx, result) -> {
                            if (result.wasCommitted()) {
                                fluidTank.tryInsert(new FluidTankMultiblock.FluidContent(incoming.getFluid(), accepted), false);
                            }
                        });
                    }

                    return accepted;
                }

                @Override
                public long extract(FluidVariant outgoing, long maxAmount, TransactionContext transaction) {
                    if (!resource.equals(outgoing)) return 0;
                    if (self.mode != ValveMode.OUTPUT) return 0;

                    int extracted = fluidTank.tryExtract((int) maxAmount, true).amount();

                    if (extracted > 0) {
                        transaction.addCloseCallback((ctx, result) -> {
                            if (result.wasCommitted()) {
                                fluidTank.tryExtract(extracted, false);
                            }
                        });
                    }

                    return extracted;
                }

                @Override
                public boolean supportsInsertion() {
                    return self.mode == ValveMode.INPUT;
                }

                @Override
                public boolean supportsExtraction() {
                    return true; // allow connection; return 0 in extract() when mode != OUTPUT
                }

                @Override
                public Iterator<StorageView<FluidVariant>> iterator() {
                    return Collections.singletonList(new StorageView<FluidVariant>() {

                        @Override
                        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                            return ValveBlockEntity.this.getFluidStorage().extract(resource, maxAmount, transaction);
                        }

                        @Override
                        public boolean isResourceBlank() {
                            return resource.isBlank() || amount <= 0;
                        }

                        @Override
                        public FluidVariant getResource() {
                            return resource;
                        }

                        @Override
                        public long getAmount() {
                            return amount;
                        }

                        @Override
                        public long getCapacity() {
                            return fluidTank.getMaxCapacity();
                        }
                    }.getUnderlyingView()).iterator();
                }
            };
        }

        return null;
    }

    public void setMode(ValveMode mode) {
        this.mode = mode;
    }
}