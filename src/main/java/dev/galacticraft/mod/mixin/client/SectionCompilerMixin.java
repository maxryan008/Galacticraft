package dev.galacticraft.mod.mixin.client;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.galacticraft.mod.client.render.DynamicFluidRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    @Inject(method = "compile", at = @At("RETURN"))
    private void gc$storeTranslucentMesh(
            SectionPos pos,
            RenderChunkRegion region,
            VertexSorting sort,
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<SectionCompiler.Results> cir
    ) {
        SectionCompiler.Results results = cir.getReturnValue();
        MeshData translucent = results.renderedLayers.get(RenderType.translucent());
        DynamicFluidRenderer.storeOrRemove(pos.origin(), translucent);
    }
}