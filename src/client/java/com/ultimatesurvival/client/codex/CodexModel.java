package com.ultimatesurvival.client.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.codex.Discovery;
import com.ultimatesurvival.content.AttrSpec;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.UsmState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What the Codex lists and says: rows per tab, discovery state, stat lines, recipe data. Pure data, no drawing. */
final class CodexModel {
	enum Kind {
		ITEM, BOSS, MOB
	}

	/** One list row. {@code def} is null for blocks / bosses. */
	static final class Row {
		final Kind kind;
		final String id;
		final String key;           // discovery key
		final int tier;
		final Gate gate;
		final ItemDef def;
		final Item item;            // null for bosses / mobs
		final Identifier emblem;    // bosses only

		Row(Kind kind, String id, String key, int tier, Gate gate, ItemDef def, Item item, Identifier emblem) {
			this.kind = kind;
			this.id = id;
			this.key = key;
			this.tier = tier;
			this.gate = gate;
			this.def = def;
			this.item = item;
			this.emblem = emblem;
		}

		/** The player has really had this item (or met this mob / boss), or an op revealed it. */
		boolean collected() {
			return UsmState.progress().isDiscovered(key);
		}

		/** Tier 1 gear can be made from the first minute, and a beaten Sovereign opens all of its gear: the page is open before the first piece exists, so you can read what to craft. */
		boolean preview() {
			return kind == Kind.ITEM && gate != null && Gates.isCleared(gate);
		}

		/** Listed with its page open, but not obtained yet: drawn faded. */
		boolean ghost() {
			return preview() && !collected();
		}

		/** Whether the page shows its content rather than "???". */
		boolean known() {
			return switch (kind) {
				case BOSS -> collected() || bossRevealed(gate);
				case ITEM -> collected() || preview();
				default -> collected();
			};
		}

		boolean defeated() {
			return kind == Kind.BOSS && Gates.isCleared(gate);
		}

		boolean locked() {
			return gate != null && kind == Kind.ITEM && def != null && !Gates.isCleared(gate);
		}

		Component name() {
			return switch (kind) {
				case ITEM -> Component.translatable(item.getDescriptionId());
				case BOSS -> Component.literal(gate.name());
				case MOB -> Component.translatable("entity." + UltimateSurvival.MOD_ID + "." + id);
			};
		}

		/** "Ignarok, the Cinder Warden" -> "Ignarok" (list rows are narrow). */
		String shortName() {
			String n = gate != null ? gate.name() : id;
			int c = n.indexOf(',');
			return c > 0 ? n.substring(0, c) : n;
		}

		ItemStack icon() {
			return item == null ? ItemStack.EMPTY : new ItemStack(item);
		}
	}

	private CodexModel() {
	}

	/** A Sovereign page opens once the Sovereign before it has fallen (or you have met it). */
	static boolean bossRevealed(Gate g) {
		if (g.id().equals("ender_dragon")) {
			return true;                                    // always know of the Dragon: it is the first goal
		}
		for (Gate prev : Content.GATES) {
			if (prev.order() == g.order() - 1) {
				return Gates.isCleared(prev) || prev.isStart();
			}
		}
		return false;
	}

	static List<Row> items(int tierFilter) {
		List<Row> out = new ArrayList<>();
		for (ItemDef d : Content.ITEMS) {
			if (d.json.has("hidden") || (tierFilter > 0 && d.tier != tierFilter)) {
				continue;
			}
			out.add(new Row(Kind.ITEM, d.id, Discovery.itemKey(d.id), d.tier, Content.gate(d.gate), d, UsmItems.get(d.id), null));
		}
		if (Content.root.has("blocks")) {
			for (JsonElement e : Content.root.getAsJsonArray("blocks")) {
				JsonObject b = e.getAsJsonObject();
				String id = b.get("id").getAsString();
				Gate g = Content.gate(b.has("gate") ? b.get("gate").getAsString() : "start");
				if (tierFilter > 0 && g.tier() != tierFilter) {
					continue;
				}
				out.add(new Row(Kind.ITEM, id, Discovery.itemKey(id), g.tier(), g, null, UsmBlocks.ITEMS.get(id), null));
			}
		}
		out.sort((a, b) -> a.gate.order() != b.gate.order() ? Integer.compare(a.gate.order(), b.gate.order()) : 0);
		return out;
	}

	static List<Row> bosses() {
		List<Row> out = new ArrayList<>();
		for (Gate g : Content.GATES) {
			if (g.boss() != null) {
				out.add(new Row(Kind.BOSS, g.id(), Discovery.bossKey(g.id()), g.tier(), g, null, null,
						UltimateSurvival.id("textures/gui/emblem/" + g.id() + ".png")));
			}
		}
		return out;
	}

