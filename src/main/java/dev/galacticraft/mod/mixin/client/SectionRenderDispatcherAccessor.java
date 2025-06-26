package dev.galacticraft.mod.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

    @Environment(EnvType.CLIENT)
@Mixin({SectionRenderDispatcher.class})
public interface SectionRenderDispatcherAccessor {
    @Accessor("fixedBuffers")
    SectionBufferBuilderPack getFixedBuffers();
}