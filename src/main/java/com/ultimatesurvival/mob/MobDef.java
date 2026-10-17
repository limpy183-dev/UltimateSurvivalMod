package com.ultimatesurvival.mob;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.content.AbilityRef;
import java.util.ArrayList;
import java.util.List;

/** One mob or boss row of the content manifest ("mobs"), parsed once at startup. */
public final class MobDef {
	/** One attack in a phase: type id, selection weight and its parameters. */
	public record AttackSpec(String type, int weight, AbilityRef p) {
	}

	public record Phase(String name, double at, int cdMin, int cdMax, double speed, double dmg, String color,
			List<AttackSpec> attacks, List<AttackSpec> enter, double taken, JsonObject nodes) {
	}

	public record Drop(String item, int min, int max, double chance) {
	}

	/** Where and when a mob appears on its own: dimension ("overworld" | "nether" | "end"), weight, group size, progress gate, night only, height band. */
	public record SpawnRule(String dim, int weight, int minGroup, int maxGroup, int minTier, String gate, boolean night, int yMin, int yMax) {
	}

	public final JsonObject json;
	public final String id;
	public final boolean boss;
	public final String name;
	public final String gate;
	public final int tier;
	public final double hp;
	public final double dmg;
	public final double armor;
	public final double speed;
	public final float width;
	public final float height;
	public final float scale;
	public final float eye;
	public final double kb;
	public final boolean fire;
	public final boolean fly;
	public final double hover;
	public final int xp;
	public final double follow;
	public final boolean melee;
	public final String role;
	public final List<Phase> phases = new ArrayList<>();
	public final List<Drop> drops = new ArrayList<>();
	public final List<Drop> bag = new ArrayList<>();
	public final JsonObject ritual;
	public final String barColor;
	public final String barOverlay;
	public final int spawnTicks;
	public final int deathTicks;
	public final int enrageSeconds;
	public final String minion;
	public final String sigil;
	public final String minionOf;
	/** Visual theme id ("ember", "storm", ...) used for the boss's particles. */
	public final String style;
	/** Preferred distance band to the target (0,0 = melee: just close in). */
	public final double keepMin;
	public final double keepMax;
	/** Natural spawning, or null when the mob only appears from eggs, rituals, summons and invasions. */
	public final SpawnRule spawn;

	public MobDef(JsonObject o) {
		this.json = o;
		this.id = o.get("id").getAsString();
		this.boss = o.get("kind").getAsString().equals("boss");
		this.name = str(o, "name", id);
		this.gate = str(o, "gate", "start");
		this.tier = o.has("tier") ? o.get("tier").getAsInt() : 1;
		this.hp = num(o, "hp", 20);
		this.dmg = num(o, "dmg", 3);
		this.armor = num(o, "armor", 0);
		this.speed = num(o, "speed", 0.25);
		JsonArray size = o.getAsJsonArray("size");
		this.width = size.get(0).getAsFloat();
		this.height = size.get(1).getAsFloat();
		this.scale = (float) num(o, "scale", 1.0);
		this.eye = (float) num(o, "eye", height * 0.85);
		this.kb = num(o, "kb", 0);
		this.fire = flag(o, "fire");
		this.fly = flag(o, "fly");
		this.hover = num(o, "hover", 0);
		this.xp = (int) num(o, "xp", 5);
		this.follow = num(o, "follow", 32);
		this.melee = !o.has("melee") || o.get("melee").getAsBoolean();
		this.role = str(o, "role", "mob");
		this.ritual = o.has("ritual") && o.get("ritual").isJsonObject() ? o.getAsJsonObject("ritual") : null;
		JsonObject bar = o.has("bar") && o.get("bar").isJsonObject() ? o.getAsJsonObject("bar") : new JsonObject();
		this.barColor = str(bar, "color", "red");
		this.barOverlay = str(bar, "overlay", "progress");
		this.spawnTicks = (int) num(o, "spawn_ticks", 0);
		this.deathTicks = (int) num(o, "death_ticks", 20);
		this.enrageSeconds = (int) num(o, "enrage", 0);
		this.minion = str(o, "minion", null);
		this.sigil = str(o, "sigil", null);
		this.minionOf = str(o, "minion_of", null);
		this.style = str(o, "style", "ember");
		double[] keep = o.has("keep") && o.get("keep").isJsonArray() ? new double[] {o.getAsJsonArray("keep").get(0).getAsDouble(), o.getAsJsonArray("keep").get(1).getAsDouble()}
				: new double[] {0, 0};
		this.keepMin = keep[0];
		this.keepMax = keep[1];
		if (o.has("spawn") && o.get("spawn").isJsonObject()) {
			JsonObject sp = o.getAsJsonObject("spawn");
			JsonArray g = sp.getAsJsonArray("group");
			JsonArray y = sp.has("y") && sp.get("y").isJsonArray() ? sp.getAsJsonArray("y") : null;
			this.spawn = new SpawnRule(str(sp, "dim", "overworld"), (int) num(sp, "weight", 10), g.get(0).getAsInt(), g.get(1).getAsInt(),
					(int) num(sp, "min_tier", 1), str(sp, "gate", ""), sp.has("night") && sp.get("night").getAsBoolean(),
					y == null ? -2048 : y.get(0).getAsInt(), y == null ? 2048 : y.get(1).getAsInt());
		} else {
			this.spawn = null;
		}
		if (o.has("phases")) {
			for (JsonElement e : o.getAsJsonArray("phases")) {
				JsonObject p = e.getAsJsonObject();
				JsonArray cd = p.getAsJsonArray("cd");
				phases.add(new Phase(str(p, "name", "Phase"), num(p, "at", 1.0), cd.get(0).getAsInt(), cd.get(1).getAsInt(),
						num(p, "speed", 1.0), num(p, "dmg", 1.0), str(p, "color", null), attacks(p.getAsJsonArray("attacks")),
						p.has("enter") ? attacks(p.getAsJsonArray("enter")) : List.of(), num(p, "taken", 1.0),
						p.has("nodes") && p.get("nodes").isJsonObject() ? p.getAsJsonObject("nodes") : null));
			}
		} else {
			phases.add(new Phase("Base", 1.0, 40, 80, 1.0, 1.0, null, o.has("attacks") ? attacks(o.getAsJsonArray("attacks")) : List.of(), List.of(), 1.0, null));
		}
		if (o.has("drops")) {
			for (JsonElement e : o.getAsJsonArray("drops")) {
				drops.add(drop(e.getAsJsonObject()));
			}
		}
		if (o.has("bag")) {
			for (JsonElement e : o.getAsJsonArray("bag")) {
				bag.add(drop(e.getAsJsonObject()));
			}
		}
	}

	private static Drop drop(JsonObject d) {
		return new Drop(d.get("item").getAsString(), d.get("min").getAsInt(), d.get("max").getAsInt(), d.get("chance").getAsDouble());
	}

	private static List<AttackSpec> attacks(JsonArray arr) {
		List<AttackSpec> out = new ArrayList<>();
		for (JsonElement e : arr) {
			JsonObject a = e.getAsJsonObject();
			out.add(new AttackSpec(a.get("type").getAsString(), a.has("w") ? a.get("w").getAsInt() : 1, new AbilityRef(a.get("type").getAsString(), a)));
		}
		return out;
	}

	private static String str(JsonObject o, String k, String def) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
	}

	private static double num(JsonObject o, String k, double def) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : def;
	}

	private static boolean flag(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean();
	}

	public boolean has(String key) {
		return json.has(key) && !json.get(key).isJsonNull();
	}
}
