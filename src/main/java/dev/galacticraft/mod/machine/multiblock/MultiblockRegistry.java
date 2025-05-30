package dev.galacticraft.mod.machine.multiblock;

import dev.galacticraft.mod.machine.multiblock.multiblocks.FluidTankMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Supplier;

public class MultiblockRegistry {
    private static final List<Supplier<MultiblockShell>> REGISTERED = new ArrayList<>();

    // Formed instances stored per dimension
    private static final Map<ResourceKey<Level>, Set<FormedMultiblockInstance>> ACTIVE_INSTANCES = new HashMap<>();

    public static void register(Supplier<MultiblockShell> supplier) {
        REGISTERED.add(supplier);
    }

    public static void triggerCheck(Level level, BlockPos origin) {
        ResourceKey<Level> dimension = level.dimension();

        // Get or create instance set for this dimension
        Set<FormedMultiblockInstance> instances = ACTIVE_INSTANCES.computeIfAbsent(dimension, k -> new HashSet<>());

        // Break existing structures if invalid
        Iterator<FormedMultiblockInstance> it = instances.iterator();
        while (it.hasNext()) {
            FormedMultiblockInstance instance = it.next();
            if (instance.contains(origin)) {
                if (!ShellScanner.isValidShell(level, instance.min(), instance.max(), instance.shell().rules).valid()) {
                    instance.shell().onBroken(level, origin);
                    it.remove();
                }
            }
        }

        // Attempt to form new structures at this location
        for (Supplier<MultiblockShell> shellSupplier : REGISTERED) {
            MultiblockShell shell = shellSupplier.get();
            BlockPos formedMin = shell.tryFormAndGetMin(level, origin);
            if (formedMin != null) {
                BlockPos formedMax = formedMin.offset(shell.maxX - 1, shell.maxY - 1, shell.maxZ - 1);
                instances.add(new FormedMultiblockInstance(shell, formedMin, formedMax));
                break;
            }
        }
    }

    public static Set<FormedMultiblockInstance> getActiveInstances(Level level) {
        return ACTIVE_INSTANCES.getOrDefault(level.dimension(), Set.of());
    }

    public static void init() {
        register(FluidTankMultiblock::new);
    }
}