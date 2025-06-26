package dev.galacticraft.mod.mixin.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.DynamicFluidRenderer;
import dev.galacticraft.mod.client.render.TransparentQuadInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public abstract class ViewAreaMixin {
    @Shadow public SectionRenderDispatcher.RenderSection[] sections;

    @Shadow protected int sectionGridSizeX;

    @Shadow protected int sectionGridSizeZ;

    @Shadow @Final protected Level level;

    @Shadow protected int sectionGridSizeY;

    @Shadow protected abstract int getSectionIndex(int x, int y, int z);

    @Inject(method = "setDirty", at = @At("HEAD"))
    private void gc$onSectionSetDirty(int x, int y, int z, boolean important, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        SectionPos section = SectionPos.of(x, y, z);

        // Clear both fluid quads and mesh data
        TransparentQuadInjector.removeAllInSection(mc.level, section);
        DynamicFluidRenderer.clear(section.origin());
        int i = Math.floorMod(x, this.sectionGridSizeX);
        int j = Math.floorMod(y - this.level.getMinSection(), this.sectionGridSizeY);
        int k = Math.floorMod(z, this.sectionGridSizeZ);
        SectionRenderDispatcher.RenderSection renderSection = this.sections[this.getSectionIndex(i, j, k)];
        //Object buffer = ((RenderSectionAccessor) renderSection).getBufferMap().remove(RenderType.translucent());
        //if (buffer instanceof VertexBuffer buffer2) {
            //buffer2.close();
        //}
        Constant.LOGGER.debug("Cleared fluid data from section: {}", section);
    }
}