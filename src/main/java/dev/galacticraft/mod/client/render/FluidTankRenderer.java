package dev.galacticraft.mod.client.render;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.mod.machine.multiblock.FormedMultiblockInstance;
import dev.galacticraft.mod.machine.multiblock.MultiblockRegistry;
import dev.galacticraft.mod.machine.multiblock.multiblocks.FluidTankMultiblock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class FluidTankRenderer {
    public static void renderAll(WorldRenderContext context) {
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource buffer = context.consumers();
        Level level = Minecraft.getInstance().level;

        if (level == null || buffer == null) return;

        renderAllImpl(poseStack, buffer, level);
    }

    private static void renderAllImpl(PoseStack poseStack, MultiBufferSource bufferSource, Level level) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        for (FormedMultiblockInstance instance : MultiblockRegistry.getActiveInstances(level)) {
            if (!(instance.shell() instanceof FluidTankMultiblock tank)) continue;
            if (tank.getStored().fluid() == Fluids.EMPTY || tank.getStored().amount() == 0) continue;

            BlockPos min = instance.min();
            BlockPos max = instance.max();

            int fluidAmount = tank.getStored().amount();
            int maxAmount = tank.getMaxCapacity();
            float fillRatio = Mth.clamp((float) fluidAmount / maxAmount, 0.0f, 1.0f);

            FluidState fluidState = tank.getStored().fluid().defaultFluidState();
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());
            TextureAtlasSprite sprite = handler.getFluidSprites(level, min, fluidState)[0];

            RenderType renderType = RenderType.translucent();
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            int fluidMinY = min.getY() + 1;
            int fluidMaxY = max.getY() - 1;
            float fluidHeight = fluidMinY + (fluidMaxY - fluidMinY + 1) * fillRatio;

            for (int x = min.getX() + 1; x < max.getX(); x++) {
                for (int y = min.getY() + 1; y < max.getY(); y++) {
                    if (y > fluidHeight) continue;

                    float top = Math.min(1.0f, fluidHeight - y);
                    for (int z = min.getZ() + 1; z < max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        poseStack.pushPose();
                        poseStack.translate(x - camX, y - camY, z - camZ);
                        Matrix4f matrix = poseStack.last().pose();

                        for (Direction dir : Direction.values()) {
                            BlockPos neighbor = pos.relative(dir);
                            if (level.getBlockState(neighbor).isAir() && dir != Direction.UP && dir != Direction.DOWN) continue;

                            renderFace(consumer, matrix, sprite, dir, top);
                        }

                        poseStack.popPose();
                    }
                }
            }
        }
    }

    private static void renderFace(VertexConsumer consumer, Matrix4f pose, TextureAtlasSprite sprite, Direction dir, float height) {
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();
        if (dir != Direction.UP && dir != Direction.DOWN) {
            minV += (maxV - minV) * (1 - height);
        }

        int color = 0xFFFFFFFF;
        int light = 0xF000F0;

        switch (dir) {
            case UP -> {
                consumer.addVertex(pose, 0, height, 0).setColor(color).setUv(minU, minV).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(pose, 0, height, 1).setColor(color).setUv(minU, maxV).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(pose, 1, height, 1).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(0, 1, 0);
                consumer.addVertex(pose, 1, height, 0).setColor(color).setUv(maxU, minV).setLight(light).setNormal(0, 1, 0);
            }
            case DOWN -> {
                consumer.addVertex(pose, 1, 0, 0).setColor(color).setUv(maxU, minV).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(pose, 1, 0, 1).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(pose, 0, 0, 1).setColor(color).setUv(minU, maxV).setLight(light).setNormal(0, -1, 0);
                consumer.addVertex(pose, 0, 0, 0).setColor(color).setUv(minU, minV).setLight(light).setNormal(0, -1, 0);
            }
            case NORTH -> {
                consumer.addVertex(pose, 0, 0, 0).setColor(color).setUv(minU, maxV).setLight(light).setNormal(0, 0, -1);
                consumer.addVertex(pose, 0, height, 0).setColor(color).setUv(minU, minV).setLight(light).setNormal(0, 0, -1);
                consumer.addVertex(pose, 1, height, 0).setColor(color).setUv(maxU, minV).setLight(light).setNormal(0, 0, -1);
                consumer.addVertex(pose, 1, 0, 0).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(0, 0, -1);
            }
            case SOUTH -> {
                consumer.addVertex(pose, 1, 0, 1).setColor(color).setUv(minU, maxV).setLight(light).setNormal(0, 0, 1);
                consumer.addVertex(pose, 1, height, 1).setColor(color).setUv(minU, minV).setLight(light).setNormal(0, 0, 1);
                consumer.addVertex(pose, 0, height, 1).setColor(color).setUv(maxU, minV).setLight(light).setNormal(0, 0, 1);
                consumer.addVertex(pose, 0, 0, 1).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(0, 0, 1);
            }
            case EAST -> {
                consumer.addVertex(pose, 1, 0, 0).setColor(color).setUv(minU, maxV).setLight(light).setNormal(1, 0, 0);
                consumer.addVertex(pose, 1, height, 0).setColor(color).setUv(minU, minV).setLight(light).setNormal(1, 0, 0);
                consumer.addVertex(pose, 1, height, 1).setColor(color).setUv(maxU, minV).setLight(light).setNormal(1, 0, 0);
                consumer.addVertex(pose, 1, 0, 1).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(1, 0, 0);
            }
            case WEST -> {
                consumer.addVertex(pose, 0, 0, 1).setColor(color).setUv(minU, maxV).setLight(light).setNormal(-1, 0, 0);
                consumer.addVertex(pose, 0, height, 1).setColor(color).setUv(minU, minV).setLight(light).setNormal(-1, 0, 0);
                consumer.addVertex(pose, 0, height, 0).setColor(color).setUv(maxU, minV).setLight(light).setNormal(-1, 0, 0);
                consumer.addVertex(pose, 0, 0, 0).setColor(color).setUv(maxU, maxV).setLight(light).setNormal(-1, 0, 0);
            }
        }
    }
}