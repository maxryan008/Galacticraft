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

    private static final Map<ResourceKey<Level>, Map<BlockRegionKey, FormedMultiblockInstance>> LAST_KNOWN_INSTANCES = new HashMap<>();

    public static void register(Supplier<MultiblockShell> supplier) {
        REGISTERED.add(supplier);
    }

    public static void triggerCheck(Level level, BlockPos origin) {
        ResourceKey<Level> dimension = level.dimension();

        Set<FormedMultiblockInstance> instances = ACTIVE_INSTANCES.computeIfAbsent(dimension, k -> new HashSet<>());
        Map<BlockRegionKey, FormedMultiblockInstance> lastKnown = LAST_KNOWN_INSTANCES.computeIfAbsent(dimension, k -> new HashMap<>());

        BlockRegionKey brokenKey = null;

        // Break existing structures if invalid
        Iterator<FormedMultiblockInstance> it = instances.iterator();
        while (it.hasNext()) {
            FormedMultiblockInstance instance = it.next();
            if (instance.contains(origin)) {
                if (!ShellScanner.isValidShell(level, instance.min(), instance.max(), instance.shell().rules).valid()) {
                    instance.shell().onBroken(level, origin);

                    // Save last known instance for possible restoration
                    brokenKey = new BlockRegionKey(instance.min(), instance.max());
                    lastKnown.put(brokenKey, instance);

                    it.remove();
                }
            }
        }

        boolean structureReformed = false;

        // Attempt to form new structures at this location
        for (Supplier<MultiblockShell> shellSupplier : REGISTERED) {
            MultiblockShell shell = shellSupplier.get();
            MultiblockShell.MultiblockFormResult result = shell.tryForm(level, origin);
            if (result != null) {
                BlockPos formedMax = result.min().offset(result.xSize() - 1, result.ySize() - 1, result.zSize() - 1);
                BlockRegionKey key = new BlockRegionKey(result.min(), formedMax);

                FormedMultiblockInstance restored = lastKnown.remove(key);
                FormedMultiblockInstance newInstance = new FormedMultiblockInstance(shell, result.min(), formedMax);

                if (restored != null
                        && restored.shell().getClass() == shell.getClass()
                        && restored.min() != null && restored.max() != null
                        && restored.min().equals(result.min())
                        && restored.max().equals(formedMax)) {
                    shell.copyFrom(restored.shell());
                    shell.restored();
                }

                getActiveInstances(level).add(newInstance);
                structureReformed = true;
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

    public static boolean isActive(Level level, BlockPos pos) {
        for (FormedMultiblockInstance activeInstance : getActiveInstances(level)) {
            if (activeInstance.contains(pos)) {
                return true;
            }
        }

        return false;
    }
}