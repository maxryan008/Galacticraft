/*
 * Copyright (c) 2019-2021 Team Galacticraft
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

package dev.galacticraft.mod.mixin;

import dev.galacticraft.api.atmosphere.AtmosphericGas;
import dev.galacticraft.api.registry.RegistryUtil;
import dev.galacticraft.api.universe.celestialbody.landable.Landable;
import dev.galacticraft.mod.accessor.ChunkOxygenAccessor;
import dev.galacticraft.mod.accessor.WorldOxygenAccessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author <a href="https://github.com/TeamGalacticraft">TeamGalacticraft</a>
 */
@Mixin(World.class)
public abstract class WorldMixin implements WorldOxygenAccessor {

    @Shadow public abstract WorldChunk getWorldChunk(BlockPos pos);

    @Shadow @Final private RegistryKey<World> registryKey;

    @Shadow
    public static boolean isValid(BlockPos pos) {
        return false;
    }

    private @Unique boolean breathable = true;
    private @Unique boolean init = false;

    @Override
    public boolean isBreathable(BlockPos pos) {
        if (!this.init) {
            this.init = true;
            RegistryUtil.getCelestialBodyByDimension(((World)(Object)this).getRegistryManager(), this.registryKey).ifPresent(celestialBodyType -> this.breathable = ((Landable) celestialBodyType.type()).atmosphere(celestialBodyType.config()).composition().containsKey(AtmosphericGas.OXYGEN_ID));
        }
        if (breathable) return true;
        if (isValid(pos)) return false;
        return ((ChunkOxygenAccessor) this.getWorldChunk(pos)).isBreathable(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
    }

    @Override
    public void setBreathable(BlockPos pos, boolean value) {
        if (!this.init) {
            this.init = true;
            RegistryUtil.getCelestialBodyByDimension(((World)(Object)this).getRegistryManager(), this.registryKey).ifPresent(celestialBodyType -> this.breathable = ((Landable) celestialBodyType.type()).atmosphere(celestialBodyType.config()).composition().containsKey(AtmosphericGas.OXYGEN_ID));
        }
        if (isValid(pos) || breathable) return;
        ((ChunkOxygenAccessor) this.getWorldChunk(pos)).setBreathable(pos.getX() & 15, pos.getY(), pos.getZ() & 15, value);
    }
}