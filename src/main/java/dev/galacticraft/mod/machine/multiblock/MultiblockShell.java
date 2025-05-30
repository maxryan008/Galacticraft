package dev.galacticraft.mod.machine.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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

    public abstract MultiblockShell newInstance();

    public record MultiblockFormResult(BlockPos min, int xSize, int ySize, int zSize) {}

    public MultiblockFormResult tryForm(Level level, BlockPos origin) {
        ShellComponent role = testBlock(level, origin);
        if (role != null) {
            if (role == ShellComponent.INTERIOR) {
                return tryFormFromInterior(level, origin, role);
            } else {
                ShellBlockRule interiorRule = rules.stream()
                        .filter(rule -> rule.component() == ShellComponent.INTERIOR)
                        .findFirst()
                        .orElseThrow();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue; // skip the origin itself
                            BlockPos neighbor = origin.offset(dx, dy, dz);
                            if (interiorRule.predicate().test(level.getBlockState(neighbor))) {
                                MultiblockFormResult multiblockFormResult = tryFormFromInterior(level, neighbor, ShellComponent.INTERIOR);
                                if (multiblockFormResult != null) {
                                    return multiblockFormResult;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private MultiblockFormResult tryFormFromInterior(Level level, BlockPos origin, ShellComponent role) {
        int x = 0;
        int y = 0;
        int z = 0;
        while (role == ShellComponent.INTERIOR) {
            y -= 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (y < -maxY) {
                return null;
            }
        }
        //we now know where the faces min y level is. double check this block is not part of any formed multiblock
        if (MultiblockRegistry.isActive(level, origin.offset(x,y,z))) return null;
        y += 1;
        role = ShellComponent.INTERIOR;
        while (role == ShellComponent.INTERIOR) {
            x -= 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (x < -maxX) {
                return null;
            }
        }
        //we now know where the edges min y min x is. double check no formed multiblocks part of it.
        if (MultiblockRegistry.isActive(level, origin.offset(x,y - 1,z))) return null;
        x += 1;
        role = ShellComponent.INTERIOR;
        while (role == ShellComponent.INTERIOR) {
            z -= 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (z < -maxZ) {
                return null;
            }
        }
        //we now know where the minimum corner of the shape is. double check no former multiblocks part of it
        if (MultiblockRegistry.isActive(level, origin.offset(x - 1,y - 1,z))) return null;
        BlockPos minimumCorner = origin.offset(x - 1,y - 1,z);
        z += 1;
        role = ShellComponent.INTERIOR;
        while (role == ShellComponent.INTERIOR) {
            x += 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (x > maxX) {
                return null;
            }
        }
        //found wall so continue
        x -= 1;
        role = ShellComponent.INTERIOR;
        while (role == ShellComponent.INTERIOR) {
            z += 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (z > maxZ) {
                return null;
            }
        }
        x += 1;
        //found max xz lower corner
        if (MultiblockRegistry.isActive(level, origin.offset(x,y - 1,z))) return null;
        BlockPos maxXZLowerCorner = origin.offset(x,y - 1,z);
        role = ShellComponent.INTERIOR;
        x -= 1;
        z -= 1;
        while (role == ShellComponent.INTERIOR) {
            x -= 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (x < -maxX) {
                return null;
            }
        }
        //found lower min x max z corner
        if (MultiblockRegistry.isActive(level, origin.offset(x,y - 1,z + 1))) return null;
        BlockPos minXMaxZLowerCorner = origin.offset(x,y - 1,z + 1);
        role = ShellComponent.INTERIOR;
        x += 1;
        while (role == ShellComponent.INTERIOR) {
            x += 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (x > maxX) {
                return null;
            }
        }
        //found wall
        x -= 1;
        role = ShellComponent.INTERIOR;
        while (role == ShellComponent.INTERIOR) {
            z -= 1;
            role = testBlock(level, origin.offset(x, y, z));
            if (z < -maxZ) {
                return null;
            }
        }
        x += 1;
        //found lower min z max x corner
        BlockPos maxXMinZLowerCorner = origin.offset(x,y - 1,z);
        if (MultiblockRegistry.isActive(level, origin.offset(x,y - 1,z))) return null;
        //found all lower face corners. now determine if face is valid
        if (!isValidFace(level, minimumCorner, maxXZLowerCorner, minXMaxZLowerCorner, maxXMinZLowerCorner, this.rules)) return null;
        x = 0;
        y = 0;
        z = 0;
        boolean found = false;
        boolean exit = false;
        while (!found && !exit) {
            y += 1;
            found = isValidFace(level, minimumCorner.offset(x,y,z), maxXZLowerCorner.offset(x,y,z), minXMaxZLowerCorner.offset(x,y,z), maxXMinZLowerCorner.offset(x,y,z), this.rules);
            if (!isValidSlice(level, minimumCorner.offset(x,y,z), maxXZLowerCorner.offset(x,y,z), rules)) exit = true; //exit while loop and try other direction
            if (y > maxY) {
                exit = true;
            }
        }
        if (exit && !found) {
            y = 0;
            while (!found) {
                y -= 1;
                found = isValidFace(level, minimumCorner.offset(x,y,z), maxXZLowerCorner.offset(x,y,z), minXMaxZLowerCorner.offset(x,y,z), maxXMinZLowerCorner.offset(x,y,z), this.rules);
                if (!isValidSlice(level, minimumCorner.offset(x,y,z), maxXZLowerCorner.offset(x,y,z), rules)) return null; //no valid multiblock
                if (y < -maxY) {
                    return null;
                }
            }
        }
        int xSize = maxXZLowerCorner.getX() - minimumCorner.getX() + 1;
        int ySize = Math.abs(y) + 1;
        int zSize = maxXZLowerCorner.getZ() - minimumCorner.getZ() + 1;
        System.out.println("X: " + xSize + " Y: " + ySize + " Z: " + zSize);
        onFormed(level, minimumCorner, minimumCorner.offset(xSize - 1, ySize - 1, zSize - 1), List.of());
        return new MultiblockFormResult(minimumCorner, xSize, ySize, zSize);
    }

    public ShellComponent testBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        for (ShellBlockRule rule : rules) {
            if (rule.predicate().test(state)) {
                return rule.component();
            }
        }

        return null;
    }

    public static boolean isValidFace(Level level, BlockPos p1, BlockPos p2, BlockPos p3, BlockPos p4, List<ShellBlockRule> rules) {
        BlockPos min = new BlockPos(
                Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX())),
                Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY())),
                Math.min(Math.min(p1.getZ(), p2.getZ()), Math.min(p3.getZ(), p4.getZ()))
        );

        BlockPos max = new BlockPos(
                Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX())),
                Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY())),
                Math.max(Math.max(p1.getZ(), p2.getZ()), Math.max(p3.getZ(), p4.getZ()))
        );

        Direction.Axis fixedAxis = null;
        if (min.getX() == max.getX()) fixedAxis = Direction.Axis.X;
        else if (min.getY() == max.getY()) fixedAxis = Direction.Axis.Y;
        else if (min.getZ() == max.getZ()) fixedAxis = Direction.Axis.Z;
        else return false; // Not a flat face

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    ShellComponent expected = classifyShellComponent(pos, min, max);

                    if (!isValidShellComponent(level.getBlockState(pos), expected, rules)) return false;
                }
            }
        }

        return true;
    }

    public static boolean isValidSlice(Level level, BlockPos min, BlockPos max, List<ShellBlockRule> rules) {
        ShellBlockRule interiorRule = rules.stream()
                .filter(rule -> rule.component() == ShellComponent.INTERIOR)
                .findFirst()
                .orElseThrow();
        ShellBlockRule faceRule = rules.stream()
                .filter(rule -> rule.component() == ShellComponent.FACE)
                .findFirst()
                .orElseThrow();
        ShellBlockRule edge = rules.stream()
                .filter(rule -> rule.component() == ShellComponent.EDGE)
                .findFirst()
                .orElseThrow();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    ShellComponent expected = classifySliceComponent(pos, min, max);

                    if (expected == ShellComponent.INTERIOR) {
                        if (!interiorRule.predicate().test(level.getBlockState(pos))) return false;
                    }
                    else if (expected == ShellComponent.FACE) {
                        if (!faceRule.predicate().test(level.getBlockState(pos))) return false;
                    }
                    else if (expected == ShellComponent.EDGE) {
                        if (!edge.predicate().test(level.getBlockState(pos))) return false;
                    } else {
                        throw new IllegalStateException("Unknown shell component: " + expected);
                    }
                }
            }
        }

        return true;
    }

    private static ShellComponent classifyShellComponent(BlockPos pos, BlockPos min, BlockPos max) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int xmin = min.getX(), ymin = min.getY(), zmin = min.getZ();
        int xmax = max.getX(), ymax = max.getY(), zmax = max.getZ();

        int touching = 0;
        if (x == xmin || x == xmax) touching++;
        if (y == ymin || y == ymax) touching++;
        if (z == zmin || z == zmax) touching++;

        return switch (touching) {
            case 3 -> ShellComponent.CORNER;
            case 2 -> ShellComponent.EDGE;
            case 1 -> ShellComponent.FACE;
            default -> ShellComponent.INTERIOR;
        };
    }

    private static ShellComponent classifySliceComponent(BlockPos pos, BlockPos min, BlockPos max) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int xmin = min.getX(), ymin = min.getY(), zmin = min.getZ();
        int xmax = max.getX(), ymax = max.getY(), zmax = max.getZ();

        int touching = 0;
        if (x == xmin || x == xmax) touching++;
        if (y == ymin || y == ymax) touching++;
        if (z == zmin || z == zmax) touching++;

        return switch (touching) {
            case 3 -> ShellComponent.EDGE;
            case 2 -> ShellComponent.FACE;
            default -> ShellComponent.INTERIOR;
        };
    }

    private static boolean isValidShellComponent(BlockState state, ShellComponent component, List<ShellBlockRule> rules) {
        for (ShellBlockRule rule : rules) {
            if (rule.component() == component && rule.predicate().test(state)) {
                return true;
            }
        }
        return false;
    }

    public abstract void onFormed(Level level, BlockPos min, BlockPos max, List<BlockPos> valves);
    public abstract void onBroken(Level level, BlockPos origin);
    public abstract boolean onClicked(Level level, BlockPos clickedBlock, BlockPos clickedPos, InteractionHand interactionHand, Player player);
    public abstract void copyFrom(MultiblockShell shell);
}