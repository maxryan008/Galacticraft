package dev.galacticraft.mod.machine.multiblock.multiblocks;

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.machine.multiblock.MultiblockShell;
import dev.galacticraft.mod.machine.multiblock.ShellBlockRule;
import dev.galacticraft.mod.machine.multiblock.ShellComponent;
import dev.galacticraft.mod.mixin.BucketItemAccessor;
import dev.galacticraft.mod.mixin.BucketItemMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public class FluidTankMultiblock extends PersistentContainerMultiblock<FluidTankMultiblock.FluidContent> {
    public static final int DEFAULT_MB_PER_BLOCK = 1000; // configurable if needed

    private int maxCapacity = 0; // set onFormed()

    public record FluidContent(Fluid fluid, int amount) {
        public static final FluidContent EMPTY = new FluidContent(Fluids.EMPTY, 0);
    }

    public FluidTankMultiblock() {
        super(10, 10, 10, List.of(
                new ShellBlockRule(ShellComponent.CORNER, s -> s.is(Blocks.IRON_BLOCK)),
                new ShellBlockRule(ShellComponent.EDGE, s -> s.is(Blocks.IRON_BLOCK)),
                new ShellBlockRule(ShellComponent.FACE, s -> s.is(Blocks.IRON_BLOCK) || s.is(Blocks.GLASS) || s.is(GCBlocks.VALVE)),
                new ShellBlockRule(ShellComponent.INTERIOR, BlockState::isAir)
        ), () -> FluidContent.EMPTY);
    }

    @Override
    public FluidTankMultiblock newInstance() {
        return new FluidTankMultiblock();
    }

    @Override
    public void onFormed(Level level, BlockPos min, BlockPos max, List<BlockPos> valves) {
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;

        int interiorBlocks = Math.max(0, (sizeX - 2)) * Math.max(0, (sizeY - 2)) * Math.max(0, (sizeZ - 2));
        this.maxCapacity = interiorBlocks * DEFAULT_MB_PER_BLOCK;

        super.onFormed(level, min, max, valves);
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public FluidContent tryExtract(int maxAmount, boolean simulate) {
        if (storedData.amount() == 0) return FluidContent.EMPTY;

        int toExtract = Math.min(maxAmount, storedData.amount());
        if (!simulate) {
            storedData = new FluidContent(storedData.fluid(), storedData.amount() - toExtract);
        }

        return new FluidContent(storedData.fluid(), toExtract);
    }

    @Override
    public int tryInsert(FluidContent value, boolean simulate) {
        if (value.amount() <= 0) return 0;

        // If currently not holding a fluid or amount is zero, accept any new fluid
        if (storedData.amount() == 0 || storedData.fluid() == Fluids.EMPTY) {
            int toInsert = Math.min(value.amount(), maxCapacity);
            if (!simulate && toInsert > 0) {
                storedData = new FluidContent(value.fluid(), toInsert);
            }
            return toInsert;
        }

        // Otherwise, only allow same fluid type
        if (!storedData.fluid().isSame(value.fluid())) return 0;

        int canInsert = Math.min(value.amount(), maxCapacity - storedData.amount());
        if (!simulate && canInsert > 0) {
            storedData = new FluidContent(storedData.fluid(), storedData.amount() + canInsert);
        }

        return canInsert;
    }

    @Override
    public void onBroken(Level level, BlockPos origin) {
        super.onBroken(level, origin);
    }

    @Override
    public boolean onClicked(Level level, BlockPos clickedBlock, BlockPos clickedPos, InteractionHand interactionHand, Player player) {
        if (player == null || level.isClientSide) return true;

        if (this.valvePositions.contains(clickedBlock)) {
            return false;
        }

        ItemStack heldItem = player.getItemInHand(interactionHand);

        // Try inserting fluid from a full bucket
        if (heldItem.getItem() instanceof BucketItem bucketItem) {
            Fluid bucketFluid = ((BucketItemAccessor) bucketItem).getFluid();

            if (!bucketFluid.isSame(Fluids.EMPTY)) {
                if (this.storedData.amount() == 0 || this.storedData.fluid().isSame(bucketFluid)) {
                    if (this.tryInsert(new FluidContent(bucketFluid, 1000), false) == 1000) {
                        if (!player.isCreative()) {
                            player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
                        }
                    }
                }
                return true;
            }
        }

        // Try extracting fluid into an empty bucket
        if (heldItem.is(Items.BUCKET)) {
            FluidContent stored = this.storedData;

            if (stored.amount() >= 1000 && stored.fluid().getBucket() != Items.BUCKET) {
                ItemStack fullBucket = new ItemStack(stored.fluid().getBucket());

                this.tryExtract(1000, false);

                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                if (!player.getInventory().add(fullBucket)) {
                    player.drop(fullBucket, false);
                }
            }
        }
        return true;
    }

    @Override
    public void copyFrom(MultiblockShell other) {
        if (other instanceof FluidTankMultiblock tank) {
            this.storedData = tank.storedData;
            this.maxCapacity = tank.maxCapacity;
        }
    }
}