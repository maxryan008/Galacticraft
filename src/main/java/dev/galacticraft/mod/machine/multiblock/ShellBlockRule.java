package dev.galacticraft.mod.machine.multiblock;


import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Predicate;

public record ShellBlockRule(ShellComponent component, Predicate<BlockState> predicate) {}