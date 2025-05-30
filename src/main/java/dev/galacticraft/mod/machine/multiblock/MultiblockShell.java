package dev.galacticraft.mod.machine.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class MultiblockShell {
    protected BlockPos lastMin = null;
    protected BlockPos lastMax = null;
    protected boolean formed = false;
    protected final int maxX, maxY, maxZ;
    protected final List<ShellBlockRule> rules;

    public MultiblockShell(int maxX, int maxY, int maxZ, List<ShellBlockRule> rules) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.rules = rules;
    }

    public BlockPos tryFormAndGetMin(Level level, BlockPos origin) {
        int halfX = maxX / 2, halfY = maxY / 2, halfZ = maxZ / 2;

        for (int dx = -halfX; dx <= 0; dx++) {
            for (int dy = -halfY; dy <= 0; dy++) {
                for (int dz = -halfZ; dz <= 0; dz++) {
                    for (int sizeX = 3; sizeX <= maxX; sizeX++) {
                        for (int sizeY = 3; sizeY <= maxY; sizeY++) {
                            for (int sizeZ = 3; sizeZ <= maxZ; sizeZ++) {
                                BlockPos min = origin.offset(dx, dy, dz);
                                BlockPos max = min.offset(sizeX - 1, sizeY - 1, sizeZ - 1);

                                if (min.getX() <= origin.getX() && max.getX() >= origin.getX()
                                        && min.getY() <= origin.getY() && max.getY() >= origin.getY()
                                        && min.getZ() <= origin.getZ() && max.getZ() >= origin.getZ()) {
                                    ShellScanner.ValidShell shell = ShellScanner.isValidShell(level, min, max, rules);
                                    if (shell.valid()) {
                                        onFormed(level, min, max, shell.valves());
                                        return min;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public abstract void onFormed(Level level, BlockPos min, BlockPos max, List<BlockPos> valves);
    public abstract void onBroken(Level level, BlockPos origin);
    public abstract boolean onClicked(Level level, BlockPos clickedBlock, BlockPos clickedPos, InteractionHand interactionHand, Player player);
}