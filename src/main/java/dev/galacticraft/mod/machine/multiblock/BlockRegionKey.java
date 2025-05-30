package dev.galacticraft.mod.machine.multiblock;

import net.minecraft.core.BlockPos;

public record BlockRegionKey(BlockPos min, BlockPos max) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockRegionKey(BlockPos min1, BlockPos max1))) return false;
        return min.equals(min1) && max.equals(max1);
    }

    @Override
    public int hashCode() {
        return 31 * min.hashCode() + max.hashCode();
    }
}