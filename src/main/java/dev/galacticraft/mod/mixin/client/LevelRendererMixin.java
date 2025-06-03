/*
 * Copyright (c) 2019-2025 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.dimension.OverworldRenderer;
import dev.galacticraft.mod.content.entity.orbital.RocketEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique
    private OverworldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void gc$setupRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = new OverworldRenderer();
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    public void gc$renderSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player.getVehicle() instanceof RocketEntity && player.getVehicle().getY() > Constant.OVERWORLD_SKYPROVIDER_STARTHEIGHT) {
            fogCallback.run();
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(matrix4f);
            this.worldRenderer.renderOverworldSky(player, poseStack, matrix4f, tickDelta, camera, thickFog, fogCallback);
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    public void gc$preventCloudRendering(PoseStack matrices, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof RocketEntity && player.getVehicle().getY() > Constant.CLOUD_HEIGHT) {
            // Have clouds slowly fade out
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, normalize((float) player.getY(), Constant.CLOUD_HEIGHT, Constant.CLOUD_LIMIT, 1, 0F));
            if (player.getVehicle().getY() > Constant.CLOUD_LIMIT)
                ci.cancel();
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    public void gc$cancelRainAndSnow(LightTexture lightTexture, float f, double d, double e, double g, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof RocketEntity && player.getVehicle().getY() > Constant.CLOUD_HEIGHT)
            ci.cancel();
    }


    @Unique
    private float normalize(float x, float inMin, float inMax, float outMin, float outMax) {
        float outRange = outMax - outMin;
        float inRange = inMax - inMin;
        return (x - inMin) * outRange / inRange + outMin;
    }

    @Inject(
            method = "renderSectionLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void injectSingleTranslucentQuad(RenderType renderType,
                                             double camX, double camY, double camZ,
                                             Matrix4f projectionMatrix, Matrix4f positionMatrix,
                                             CallbackInfo ci) {
        if (renderType != RenderType.translucent()) return;

        RenderSystem.enableBlend();
        //RenderSystem.defaultBlendFunc();
        //RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        //RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        PoseStack poseStack = new PoseStack();

        // Translate to world position (e.g., block center at 10, 64, 10)
        double x = 0, y = 0, z = 0;
        poseStack.pushPose();
        poseStack.translate(x - camX, y - camY, z - camZ);

        PoseStack.Pose pose = poseStack.last();

        float size = 1.0f;
        int color = FastColor.ARGB32.color(200, 255, 0, 0); // ARGB: semi-transparent red (alpha = 0xCC)

        vc.addVertex(pose.pose(), 0, 0, 0).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose.pose(), size, 0, 0).setColor(color).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose.pose(), size, size, 0).setColor(color).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose.pose(), 0, size, 0).setColor(color).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 0, 1);

        poseStack.popPose();
        bufferSource.endBatch(Sheets.translucentCullBlockSheet());

        //RenderSystem.enableCull();
        //RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        //RenderSystem.disableBlend();
    }
}
