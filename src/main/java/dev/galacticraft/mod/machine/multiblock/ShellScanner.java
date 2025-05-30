package dev.galacticraft.mod.machine.multiblock;

import dev.galacticraft.mod.content.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ShellScanner {
    public record ValidShell(boolean valid, List<BlockPos> valves) {};

    public static ValidShell isValidShell(Level level, BlockPos min, BlockPos max, List<ShellBlockRule> rules) {
        List<BlockPos> valves = new ArrayList<>();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(GCBlocks.VALVE)) {
                        valves.add(pos);
                    }
                    ShellComponent component = classifyShellComponent(x, y, z, min, max);

                    Optional<Predicate<BlockState>> predicate = rules.stream()
                            .filter(r -> r.component() == component)
                            .map(ShellBlockRule::predicate)
                            .findFirst();

                    if (component != ShellComponent.INTERIOR) {
                        if (predicate.isEmpty() || !predicate.get().test(state)) return new ValidShell(false, valves);
                    } else {
                        // optional: allow air or fluids inside
                        if (!state.isAir()) return new ValidShell(false, valves);
                    }
                }
            }
        }
        return new ValidShell(true, valves);
    }

    private static ShellComponent classifyShellComponent(int x, int y, int z, BlockPos min, BlockPos max) {
        int match = 0;
        if (x == min.getX() || x == max.getX()) match++;
        if (y == min.getY() || y == max.getY()) match++;
        if (z == min.getZ() || z == max.getZ()) match++;

        return switch (match) {
            case 3 -> ShellComponent.CORNER;
            case 2 -> ShellComponent.EDGE;
            case 1 -> ShellComponent.FACE;
            default -> ShellComponent.INTERIOR;
        };
    }
}