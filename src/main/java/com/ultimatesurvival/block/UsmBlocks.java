package com.ultimatesurvival.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.Content;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Ore and storage blocks from the manifest ("blocks") plus their block items and world generation. */
public final class UsmBlocks {
	public static final Map<String, Block> BY_ID = new LinkedHashMap<>();
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	private UsmBlocks() {
	}

	public static void register() {
		if (!Content.root.has("blocks")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("blocks")) {
			JsonObject o = e.getAsJsonObject();
			String id = o.get("id").getAsString();
			ResourceKey<Block> bk = ResourceKey.create(Registries.BLOCK, UltimateSurvival.id(id));
			boolean deep = o.has("deepslate") && o.get("deepslate").getAsBoolean();
			BlockBehaviour.Properties props = BlockBehaviour.Properties.of().setId(bk)
					.strength((float) o.get("hardness").getAsDouble(), (float) o.get("resistance").getAsDouble())
					.sound(deep ? SoundType.DEEPSLATE : o.has("metal") && o.get("metal").getAsBoolean() ? SoundType.METAL : SoundType.STONE);
			if (o.has("light") && o.get("light").getAsInt() > 0) {
				int light = o.get("light").getAsInt();
				props.lightLevel(s -> light);
			}
			boolean trophy = o.get("kind").getAsString().equals("trophy");
			if (!trophy) {
				props.requiresCorrectToolForDrops();                                       // trophies come away with bare hands
			}
			Block block;
			if (trophy) {
				props.noOcclusion();
				block = new TrophyBlock(props);
			} else if (o.get("kind").getAsString().equals("altar")) {
				props.noOcclusion();
				block = new RitualAltarBlock(props);
			} else if (o.get("kind").getAsString().equals("ore")) {
				int lo = o.has("xp_min") ? o.get("xp_min").getAsInt() : 1;
				int hi = o.has("xp_max") ? o.get("xp_max").getAsInt() : 3;
				block = new DropExperienceBlock(UniformInt.of(lo, hi), props);
			} else {
				block = new Block(props);
			}
			Registry.register(BuiltInRegistries.BLOCK, bk, block);
			BY_ID.put(id, block);
			ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, UltimateSurvival.id(id));
			Item item = Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(block, new Item.Properties().setId(ik).useBlockDescriptionPrefix()));
			ITEMS.put(id, item);
		}
		if (Content.root.has("worldgen")) {
			for (JsonElement e : Content.root.getAsJsonArray("worldgen")) {
				JsonObject o = e.getAsJsonObject();
				ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, UltimateSurvival.id(o.get("feature").getAsString()));
				var selector = switch (o.get("dimension").getAsString()) {
					case "nether" -> BiomeSelectors.foundInTheNether();
					case "end" -> BiomeSelectors.foundInTheEnd();
					default -> BiomeSelectors.foundInOverworld();
				};
				BiomeModifications.addFeature(selector, GenerationStep.Decoration.UNDERGROUND_ORES, key);
			}
		}
	}
}
