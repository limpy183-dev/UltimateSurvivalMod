package com.ultimatesurvival.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One item row from the content manifest (see tools/usm_gen/items.py). */
public final class ItemDef {
	public final String id;
	public final String kind;
	public final String gate;
	public final int tier;
	public final String rarity;
	public final boolean glint;
	public final Map<String, Double> stats = new HashMap<>();
	public final List<AbilityRef> abilities = new ArrayList<>();
	public final JsonObject json;

	public ItemDef(JsonObject o) {
		this.json = o;
		this.id = o.get("id").getAsString();
		this.kind = o.get("kind").getAsString();
		this.gate = o.get("gate").getAsString();
		this.tier = o.get("tier").getAsInt();
		this.rarity = o.has("rarity") ? o.get("rarity").getAsString() : "common";
		this.glint = o.has("glint") && o.get("glint").getAsBoolean();
		if (o.has("stats")) {
			for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("stats").entrySet()) {
				if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
					stats.put(e.getKey(), e.getValue().getAsDouble());
				}
			}
		}
		if (o.has("abilities")) {
			for (JsonElement e : o.getAsJsonArray("abilities")) {
				JsonObject a = e.getAsJsonObject();
				abilities.add(new AbilityRef(a.get("id").getAsString(), a));
			}
		}
	}

	public double stat(String key, double def) {
		return stats.getOrDefault(key, def);
	}

	public boolean has(String key) {
		return json.has(key);
	}

	public String str(String key, String def) {
		return json.has(key) ? json.get(key).getAsString() : def;
	}
}