	static List<Row> mobs() {
		List<Row> out = new ArrayList<>();
		if (Content.root.has("mobs")) {
			for (JsonElement e : Content.root.getAsJsonArray("mobs")) {
				JsonObject m = e.getAsJsonObject();
				if (!m.get("kind").getAsString().equals("mob")) {
					continue;
				}
				String id = m.get("id").getAsString();
				Gate g = Content.gate(m.has("gate") ? m.get("gate").getAsString() : "start");
				out.add(new Row(Kind.MOB, id, Discovery.mobKey(id), g.tier(), g, null, null, null));
			}
		}
		return out;
	}

	// ---- stats ---------------------------------------------------------------------------------
	record Stat(Component label, String value) {
	}

	static List<Stat> stats(ItemDef d) {
		List<Stat> s = new ArrayList<>();
		switch (d.kind) {
			case "sword", "staff" -> {
				s.add(stat("damage", fmt(d.stat("damage", 0))));
				s.add(stat("speed", fmt(d.stat("speed", 0)) + "/s"));
				if (d.stat("reach", 0) != 0) {
					s.add(stat("reach", (d.stat("reach", 0) > 0 ? "+" : "") + fmt(d.stat("reach", 0))));
				}
				s.add(stat("durability", String.valueOf((int) d.stat("durability", 0))));
			}
			case "pickaxe", "axe", "shovel", "hoe" -> {
				s.add(stat("mining", fmt(d.stat("mine", 0))));
				s.add(stat("damage", fmt(d.stat("damage", 0))));
				s.add(stat("durability", String.valueOf((int) d.stat("durability", 0))));
			}
			case "bow" -> {
				s.add(new Stat(Component.literal("Draw time"), fmt(d.stat("draw", 20) / 20.0) + "s"));
				s.add(new Stat(Component.literal("Ammo"), d.stat("ammo", 1) > 0 ? "Arrows" : "None (energy)"));
				s.add(stat("durability", String.valueOf((int) d.stat("durability", 0))));
			}
			case "armor" -> {
				s.add(stat("armor", String.valueOf((int) d.stat("armor", 0))));
				s.add(stat("toughness", fmt(d.stat("toughness", 0))));
				s.add(stat("durability", String.valueOf((int) (d.stat("durability_mult", 0)
						* switch (d.str("slot", "chest")) { case "head" -> 11; case "legs" -> 15; case "feet" -> 13; default -> 16; }))));
			}
			case "accessory" -> {
				for (AttrSpec a : AttrSpec.of(d)) {
					boolean pct = a.op() != AttributeModifier.Operation.ADD_VALUE;
					String v = (a.amount() >= 0 ? "+" : "") + (pct ? fmt(a.amount() * 100) + "%" : fmt(a.amount()));
					s.add(new Stat(Component.translatable("attribute.name." + a.name()), v));
				}
			}
			default -> {
			}
		}
		// digging traits
		if (d.stat("area", 0) > 0) {
			int n = (int) d.stat("area", 0) * 2 + 1;
			s.add(new Stat(Component.literal("Area"), n + "x" + n));
		}
		if (d.stat("fell", 0) > 0) {
			s.add(new Stat(Component.literal("Tree felling"), d.stat("fell_leaves", 0) > 0 ? "logs + leaves" : "logs"));
		}
		if (d.stat("vein", 0) > 0) {
			s.add(new Stat(Component.literal("Vein mining"), "up to " + (int) d.stat("vein", 0)));
		}
		if (d.stat("hoe_area", 0) > 0) {
			int n = (int) d.stat("hoe_area", 0) * 2 + 1;
			s.add(new Stat(Component.literal("Farming area"), n + "x" + n + (d.stat("replant", 0) > 0 ? ", replants" : "")));
		}
		if (d.stat("smelt", 0) > 0) {
			s.add(new Stat(Component.literal("Auto-smelt"), "yes"));
		}
		if (d.stat("magnet", 0) > 0) {
			s.add(new Stat(Component.literal("Magnet drops"), "yes"));
		}
		return s;
	}

	private static Stat stat(String key, String v) {
		return new Stat(Component.translatable("codex.stat." + key), v);
	}

	static String fmt(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
	}

	// ---- ability text (same lang entries the tooltips use) --------------------------------------
	record AbilityText(String kind, String title, List<String> body, String cooldown) {
	}

	static List<AbilityText> abilities(String descriptionId) {
		List<AbilityText> out = new ArrayList<>();
		Language lang = Language.getInstance();
		for (int n = 0; lang.has(descriptionId + ".ab." + n); n++) {
			String[] p = lang.getOrDefault(descriptionId + ".ab." + n).split("\\|", -1);
			if (p.length >= 3) {
				out.add(new AbilityText(p[0], p[1], List.of(p[2].split("\n")), p.length > 3 ? p[3] : ""));
			}
		}
		return out;
	}

