package dev.galacticraft.impl.internal.mixin.multiblock;

import dev.galacticraft.mod.machine.multiblock.FormedMultiblockInstance;
import dev.galacticraft.mod.machine.multiblock.MultiblockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGamemodeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onRightClickBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand interactionHand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (interactionHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return;

        BlockPos pos = hitResult.getBlockPos();
        for (FormedMultiblockInstance instance : MultiblockRegistry.getActiveInstances(level)) {
            if (instance.contains(pos)) {
                if (instance.shell().onClicked(level, pos, hitResult.getBlockPos(), interactionHand, player)) {
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
                return;
            }
        }
    }
}
