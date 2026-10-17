package com.ultimatesurvival.armor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Armor set definitions from the content manifest ("sets"). */
public final class ArmorSets {
	/** One bonus tier of a set: granted while at least {@code need} unlocked pieces are worn. */
	public record Bonus(int need, AbilityRef ref) {
	}

	public record SetDef(String id, String name, String gate, String ambient, int color, List<String> pieces, List<Bonus> bonuses) {
		/** Language key prefix for this set: set.ultimatesurvival.<id> (+ .bonus.N). */
		public String langKey() {
			return "set." + com.ultimatesurvival.UltimateSurvival.MOD_ID + "." + id;
		}
	}

	private static final Map<String, SetDef> BY_ID = new LinkedHashMap<>();
	private static final Map<Item, SetDef> BY_ITEM = new IdentityHashMap<>();

	private ArmorSets() {
	}

	public static Iterable<SetDef> all() {
		return BY_ID.values();
	}

	public static SetDef get(String id) {
		return BY_ID.get(id);
	}

	public static SetDef of(Item item) {
		return BY_ITEM.get(item);
	}

	/** Called after items are registered. */
	public static void load() {
		BY_ID.clear();
		BY_ITEM.clear();
		if (!Content.root.has("sets")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("sets")) {
			JsonObject o = e.getAsJsonObject();
			List<String> pieces = new ArrayList<>();
			o.getAsJsonArray("pieces").forEach(x -> pieces.add(x.getAsString()));
			List<Bonus> bonuses = new ArrayList<>();
			for (JsonElement b : o.getAsJsonArray("bonus")) {
				JsonObject bo = b.getAsJsonObject();
				bonuses.add(new Bonus(bo.get("need").getAsInt(), new AbilityRef(bo.get("id").getAsString(), bo)));
			}
			SetDef def = new SetDef(o.get("id").getAsString(), o.get("name").getAsString(), o.get("gate").getAsString(),
					o.has("ambient") ? o.get("ambient").getAsString() : "", o.has("color") ? o.get("color").getAsInt() : 0xFFFFFF,
					pieces, bonuses);
			BY_ID.put(def.id(), def);
			for (String p : pieces) {
				BY_ITEM.put(UsmItems.get(p), def);
			}
		}
	}

	/** Number of unlocked pieces of {@code set} the player currently wears. */
	public static int worn(Player p, SetDef set) {
		int n = 0;
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack s = p.getItemBySlot(slot);
			if (!s.isEmpty() && BY_ITEM.get(s.getItem()) == set && !Gates.isLocked(s)) {
				n++;
			}
		}
		return n;
	}
}