	// ---- recipes ------------------------------------------------------------------------------------
	/** A recipe as the Codex draws it. cells = 9 ingredient strings (3x3 grid, "" = empty; may be "a|b" alternatives). */
	record Recipe(String type, String[] cells, String result, int count, List<String> extra) {
	}

	static List<Recipe> recipes(ItemDef d) {
		List<Recipe> out = new ArrayList<>();
		if (d == null || !d.json.has("recipe")) {
			return out;
		}
		for (JsonElement e : d.json.getAsJsonArray("recipe")) {
			out.add(parseRecipe(e.getAsJsonObject(), d.id));
		}
		return out;
	}

	static List<Recipe> blockRecipes(String blockId) {
		List<Recipe> out = new ArrayList<>();
		if (Content.root.has("blocks")) {
			for (JsonElement e : Content.root.getAsJsonArray("blocks")) {
				JsonObject b = e.getAsJsonObject();
				if (b.get("id").getAsString().equals(blockId) && b.has("recipe")) {
					for (JsonElement r : b.getAsJsonArray("recipe")) {
						out.add(parseRecipe(r.getAsJsonObject(), blockId));
					}
				}
			}
		}
		return out;
	}

	private static Recipe parseRecipe(JsonObject r, String resultId) {
		String type = r.get("type").getAsString();
		String[] cells = new String[9];
		java.util.Arrays.fill(cells, "");
		int count = r.has("count") ? r.get("count").getAsInt() : 1;
		switch (type) {
			case "shaped" -> {
				JsonArray pat = r.getAsJsonArray("pattern");
				JsonObject key = r.getAsJsonObject("key");
				for (int row = 0; row < pat.size(); row++) {
					String line = pat.get(row).getAsString();
					for (int col = 0; col < line.length(); col++) {
						char c = line.charAt(col);
						if (c != ' ') {
							cells[row * 3 + col] = key.get(String.valueOf(c)).getAsString();
						}
					}
				}
			}
			case "shapeless" -> {
				JsonArray ing = r.getAsJsonArray("ingredients");
				for (int i = 0; i < ing.size() && i < 9; i++) {
					cells[i] = ing.get(i).getAsString();
				}
			}
			case "smelting" -> {
				JsonElement in = r.get("ingredient");
				if (in.isJsonArray()) {
					List<String> alts = new ArrayList<>();
					in.getAsJsonArray().forEach(x -> alts.add(x.getAsString()));
					cells[4] = String.join("|", alts);
				} else {
					cells[4] = in.getAsString();
				}
			}
			default -> {
			}
		}
		return new Recipe(type, cells, resultId, count, List.of());
	}

	static int itemsFound() {
		return Discovery.itemsKnown();
	}

	static int totalEntries() {
		return Discovery.allKeys().size();
	}

	static JsonObject bossStats(String gateId) {
		JsonObject bal = Content.root.has("balance") ? Content.root.getAsJsonObject("balance") : null;
		if (bal != null && bal.has("boss") && bal.getAsJsonObject("boss").has(gateId)) {
			return bal.getAsJsonObject("boss").getAsJsonObject(gateId);
		}
		return null;
	}

	static int unlockCount(Gate g) {
		int n = 0;
		for (ItemDef d : Content.ITEMS) {
			if (!d.json.has("hidden") && d.gate.equals(g.id())) {
				n++;
			}
		}
		return n;
	}

	static List<Item> unlockPreview(Gate g, int max) {
		List<Item> out = new ArrayList<>();
		for (ItemDef d : Content.ITEMS) {
			if (!d.json.has("hidden") && d.gate.equals(g.id()) && out.size() < max) {
				out.add(UsmItems.get(d.id));
			}
		}
		return out;
	}

	static Item itemOf(String id) {
		return BuiltInRegistries.ITEM.getValue(id.contains(":") ? Identifier.parse(id) : UltimateSurvival.id(id));
	}

	static int tipCount() {
		return Content.root.has("codex") ? Content.root.getAsJsonObject("codex").getAsJsonArray("tips").size() : 0;
	}

	static int tipTier(int n) {
		return Content.root.getAsJsonObject("codex").getAsJsonArray("tips").get(n).getAsJsonObject().get("tier").getAsInt();
	}

	static int bossTipCount(String gateId) {
		for (JsonElement e : Content.root.getAsJsonObject("codex").getAsJsonArray("bosses")) {
			if (e.getAsJsonObject().get("id").getAsString().equals(gateId)) {
				return e.getAsJsonObject().get("tips").getAsInt();
			}
		}
		return 0;
	}
}
