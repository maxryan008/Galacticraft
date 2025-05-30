package dev.galacticraft.mod.machine.multiblock;

import net.minecraft.core.BlockPos;

public record FormedMultiblockInstance(MultiblockShell shell, BlockPos min, BlockPos max) {
    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }
}