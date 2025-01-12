/*
 * Copyright (c) 2019-2024 Team Galacticraft
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

package dev.galacticraft.mod.content.item;

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockRegistry;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.storage.PlaceholderItemStorage;
import dev.galacticraft.mod.util.Translations;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import static dev.galacticraft.mod.content.item.GCItems.*;

public class GCCreativeModeTabs {
    public static final CreativeModeTab ITEMS_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(GCItems.CANVAS))
            .title(Component.translatable(Translations.ItemGroup.ITEMS))
            .displayItems((parameters, output) -> { // todo: add rockets here
                // GEAR
                output.accept(OXYGEN_MASK);
                output.accept(OXYGEN_GEAR);

                try (Transaction t = Transaction.openOuter()) {
                    PlaceholderItemStorage itemStorage = new PlaceholderItemStorage();
                    ContainerItemContext context = ContainerItemContext.ofSingleSlot(itemStorage);

                    output.accept(SMALL_OXYGEN_TANK);
                    itemStorage.setItem(SMALL_OXYGEN_TANK);
                    context.find(FluidStorage.ITEM).insert(FluidVariant.of(Gases.OXYGEN), Long.MAX_VALUE, t);
                    output.accept(itemStorage.variant.toStack());

                    output.accept(MEDIUM_OXYGEN_TANK);
                    itemStorage.setItem(MEDIUM_OXYGEN_TANK);
                    context.find(FluidStorage.ITEM).insert(FluidVariant.of(Gases.OXYGEN), Long.MAX_VALUE, t);
                    output.accept(itemStorage.variant.toStack());

                    output.accept(LARGE_OXYGEN_TANK);
                    itemStorage.setItem(LARGE_OXYGEN_TANK);
                    context.find(FluidStorage.ITEM).insert(FluidVariant.of(Gases.OXYGEN), Long.MAX_VALUE, t);
                    output.accept(itemStorage.variant.toStack());
                }

                output.accept(INFINITE_OXYGEN_TANK);
                output.accept(SENSOR_GLASSES);
                output.accept(FREQUENCY_MODULE);
                PARACHUTE.colorMap().values().forEach(output::accept);

//                output.accept(SPACE_EMERGENCY_KIT);
                output.accept(SHIELD_CONTROLLER);

                // ROCKETS
                output.accept(ROCKET.getDefaultInstance());

                var rocket = ROCKET.getDefaultInstance();
                CompoundTag tag = rocket.getOrCreateTag();
                tag.putBoolean("creative", true);
                output.accept(rocket);

                // MATERIALS
                output.accept(TIN_NUGGET);
                output.accept(ALUMINUM_NUGGET);
                output.accept(METEORIC_IRON_NUGGET);
                output.accept(DESH_NUGGET);
                output.accept(LEAD_NUGGET);
                output.accept(TITANIUM_NUGGET);

                output.accept(RAW_TIN);
                output.accept(RAW_ALUMINUM);
                output.accept(RAW_METEORIC_IRON);
                output.accept(RAW_DESH);
                output.accept(RAW_LEAD);
                output.accept(RAW_TITANIUM);
                output.accept(RAW_SILICON);
                output.accept(LUNAR_SAPPHIRE);

                output.accept(METEORIC_IRON_INGOT);
                output.accept(DESH_INGOT);
                output.accept(LEAD_INGOT);
                output.accept(ALUMINUM_INGOT);
                output.accept(TIN_INGOT);
                output.accept(TITANIUM_INGOT);

                output.accept(COMPRESSED_COPPER);
                output.accept(COMPRESSED_TIN);
                output.accept(COMPRESSED_ALUMINUM);
                output.accept(COMPRESSED_STEEL);
                output.accept(COMPRESSED_BRONZE);
                output.accept(COMPRESSED_IRON);
                output.accept(COMPRESSED_METEORIC_IRON);
                output.accept(COMPRESSED_DESH);
                output.accept(COMPRESSED_TITANIUM);

                output.accept(TIER_1_HEAVY_DUTY_PLATE);
                output.accept(TIER_2_HEAVY_DUTY_PLATE);
                output.accept(TIER_3_HEAVY_DUTY_PLATE);

                output.accept(DESH_STICK);
                output.accept(CARBON_FRAGMENTS);
                output.accept(IRON_SHARD);
                output.accept(SOLAR_DUST);
                output.accept(BASIC_WAFER);
                output.accept(ADVANCED_WAFER);
                output.accept(BEAM_CORE);
                output.accept(CANVAS);

                output.accept(FLUID_MANIPULATOR);
                output.accept(OXYGEN_CONCENTRATOR);
                output.accept(OXYGEN_FAN);
                output.accept(OXYGEN_VENT);
                output.accept(SENSOR_LENS);
                output.accept(BLUE_SOLAR_WAFER);
                output.accept(SINGLE_SOLAR_MODULE);
                output.accept(FULL_SOLAR_PANEL);
                output.accept(SOLAR_ARRAY_WAFER);
                output.accept(STEEL_POLE);
                output.accept(COPPER_CANISTER);
                output.accept(TIN_CANISTER);
                output.accept(THERMAL_CLOTH);
                output.accept(ISOTHERMAL_FABRIC);
                output.accept(ORION_DRIVE);
                output.accept(ATMOSPHERIC_VALVE);
                output.accept(AMBIENT_THERMAL_CONTROLLER);

                // FOOD
                output.accept(MOON_BERRIES);
                output.accept(CHEESE_CURD);

                output.accept(CHEESE_SLICE);
                output.accept(BURGER_BUN);
                output.accept(GROUND_BEEF);
                output.accept(BEEF_PATTY);
                output.accept(CHEESEBURGER);

                output.accept(CANNED_DEHYDRATED_APPLE);
                output.accept(CANNED_DEHYDRATED_CARROT);
                output.accept(CANNED_DEHYDRATED_MELON);
                output.accept(CANNED_DEHYDRATED_POTATO);
                output.accept(CANNED_BEEF);
                output.accept(THROWABLE_METEOR_CHUNK);
                output.accept(HOT_THROWABLE_METEOR_CHUNK);

                // BATTERIES
                output.accept(BATTERY);
                output.accept(INFINITE_BATTERY);

                //FLUID BUCKETS
                output.accept(CRUDE_OIL_BUCKET);
                output.accept(FUEL_BUCKET);
                output.accept(SULFURIC_ACID_BUCKET);

                // ROCKET PARTS
                output.accept(NOSE_CONE);
                output.accept(HEAVY_NOSE_CONE);

                output.accept(ROCKET_FIN);
                output.accept(ROCKET_ENGINE);

                // SCHEMATICS
                output.accept(TIER_2_ROCKET_SCHEMATIC);
                output.accept(CARGO_ROCKET_SCHEMATIC);
                output.accept(MOON_BUGGY_SCHEMATIC);
                output.accept(TIER_3_ROCKET_SCHEMATIC);
                output.accept(ASTRO_MINER_SCHEMATIC);

                // SMITHING TEMPLATES
                output.accept(TITANTIUM_UPGRADE_SMITHING_TEMPLATE);

                // ARMOR
                output.accept(HEAVY_DUTY_HELMET);
                output.accept(HEAVY_DUTY_CHESTPLATE);
                output.accept(HEAVY_DUTY_LEGGINGS);
                output.accept(HEAVY_DUTY_BOOTS);

                output.accept(DESH_HELMET);
                output.accept(DESH_CHESTPLATE);
                output.accept(DESH_LEGGINGS);
                output.accept(DESH_BOOTS);

                output.accept(TITANIUM_HELMET);
                output.accept(TITANIUM_CHESTPLATE);
                output.accept(TITANIUM_LEGGINGS);
                output.accept(TITANIUM_BOOTS);

                // THERMAL PADDING
                output.accept(THERMAL_PADDING_HELMET);
                output.accept(THERMAL_PADDING_CHESTPIECE);
                output.accept(THERMAL_PADDING_LEGGINGS);
                output.accept(THERMAL_PADDING_BOOTS);

                // TOOLS + WEAPONS
                output.accept(HEAVY_DUTY_SWORD);
                output.accept(HEAVY_DUTY_SHOVEL);
                output.accept(HEAVY_DUTY_PICKAXE);
                output.accept(HEAVY_DUTY_AXE);
                output.accept(HEAVY_DUTY_HOE);

                output.accept(DESH_SWORD);
                output.accept(DESH_SHOVEL);
                output.accept(DESH_PICKAXE);
                output.accept(DESH_AXE);
                output.accept(DESH_HOE);

                output.accept(TITANIUM_SWORD);
                output.accept(TITANIUM_SHOVEL);
                output.accept(TITANIUM_PICKAXE);
                output.accept(TITANIUM_AXE);
                output.accept(TITANIUM_HOE);

                output.accept(STANDARD_WRENCH);
            })
            .build();

    public static final CreativeModeTab BLOCKS_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(GCItems.MOON_TURF))
            .title(Component.translatable(Translations.ItemGroup.BLOCKS))
            .displayItems((parameters, output) -> {
                // DECORATION BLOCKS
                for (GCBlockRegistry.DecorationSet decorationSet : GCBlocks.BLOCKS.getDecorations()) {
                    output.accept(decorationSet.item());
                    output.accept(decorationSet.slabItem());
                    output.accept(decorationSet.stairsItem());
                    output.accept(decorationSet.wallItem());
                    output.accept(decorationSet.detailedItem());
                    output.accept(decorationSet.detailedSlabItem());
                    output.accept(decorationSet.detailedStairsItem());
                    output.accept(decorationSet.detailedWallItem());
                }

                // TORCHES
                output.accept(GLOWSTONE_TORCH);
                output.accept(UNLIT_TORCH);

                // LANTERNS
                output.accept(GLOWSTONE_LANTERN);
                output.accept(UNLIT_LANTERN);

                // MOON NATURAL
                output.accept(MOON_TURF);
                output.accept(MOON_DIRT);
                output.accept(MOON_DIRT_PATH);
                output.accept(MOON_SURFACE_ROCK);

                output.accept(MOON_ROCK);
                output.accept(MOON_ROCK_SLAB);
                output.accept(MOON_ROCK_STAIRS);
                output.accept(MOON_ROCK_WALL);

                output.accept(COBBLED_MOON_ROCK);
                output.accept(COBBLED_MOON_ROCK_SLAB);
                output.accept(COBBLED_MOON_ROCK_STAIRS);
                output.accept(COBBLED_MOON_ROCK_WALL);

                output.accept(LUNASLATE);
                output.accept(LUNASLATE_SLAB);
                output.accept(LUNASLATE_STAIRS);
                output.accept(LUNASLATE_WALL);

                output.accept(COBBLED_LUNASLATE);
                output.accept(COBBLED_LUNASLATE_SLAB);
                output.accept(COBBLED_LUNASLATE_STAIRS);
                output.accept(COBBLED_LUNASLATE_WALL);

                output.accept(MOON_BASALT);
                output.accept(MOON_BASALT_SLAB);
                output.accept(MOON_BASALT_STAIRS);
                output.accept(MOON_BASALT_WALL);

                output.accept(MOON_BASALT_BRICK);
                output.accept(MOON_BASALT_BRICK_SLAB);
                output.accept(MOON_BASALT_BRICK_STAIRS);
                output.accept(MOON_BASALT_BRICK_WALL);

                output.accept(CRACKED_MOON_BASALT_BRICK);
                output.accept(CRACKED_MOON_BASALT_BRICK_SLAB);
                output.accept(CRACKED_MOON_BASALT_BRICK_STAIRS);
                output.accept(CRACKED_MOON_BASALT_BRICK_WALL);

                // MARS NATURAL
                output.accept(MARS_SURFACE_ROCK);
                output.accept(MARS_SUB_SURFACE_ROCK);
                output.accept(MARS_STONE);
                output.accept(MARS_COBBLESTONE);
                output.accept(MARS_COBBLESTONE_SLAB);
                output.accept(MARS_COBBLESTONE_STAIRS);
                output.accept(MARS_COBBLESTONE_WALL);

                // ASTEROID NATURAL
                output.accept(ASTEROID_ROCK);
                output.accept(ASTEROID_ROCK_1);
                output.accept(ASTEROID_ROCK_2);

                // VENUS NATURAL
                output.accept(SOFT_VENUS_ROCK);
                output.accept(HARD_VENUS_ROCK);
                output.accept(SCORCHED_VENUS_ROCK);
                output.accept(VOLCANIC_ROCK);
                output.accept(PUMICE);
                output.accept(VAPOR_SPOUT);

                // MISC DECOR
                output.accept(WALKWAY);
                output.accept(WIRE_WALKWAY);
                output.accept(FLUID_PIPE_WALKWAY);
                output.accept(TIN_LADDER);
                output.accept(GRATING);

                // SPECIAL
                output.accept(ALUMINUM_WIRE);
                output.accept(SEALABLE_ALUMINUM_WIRE);
                output.accept(HEAVY_SEALABLE_ALUMINUM_WIRE);
                output.accept(GLASS_FLUID_PIPE);
                output.accept(ROCKET_LAUNCH_PAD);

                for (DyeColor color : DyeColor.values()) {
                    ItemStack stack = new ItemStack(GCBlocks.PARACHEST);
                    CompoundTag itemTag = new CompoundTag();
                    CompoundTag blockStateTag = new CompoundTag();
                    itemTag.put("BlockStateTag", blockStateTag);
                    blockStateTag.putString("color", color.getName());

                    stack.setTag(itemTag);
                    output.accept(stack);
                }

                // LIGHT PANELS
                output.accept(SQUARE_LIGHT_PANEL);
                output.accept(SPOTLIGHT_LIGHT_PANEL);
                output.accept(LINEAR_LIGHT_PANEL);
                output.accept(DASHED_LIGHT_PANEL);
                output.accept(DIAGONAL_LIGHT_PANEL);

                // VACUUM GLASS
                output.accept(VACUUM_GLASS);
                output.accept(CLEAR_VACUUM_GLASS);
                output.accept(STRONG_VACUUM_GLASS);

                // ORES
                output.accept(SILICON_ORE);
                output.accept(DEEPSLATE_SILICON_ORE);

                output.accept(MOON_COPPER_ORE);
                output.accept(LUNASLATE_COPPER_ORE);

                output.accept(TIN_ORE);
                output.accept(DEEPSLATE_TIN_ORE);
                output.accept(MOON_TIN_ORE);
                output.accept(LUNASLATE_TIN_ORE);

                output.accept(ALUMINUM_ORE);
                output.accept(DEEPSLATE_ALUMINUM_ORE);

                output.accept(DESH_ORE);

                output.accept(ILMENITE_ORE);

                output.accept(GALENA_ORE);

                // COMPACT MINERAL BLOCKS
                output.accept(MOON_CHEESE_WHEEL);
                output.accept(SILICON_BLOCK);
                output.accept(METEORIC_IRON_BLOCK);
                output.accept(DESH_BLOCK);
                output.accept(TITANIUM_BLOCK);
                output.accept(LEAD_BLOCK);
                output.accept(LUNAR_SAPPHIRE_BLOCK);

                // MOON VILLAGER SPECIAL
                output.accept(LUNAR_CARTOGRAPHY_TABLE);

                // MISC WORLD GEN
                output.accept(CAVERNOUS_VINES);

                // MISC MACHINES
                output.accept(CRYOGENIC_CHAMBER);
                output.accept(PLAYER_TRANSPORT_TUBE);

                // MACHINES
            }).build();

    public static final CreativeModeTab MACHINES_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(GCItems.COAL_GENERATOR))
            .title(Component.translatable(Translations.ItemGroup.MACHINES))
            .displayItems((parameters, output) -> {
                output.accept(CIRCUIT_FABRICATOR);
                output.accept(COMPRESSOR);
                output.accept(ELECTRIC_COMPRESSOR);
                output.accept(COAL_GENERATOR);
                output.accept(BASIC_SOLAR_PANEL);
                output.accept(ADVANCED_SOLAR_PANEL);
                output.accept(ENERGY_STORAGE_MODULE);
                output.accept(ELECTRIC_FURNACE);
                output.accept(ELECTRIC_ARC_FURNACE);
                output.accept(REFINERY);
                output.accept(OXYGEN_COLLECTOR);
                output.accept(OXYGEN_SEALER);
                output.accept(OXYGEN_BUBBLE_DISTRIBUTOR);
                output.accept(OXYGEN_DECOMPRESSOR);
                output.accept(OXYGEN_COMPRESSOR);
                output.accept(OXYGEN_STORAGE_MODULE);
                output.accept(FUEL_LOADER);
            }).build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id(Constant.Item.ITEM_GROUP), ITEMS_GROUP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id(Constant.Block.ITEM_GROUP_BLOCKS), BLOCKS_GROUP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id(Constant.Block.ITEM_GROUP_MACHINES), MACHINES_GROUP);
    }
}