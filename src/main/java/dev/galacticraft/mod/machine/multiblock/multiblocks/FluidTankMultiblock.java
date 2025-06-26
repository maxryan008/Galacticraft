package dev.galacticraft.mod.machine.multiblock.multiblocks;

import dev.galacticraft.mod.client.render.TransparentQuadInjector;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.machine.multiblock.MultiblockShell;
import dev.galacticraft.mod.machine.multiblock.ShellBlockRule;
import dev.galacticraft.mod.machine.multiblock.ShellComponent;
import dev.galacticraft.mod.mixin.BucketItemAccessor;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.awt.*;
import java.util.List;

public class FluidTankMultiblock extends PersistentContainerMultiblock<FluidTankMultiblock.FluidContent> {

    private long maxCapacity = 0; // set onFormed()

    public FluidContent getStored() {
        return this.storedData;
    }

    public void modifyStoredAmount(int change) {
        this.storedData = this.storedData.modifyStoredAmount(change);
    }

    public record FluidContent(Fluid fluid, long amount) {
        public static final FluidContent EMPTY = new FluidContent(Fluids.EMPTY, 0);

        public FluidContent modifyStoredAmount(int change) {
            return new FluidContent(this.fluid, this.amount + change);
        }
    }

    public FluidTankMultiblock() {
        super(10, 10, 10, List.of(
                new ShellBlockRule(ShellComponent.CORNER, s -> s.is(GCBlocks.FLUID_TANK_CASING)),
                new ShellBlockRule(ShellComponent.EDGE, s -> s.is(GCBlocks.FLUID_TANK_CASING)),
                new ShellBlockRule(ShellComponent.FACE, s -> s.is(GCBlocks.FLUID_TANK_CASING) || s.is(GCBlocks.TEMPERED_GLASS) || s.is(GCBlocks.VALVE) || s.is(GCBlocks.CAPACITY_DISPLAY)),
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
        this.maxCapacity = FluidUtil.bucketsToDroplets(interiorBlocks);

        super.onFormed(level, min, max, valves);

        FluidState fluidState = Fluids.WATER.defaultFluidState();
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());
        if (handler == null) return;

        TextureAtlasSprite[] sprites = handler.getFluidSprites(level, min, fluidState);
        if (sprites == null || sprites.length == 0 || sprites[0] == null) return;

        TextureAtlasSprite sprite = sprites[0];

        updateFluidQuads(level, min, max);
    }

    public long getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public FluidContent tryExtract(long maxAmount, boolean simulate) {
        if (storedData.amount() == 0) return FluidContent.EMPTY;

        long toExtract = Math.min(maxAmount, storedData.amount());
        if (!simulate) {
            storedData = new FluidContent(storedData.fluid(), storedData.amount() - toExtract);
        }

        Level level = this.getLevel();
        if (!simulate && level != null) {
            updateFluidQuads(level, this.getMin(), this.getMax());
        }

        return new FluidContent(storedData.fluid(), toExtract);
    }

    @Override
    public long tryInsert(FluidContent value, boolean simulate) {
        if (value.amount() <= 0) return 0;

        // If currently not holding a fluid or amount is zero, accept any new fluid
        if (storedData.amount() == 0 || storedData.fluid() == Fluids.EMPTY) {
            long toInsert = Math.min(value.amount(), maxCapacity);
            if (!simulate && toInsert > 0) {
                storedData = new FluidContent(value.fluid(), toInsert);
            }
            return toInsert;
        }

        // Otherwise, only allow same fluid type
        if (!storedData.fluid().isSame(value.fluid())) return 0;

        long canInsert = Math.min(value.amount(), maxCapacity - storedData.amount());
        if (!simulate && canInsert > 0) {
            storedData = new FluidContent(storedData.fluid(), storedData.amount() + canInsert);
        }

        Level level = this.getLevel();
        if (!simulate && level != null) {
            updateFluidQuads(level, this.getMin(), this.getMax());
        }

        return canInsert;
    }

    @Override
    public void onBroken(Level level, BlockPos origin) {
        super.onBroken(level, origin);
        TransparentQuadInjector.removeQuadsInRegion(level, this.getMin(), this.getMax());
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
                    if (this.tryInsert(new FluidContent(bucketFluid, FluidUtil.bucketsToDroplets(1)), false) == FluidUtil.bucketsToDroplets(1)) {
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

            if (stored.amount() >= FluidUtil.bucketsToDroplets(1) && stored.fluid().getBucket() != Items.BUCKET) {
                ItemStack fullBucket = new ItemStack(stored.fluid().getBucket());

                this.tryExtract(FluidUtil.bucketsToDroplets(1), false);

                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                if (!player.getInventory().add(fullBucket)) {
                    player.drop(fullBucket, false);
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(String.valueOf(this.storedData.amount)));
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

    private void updateFluidQuads(Level level, BlockPos min, BlockPos max) {
        // Remove existing quads within the tank bounds
        TransparentQuadInjector.removeQuadsInRegion(level, min, max);

        if (this.storedData.amount() <= 0 || this.maxCapacity <= 0 || this.storedData.fluid() == Fluids.EMPTY)
            return;

        // Calculate fluid fill ratio and height
        float fillRatio = Mth.clamp((float) this.storedData.amount() / (float) this.maxCapacity, 0.0f, 1.0f);
        if (fillRatio <= 0.001f) return; // no fluid to display

        int tankHeight = max.getY() - min.getY();
        int internalHeight = tankHeight - 1;
        float fluidLevelY = min.getY() + 1 + (internalHeight * fillRatio);

        // Get fluid sprite
        FluidState fluidState = this.storedData.fluid().defaultFluidState();
        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());
        if (handler == null) return;

        TextureAtlasSprite[] sprites = handler.getFluidSprites(level, min, fluidState);
        if (sprites == null || sprites.length == 0 || sprites[0] == null) return;
        TextureAtlasSprite sprite = sprites[0];

        // Quad appearance
        int light = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;
        int color = 0x88FFFFFF;

        // Iterate over the interior XZ plane of the tank
        for (int x = min.getX() + 1; x < max.getX(); x++) {
            for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                // Create top face at fluidLevelY
                TransparentQuadInjector.InjectedQuad quad = TransparentQuadInjector.faceQuad(
                        x, fluidLevelY, z,
                        x + 1, fluidLevelY, z + 1,
                        sprite, Direction.UP, color, light, overlay
                );
                TransparentQuadInjector.addQuad(level, quad);
            }
        }
    }

    @Override
    public void restored() {
        updateFluidQuads(getLevel(), getMin(), getMax());
    }
}