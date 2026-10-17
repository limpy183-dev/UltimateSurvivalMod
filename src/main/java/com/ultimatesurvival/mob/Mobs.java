package com.ultimatesurvival.mob;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.entity.UsmEntities;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.mob.attack.Attacks;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import com.ultimatesurvival.progress.UsmState;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/** Registry of every manifest mob / boss: entity types, attributes, spawn eggs, drops and sound lookup. */
public final class Mobs {
	public static final Map<String, MobDef> DEFS = new LinkedHashMap<>();
	public static final Map<String, EntityType<? extends UsmMob>> TYPES = new LinkedHashMap<>();
	public static final Map<String, Item> EGGS = new LinkedHashMap<>();
	private static final Map<EntityType<?>, MobDef> BY_TYPE = new IdentityHashMap<>();

	private Mobs() {
	}

	public static MobDef defOf(EntityType<?> type) {
		return BY_TYPE.get(type);
	}

	public static void register() {
		if (!Content.root.has("mobs")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("mobs")) {
			MobDef d = new MobDef(e.getAsJsonObject());
			DEFS.put(d.id, d);
			EntityType<? extends UsmMob> type;
			if (d.boss) {
				type = UsmEntities.register(d.id, configure(EntityType.Builder.<UsmBoss>of(UsmBoss::new, MobCategory.MONSTER), d));
			} else if (d.spawn != null) {
				var placement = d.fly ? net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS : net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;
				type = UsmEntities.register(d.id, configure(FabricEntityType.Builder.createMob(UsmMob::new, MobCategory.MONSTER,
						mb -> mb.spawnPlacement(placement, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
								(t, level, reason, pos, random) -> UsmMob.canSpawnNaturally(d, level, reason, pos, random))), d));
				var selector = switch (d.spawn.dim()) {
					case "nether" -> BiomeSelectors.foundInTheNether();
					case "end" -> BiomeSelectors.foundInTheEnd();
					default -> BiomeSelectors.foundInOverworld();
				};
				BiomeModifications.addSpawn(selector, MobCategory.MONSTER, type, d.spawn.weight(), d.spawn.minGroup(), d.spawn.maxGroup());
			} else {
				type = UsmEntities.register(d.id, configure(EntityType.Builder.<UsmMob>of(UsmMob::new, MobCategory.MONSTER), d));
			}
			TYPES.put(d.id, type);
			BY_TYPE.put(type, d);
			FabricDefaultAttributeRegistry.register(type, UsmMob.attributes(d));
			if (d.json.has("egg") && d.json.get("egg").isJsonArray()) {
				ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, UltimateSurvival.id(d.id + "_spawn_egg"));
				Item egg = Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type)));
				EGGS.put(d.id, egg);
			}
		}
		UltimateSurvival.LOGGER.info("Registered {} mobs/bosses", DEFS.size());
	}

	private static <T extends Entity> EntityType.Builder<T> configure(EntityType.Builder<T> b, MobDef d) {
		b.sized(d.width, d.height).eyeHeight(d.eye).clientTrackingRange(d.boss ? 12 : 8).updateInterval(d.fly || d.boss ? 1 : 3);
		if (d.fire) {
			b.fireImmune();
		}
		if (d.boss) {
			b.canSpawnFarFromPlayer();
		}
		return b;
	}

	/** Fails loudly at startup if a content row names an attack type, minion or item that does not exist. */
	public static void validate() {
		for (MobDef d : DEFS.values()) {
			for (MobDef.Phase ph : d.phases) {
				for (MobDef.AttackSpec s : ph.attacks()) {
					Attacks.create(s);
				}
				for (MobDef.AttackSpec s : ph.enter()) {
					Attacks.create(s);
				}
			}
			if (d.minion != null && !TYPES.containsKey(d.minion)) {
				throw new IllegalStateException(d.id + ": unknown minion " + d.minion);
			}
			for (MobDef.Drop dr : d.drops) {
				if (item(dr.item()) == null) {
					throw new IllegalStateException(d.id + ": unknown drop " + dr.item());
				}
			}
			for (MobDef.Drop dr : d.bag) {
				if (item(dr.item()) == null) {
					throw new IllegalStateException(d.id + ": unknown bag item " + dr.item());
				}
			}
		}
	}

	// ---- drops ---------------------------------------------------------------------------------------------------------------
	/** An item by id ("ember_core" = this mod, "minecraft:diamond" = anything), or null. Safe during startup. */
	public static Item item(String id) {
		if (id.startsWith("book:")) {
			return Items.ENCHANTED_BOOK;
		}
		Item item = UsmItems.BY_ID.get(id);
		if (item == null) {
			Identifier key = id.contains(":") ? Identifier.parse(id) : UltimateSurvival.id(id);
			item = BuiltInRegistries.ITEM.get(key).map(h -> h.value()).orElse(null);
		}
		return item;
	}

	public static ItemStack stack(String id, int count) {
		if (id.startsWith("book:")) {
			return book(id, count);
		}
		Item item = item(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
	}

	/** "book:sharpness:8" -> an enchanted book holding that enchantment at that level (a level vanilla would never give you). */
	private static ItemStack book(String id, int count) {
		String[] p = id.split(":");
		ItemStack st = new ItemStack(Items.ENCHANTED_BOOK, count);
		MinecraftServer server = UsmState.server();
		if (server == null || p.length < 3) {
			return st;
		}
		var holder = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(p[1])));
		if (holder.isPresent()) {
			ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
			m.set(holder.get(), Integer.parseInt(p[2]));
			st.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
		}
		return st;
	}

	/** An item's name; an enchanted book is named after what it holds ("Enchanted Book - Sharpness VIII"). */
	public static net.minecraft.network.chat.Component displayName(ItemStack st) {
		var stored = st.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored != null && !stored.isEmpty()) {
			var first = stored.entrySet().iterator().next();
			return net.minecraft.network.chat.Component.empty().append(st.getHoverName()).append(" - ")
					.append(net.minecraft.world.item.enchantment.Enchantment.getFullname(first.getKey(), first.getIntValue()));
		}
		return st.getHoverName();
	}

	public static void rollDrops(Entity at, ServerLevel level, List<MobDef.Drop> rows) {
		for (MobDef.Drop d : rows) {
			if (level.getRandom().nextDouble() <= d.chance()) {
				int n = d.min() + (d.max() > d.min() ? level.getRandom().nextInt(d.max() - d.min() + 1) : 0);
				ItemStack st = stack(d.item(), n);
				if (!st.isEmpty()) {
					at.spawnAtLocation(level, st);
				}
			}
		}
	}

	// ---- sounds ----------------------------------------------------------------------------------------------------------------
	/** Sound from the mob's "sounds" table (vanilla event ids), or the fallback. */
	public static SoundEvent sound(MobDef d, String which, SoundEvent fallback) {
		JsonObject s = d.json.has("sounds") && d.json.get("sounds").isJsonObject() ? d.json.getAsJsonObject("sounds") : null;
		if (s != null && s.has(which)) {
			Identifier id = Identifier.parse(s.get(which).getAsString());
			return BuiltInRegistries.SOUND_EVENT.get(id).map(h -> h.value()).orElse(fallback);
		}
		return fallback;
	}

	public static double soundPitch(MobDef d) {
		JsonObject s = d.json.has("sounds") && d.json.get("sounds").isJsonObject() ? d.json.getAsJsonObject("sounds") : null;
		return s != null && s.has("pitch") ? s.get("pitch").getAsDouble() : 1.0;
	}
}
