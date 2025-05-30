package dev.galacticraft.impl.internal.mixin.multiblock;

import dev.galacticraft.mod.machine.multiblock.FormedMultiblockInstance;
import dev.galacticraft.mod.machine.multiblock.MultiblockRegistry;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onClientClick(LocalPlayer player, InteractionHand interactionHand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (interactionHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return;

        BlockPos pos = hitResult.getBlockPos();
        Level level = player.level();

        for (FormedMultiblockInstance instance : MultiblockRegistry.getActiveInstances(level)) {
            if (instance.contains(pos)) {
                if (instance.shell().onClicked(level, pos, pos, interactionHand, player)) {
                    // Send the interaction packet manually
                    player.connection.send(new ServerboundUseItemOnPacket(interactionHand, hitResult, 0));

                    // Play swing and cancel the rest
                    player.swing(interactionHand);
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
                return;
            }
        }
    }
}