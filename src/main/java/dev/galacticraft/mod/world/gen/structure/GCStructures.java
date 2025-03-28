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

package dev.galacticraft.mod.world.gen.structure;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.structure.GCStructurePieceTypes;
import dev.galacticraft.mod.structure.GCStructureTemplatePools;
import dev.galacticraft.mod.structure.dungeon.DungeonConfiguration;
import dev.galacticraft.mod.structure.dungeon.DungeonStructure;
import dev.galacticraft.mod.tag.GCTags;
import dev.galacticraft.mod.world.gen.structure.meteor.MeteorStructure;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.galacticraft.mod.world.gen.structure.meteor.MeteorStructure.MeteorType.SINGULAR;

public class GCStructures {
    public static final class Moon {
        public static final ResourceKey<Structure> RUINS = key("moon_ruins");
        public static final ResourceKey<Structure> PILLAGER_BASE = key("moon_pillager_base");
        public static final ResourceKey<Structure> VILLAGE = key("moon_village");
        public static final ResourceKey<Structure> BOSS = key("moon_boss");
    }

    public static final class Meteors {
        public static final ResourceKey<Structure> SMALL_METEOR = key("small_meteor");
        public static final ResourceKey<Structure> LARGE_METEOR = key("large_meteor");
        public static final ResourceKey<Structure> EXTREME_METEOR = key("extreme_meteor");
    }

    private static ResourceKey<Structure> key(String id) {
        return Constant.key(Registries.STRUCTURE, id);
    }

    public static void bootstrapRegistries(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomeLookup = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> templatePoolLookup = context.lookup(Registries.TEMPLATE_POOL);
        context.register(Moon.RUINS, new MoonRuinsStructure(new Structure.StructureSettings(biomeLookup.getOrThrow(GCTags.MOON_RUINS_HAS_STRUCTURE))));
        context.register(Moon.PILLAGER_BASE, new JigsawStructure(
                new Structure.StructureSettings(biomeLookup.get(GCTags.MOON_PILLAGER_BASE_HAS_STRUCTURE).orElseGet(() -> createEmptyTag(GCTags.MOON_PILLAGER_BASE_HAS_STRUCTURE)), Map.of(MobCategory.MONSTER,
                        new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE,
                                WeightedRandomList.create(new MobSpawnSettings.SpawnerData(GCEntityTypes.EVOLVED_PILLAGER, 1, 1, 1))
                        )),
                        GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
                templatePoolLookup.getOrThrow(GCStructureTemplatePools.Moon.PillagerOutpost.ENTRANCE),
                7,
                ConstantHeight.of(VerticalAnchor.absolute(0)),
                true,
                Heightmap.Types.WORLD_SURFACE_WG
        ));
        context.register(Moon.VILLAGE, new JigsawStructure(
                new Structure.StructureSettings(biomeLookup.get(GCTags.MOON_VILLAGE_HIGHLANDS_HAS_STRUCTURE).orElseGet(() -> createEmptyTag(GCTags.MOON_VILLAGE_HIGHLANDS_HAS_STRUCTURE)),
                        Collections.emptyMap(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES,
                        TerrainAdjustment.BEARD_THIN),
                templatePoolLookup.getOrThrow(GCStructureTemplatePools.Moon.Village.STARTS),
                6,
                ConstantHeight.of(VerticalAnchor.absolute(0)),
                true,
                Heightmap.Types.WORLD_SURFACE_WG
        ));
        context.register(Moon.BOSS, new DungeonStructure(new Structure.StructureSettings(biomeLookup.getOrThrow(GCTags.MOON_BOSS_HAS_STRUCTURE)), new DungeonConfiguration(GCBlocks.MOON_DUNGEON_BRICK.defaultBlockState(), 25, 8, 16,
                5, 6, GCStructurePieceTypes.ROOM_BOSS, GCStructurePieceTypes.ROOM_TREASURE)));

        context.register(Meteors.SMALL_METEOR, new MeteorStructure(new Structure.StructureSettings(biomeLookup.getOrThrow(BiomeTags.IS_OVERWORLD)), new MeteorStructure.MeteorConfiguration(
                false,
                0,
                SINGULAR,
                3,
                List.of(new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_ALUMINUM_ORE), 1)),
                List.of(new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK), 1)),
                Optional.empty()
        )));

        context.register(Meteors.LARGE_METEOR, new MeteorStructure(new Structure.StructureSettings.Builder(biomeLookup.getOrThrow(BiomeTags.IS_OVERWORLD)).generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES).terrainAdapation(TerrainAdjustment.BEARD_THIN).build(), new MeteorStructure.MeteorConfiguration(
                false,
                0,
                SINGULAR,
                27,
                List.of(new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_ALUMINUM_ORE), 9), new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_IRON_ORE), 9), new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_SILICON_ORE), 9)),
                List.of(new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK), 3), new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK_2), 2), new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.DENSE_ICE), 1)),
                Optional.empty()
        )));

        context.register(Meteors.EXTREME_METEOR, new MeteorStructure(new Structure.StructureSettings.Builder(biomeLookup.getOrThrow(BiomeTags.IS_OVERWORLD)).generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES).terrainAdapation(TerrainAdjustment.BEARD_THIN).build(), new MeteorStructure.MeteorConfiguration(
                false,
                0,
                SINGULAR,
                120,
                List.of(new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_ALUMINUM_ORE), 50), new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_IRON_ORE), 100), new MeteorStructure.Core(BlockStateProvider.simple(GCBlocks.ASTEROID_SILICON_ORE), 70)),
                List.of(new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK), 9), new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.ASTEROID_ROCK_2), 6), new MeteorStructure.Shell(BlockStateProvider.simple(GCBlocks.DENSE_ICE), 3),  new MeteorStructure.Shell(BlockStateProvider.simple(Blocks.OBSIDIAN), 1)),
                Optional.empty()
        )));
    }

    @Contract("_ -> new")
    private static <T> HolderSet.@NotNull Named<T> createEmptyTag(@NotNull TagKey<T> tagKey) {
        return HolderSet.emptyNamed(new HolderOwner<>() {
            @Override
            public boolean canSerializeIn(HolderOwner<T> holderOwner) {
                return true;
            }
        }, tagKey);
    }
}
