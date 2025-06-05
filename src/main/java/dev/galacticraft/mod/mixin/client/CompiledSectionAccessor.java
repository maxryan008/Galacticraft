package dev.galacticraft.mod.mixin.client;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(SectionRenderDispatcher.CompiledSection.class)
public interface CompiledSectionAccessor {
    @Accessor("hasBlocks")
    Set<RenderType> getHasBlocks();

    @Accessor("transparencyState")
    void setTransparencyState(MeshData.SortState state);

    @Accessor("transparencyState")
    MeshData.SortState getSortState();
}