package dev.galacticraft.mod.machine.multiblock.multiblocks;

import dev.galacticraft.mod.machine.multiblock.MultiblockShell;
import dev.galacticraft.mod.machine.multiblock.ShellBlockRule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public abstract class PersistentContainerMultiblock<T> extends MultiblockShell {
    protected T storedData;
    private final Supplier<T> defaultSupplier;

    public PersistentContainerMultiblock(int maxX, int maxY, int maxZ, List<ShellBlockRule> rules, Supplier<T> defaultSupplier) {
        super(maxX, maxY, maxZ, rules);
        this.defaultSupplier = defaultSupplier;
        this.storedData = defaultSupplier.get();
    }

    @Override
    public void onBroken(Level level, BlockPos origin) {
        System.out.println("Structure broken; preserving data: " + storedData);
    }

    public T getStoredData() {
        return storedData;
    }

    public void setStoredData(T value) {
        this.storedData = value;
    }

    public void clearStoredData() {
        this.storedData = defaultSupplier.get();
    }

    public abstract T tryExtract(int maxAmount, boolean simulate);
    public abstract int tryInsert(T value, boolean simulate);
}
