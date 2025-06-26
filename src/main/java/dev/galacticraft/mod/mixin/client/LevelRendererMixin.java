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
import dev.galacticraft.mod.client.render.TransparentQuadInjector;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static dev.galacticraft.mod.client.render.DynamicFluidRenderer.COMPILED_TRANSLUCENT_MESH_BUFFERS;
import static dev.galacticraft.mod.client.render.TransparentQuadInjector.quadMapByDimension;

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

    @Inject(method = "renderLevel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            shift = At.Shift.BEFORE
    ))
    private void gc$injectFluidGeometry(
            DeltaTracker tickCounter,
            boolean renderOutline,
            Camera camera,
            GameRenderer renderer,
            LightTexture lightmap,
            Matrix4f projection,
            Matrix4f viewMatrix,
            CallbackInfo ci
    ) {
        if (this.minecraft.level == null) return;

        SectionRenderDispatcher dispatcher = this.minecraft.levelRenderer.getSectionRenderDispatcher();
        Vec3 cameraPos = camera.getPosition();

        for (SectionRenderDispatcher.RenderSection section : List.copyOf(this.visibleSections)) {
            BlockPos origin = section.getOrigin();
            SectionPos sectionPos = SectionPos.of(origin);
            List<TransparentQuadInjector.InjectedQuad> quads = new ArrayList<>(TransparentQuadInjector.getQuadsForSection(this.minecraft.level, sectionPos));

            if (quads.isEmpty()) continue;

            DynamicFluidRenderer.MeshDataCopy meshDataCopy = DynamicFluidRenderer.get(origin);

            VertexFormat.Mode mode = (meshDataCopy != null && meshDataCopy.drawState() != null && meshDataCopy.drawState().mode() != null)
                    ? meshDataCopy.drawState().mode()
                    : VertexFormat.Mode.QUADS;

            VertexFormat format = (meshDataCopy != null && meshDataCopy.drawState() != null && meshDataCopy.drawState().format() != null)
                    ? meshDataCopy.drawState().format()
                    : DefaultVertexFormat.BLOCK;

            ByteBufferBuilder byteBuilder = new ByteBufferBuilder(1024);
            BufferBuilder builder = new BufferBuilder(byteBuilder, mode, format);

            for (TransparentQuadInjector.InjectedQuad quad : quads) {
                TransparentQuadInjector.QuadVertex[] vertices = {quad.v1(), quad.v2(), quad.v3(), quad.v4()};
                for (TransparentQuadInjector.QuadVertex vertex : vertices) {
                    builder.addVertex(vertex.x() - origin.getX(), vertex.y() - origin.getY(), vertex.z() - origin.getZ())
                            .setColor(vertex.color()).setUv(vertex.u(), vertex.v())
                            .setOverlay(vertex.overlay()).setLight(vertex.light())
                            .setNormal(vertex.nx(), vertex.ny(), vertex.nz());
                }
            }

            MeshData mesh;
            try {
                mesh = builder.buildOrThrow();
            } catch (Exception e) {
                Constant.LOGGER.error("Failed to build fluid mesh at {}: {}", sectionPos, e.getMessage(), e);
                continue;
            }
            //byteBuilder.close();

            MeshData merged = (meshDataCopy != null) ? DynamicFluidRenderer.mergeMeshes(meshDataCopy, mesh) : mesh;

            MeshData.SortState sortState = merged.sortQuads(
                    ((SectionRenderDispatcherAccessor) dispatcher).getFixedBuffers().buffer(RenderType.translucent()),
                    VertexSorting.byDistance(
                            (float)(cameraPos.x - origin.getX()),
                            (float)(cameraPos.y - origin.getY()),
                            (float)(cameraPos.z - origin.getZ())
                    )
            );

            VertexBuffer buffer;
            Object oldBuffer = ((RenderSectionAccessor) section).getBufferMap().get(RenderType.translucent());
            if (oldBuffer instanceof VertexBuffer oldBuffer2) {
                buffer = oldBuffer2;
            } else {
                buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            }

            buffer.bind();
            buffer.upload(merged);
            merged.close();
            VertexBuffer.unbind();

            ((RenderSectionAccessor) section).getBufferMap().put(RenderType.translucent(), buffer);

            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (((CompiledSectionAccessor) compiled).getHasBlocks().isEmpty()) {
                SectionRenderDispatcher.CompiledSection newCompiled = new SectionRenderDispatcher.CompiledSection();
                ((RenderSectionAccessor) section).invokeSetCompiled(newCompiled);
                compiled = newCompiled;
            }
            ((CompiledSectionAccessor) compiled).getHasBlocks().add(RenderType.translucent());
            ((CompiledSectionAccessor) compiled).setTransparencyState(sortState);
        }
    }
}
