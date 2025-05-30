package dev.galacticraft.mod.machine.multiblock;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ValveMode implements StringRepresentable {
    INPUT,
    OUTPUT;

    @Override
    public @NotNull String getSerializedName() {
        return this.name().toLowerCase();
    }
}