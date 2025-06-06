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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.DynamicFluidRenderer;
import dev.galacticraft.mod.client.render.dimension.OverworldRenderer;
import dev.galacticraft.mod.content.entity.orbital.RocketEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;
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
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void injectCustomFluidQuads(
            DeltaTracker tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightmapTextureManager,
            Matrix4f projection,
            Matrix4f positionMatrix,
            CallbackInfo ci
    ) {
        SectionRenderDispatcher dispatcher = this.minecraft.levelRenderer.getSectionRenderDispatcher();
        BlockPos targetOrigin = new BlockPos(0, 0, 0); // Replace with dynamic origin later

        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            if (!section.getOrigin().equals(targetOrigin)) continue;
            VertexBuffer buffer = section.getBuffer(RenderType.translucent());
            if (((VertexBufferAccessor) buffer).getFormatNullable() == null) {
                break;
            }
            DynamicFluidRenderer.MeshDataCopy copy = DynamicFluidRenderer.get(targetOrigin);

            // Build new mesh
            ByteBufferBuilder byteBuilder = new com.mojang.blaze3d.vertex.ByteBufferBuilder(256);
            var builder = new BufferBuilder(byteBuilder, copy.drawState().mode(), copy.drawState().format());
            int color = FastColor.ARGB32.color(200, 200, 0, 0);
            int light = 0xF000F0;
            float size = 1.0f;

            int x = 8, y = 8, z = 8;

            builder.addVertex(x, y, z).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
            builder.addVertex(x + size, y, z).setColor(color).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
            builder.addVertex(x + size, y + size, z).setColor(color).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
            builder.addVertex(x, y + size, z).setColor(color).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);

            MeshData dynamicMesh = builder.buildOrThrow();


            Vec3 cam = camera.getPosition();
            // Clone the mesh to prevent invalidation after upload
            MeshData mergedMesh = DynamicFluidRenderer.mergeMeshes(copy, dynamicMesh);
            if (mergedMesh == null) break;
            MeshData.SortState sorted = mergedMesh.sortQuads(((SectionRenderDispatcherAccessor) dispatcher).getFixedBuffers().buffer(RenderType.translucent()), createVertexSorting(cam, targetOrigin));

            buffer.bind();
            buffer.upload(mergedMesh);
            VertexBuffer.unbind();

            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            ((CompiledSectionAccessor) compiled).getHasBlocks().add(RenderType.translucent());
            ((CompiledSectionAccessor) compiled).setTransparencyState(sorted);
            break;
        }
    }

    VertexSorting createVertexSorting(Vec3 camPos, BlockPos origin) {
        return VertexSorting.byDistance(
                (float)(camPos.x - origin.getX()), (float)(camPos.y - origin.getY()), (float)(camPos.z - origin.getZ())
        );
    }
}
