package dev.galacticraft.mod.machine.multiblock;

import dev.galacticraft.mod.content.GCBlockEntityTypes;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Objects;

public class ValveBlock extends Block implements EntityBlock {
    public static final EnumProperty<ValveMode> MODE = EnumProperty.create("mode", ValveMode.class);

    public ValveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, ValveMode.INPUT));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ValveBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ValveMode mode = state.getValue(MODE);
            mode = mode == ValveMode.INPUT ? ValveMode.OUTPUT : ValveMode.INPUT;
            ((ValveBlockEntity) Objects.requireNonNull(level.getBlockEntity(pos))).setMode(mode);
            level.setBlock(pos, state.setValue(MODE, mode), 3);
            player.displayClientMessage(Component.literal("Set mode to: " + mode), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == GCBlockEntityTypes.VALVE ? (lvl, pos, st, be) -> ((ValveBlockEntity) be).tick(st) : null;
    }
}