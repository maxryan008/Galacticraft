package dev.galacticraft.mod.machine.multiblock.multiblocks;

import dev.galacticraft.mod.machine.multiblock.MultiblockShell;
import dev.galacticraft.mod.machine.multiblock.ShellBlockRule;
import dev.galacticraft.mod.machine.multiblock.ValveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class PersistentContainerMultiblock<T> extends MultiblockShell {
    protected T storedData;
    private final Supplier<T> defaultSupplier;
    final List<BlockPos> valvePositions = new ArrayList<>();

    public PersistentContainerMultiblock(int maxX, int maxY, int maxZ, List<ShellBlockRule> rules, Supplier<T> defaultSupplier) {
        super(maxX, maxY, maxZ, rules);
        this.defaultSupplier = defaultSupplier;
        this.storedData = defaultSupplier.get();
    }

    @Override
    public void onFormed(Level level, BlockPos min, BlockPos max, List<BlockPos> valves) {
        valvePositions.clear();
        for (BlockPos pos : valves) {
            valvePositions.add(pos);
            if (level.getBlockEntity(pos) instanceof ValveBlockEntity be) {
                be.setMultiblock(this);
                level.updateNeighborsAt(pos, be.getBlockState().getBlock());
            }
        }
    }

    @Override
    public void onBroken(Level level, BlockPos origin) {
        for (BlockPos pos : valvePositions) {
            if (level.getBlockEntity(pos) instanceof ValveBlockEntity be) {
                be.setMultiblock(null);
                level.updateNeighborsAt(pos, be.getBlockState().getBlock());
            }
        }
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
