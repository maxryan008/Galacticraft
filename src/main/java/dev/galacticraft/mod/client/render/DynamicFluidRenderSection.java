package dev.galacticraft.mod.client.render;

import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.mixin.client.CompiledSectionAccessor;
import dev.galacticraft.mod.mixin.client.SectionRenderDispatcherAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DynamicFluidRenderSection extends SectionRenderDispatcher.RenderSection {
    private final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    private final BlockPos origin;

    private SectionRenderDispatcher.CompiledSection compiled;

    public DynamicFluidRenderSection(SectionRenderDispatcher dispatcher, BlockPos origin, int index) {
        dispatcher.super(index, origin.getX(), origin.getY(), origin.getZ());
        this.origin = origin;
        this.compiled = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction from, Direction to) {
                return true;
            }
        };
        ((CompiledSectionAccessor) compiled).setTransparencyState(null);
        ((CompiledSectionAccessor) compiled).getHasBlocks().add(RenderType.translucent());
    }

    public void uploadQuads(SectionRenderDispatcher dispatcher, float size) {
        // Build geometry
        BufferBuilder builder = new BufferBuilder(new ByteBufferBuilder(256), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        float x = 0, y = 0, z = 0; // slight Y offset to prevent Z-fighting
        int color = FastColor.ARGB32.color(200, 200, 0, 0);
        int light = 0xF000F0;

        builder.addVertex(x, y, z).setColor(color).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(x + size, y, z).setColor(color).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(x + size, y + size, z).setColor(color).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        builder.addVertex(x, y + size, z).setColor(color).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

        // Build mesh
        MeshData mesh = builder.buildOrThrow();

        // Camera-based sort
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();

        MeshData.SortState sortState = mesh.sortQuads(((SectionRenderDispatcherAccessor) dispatcher).getFixedBuffers().buffer(RenderType.translucent()), createVertexSorting(camPos));

        // Upload to translucent buffer
        VertexBuffer translucentBuffer = super.getBuffer(RenderType.translucent());
        translucentBuffer.bind();
        translucentBuffer.upload(mesh);
        VertexBuffer.unbind();

        // Replace compiled with a fresh instance
        SectionRenderDispatcher.CompiledSection newCompiled = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction from, Direction to) {
                return true;
            }
        };
        ((CompiledSectionAccessor) newCompiled).getHasBlocks().add(RenderType.translucent());
        ((CompiledSectionAccessor) newCompiled).setTransparencyState(sortState);

        //  Assign new compiled every frame
        this.compiled = newCompiled;
    }

    @Override
    public @NotNull VertexBuffer getBuffer(RenderType layer) {
        return layer == RenderType.translucent() ? buffer : super.getBuffer(layer);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public @NotNull BlockPos getOrigin() {
        return origin;
    }

    @Override
    public SectionRenderDispatcher.@NotNull CompiledSection getCompiled() {
        return compiled;
    }

    @Override
    protected boolean cancelTasks() {
        return super.cancelTasks();
    }

    @Override
    protected double getDistToPlayerSqr() {
        return super.getDistToPlayerSqr();
    }

    @Override
    public void releaseBuffers() {
        super.releaseBuffers();
    }

    VertexSorting createVertexSorting(Vec3 camPos) {
        return VertexSorting.byDistance(
                (float)(camPos.x - (double)this.origin.getX()), (float)(camPos.y - (double)this.origin.getY()), (float)(camPos.z - (double)this.origin.getZ())
        );
    }

    public boolean doesLayerHaveContents(RenderType type) {
        return type == RenderType.translucent();
    }
}