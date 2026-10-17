package com.ultimatesurvival.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ultimatesurvival.UltimateSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the generated content manifest (data/ultimatesurvival/usm/content.json) from the mod jar.
 * Everything - gates, items, later blocks/mobs/bosses - is data; Java only knows behaviours.
 */
public final class Content {
	public static final String PATH = "/data/" + UltimateSurvival.MOD_ID + "/usm/content.json";

	public static final List<Gate> GATES = new ArrayList<>();
	public static final Map<String, Gate> GATE_BY_ID = new LinkedHashMap<>();
	public static final List<ItemDef> ITEMS = new ArrayList<>();
	public static JsonObject root = new JsonObject();

	private Content() {
	}

	public static void load() {
		try (InputStream in = Content.class.getResourceAsStream(PATH)) {
			if (in == null) {
				throw new IllegalStateException("missing " + PATH + " - run python tools/gen_all.py");
			}
			root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("cannot read content manifest", e);
		}
		GATES.clear();
		GATE_BY_ID.clear();
		ITEMS.clear();
		for (JsonElement e : root.getAsJsonArray("gates")) {
			JsonObject g = e.getAsJsonObject();
			Gate gate = new Gate(g.get("id").getAsString(), g.get("order").getAsInt(), g.get("tier").getAsInt(),
					g.get("name").getAsString(), g.get("title").getAsString(),
					g.has("boss") && !g.get("boss").isJsonNull() ? g.get("boss").getAsString() : null);
			GATES.add(gate);
			GATE_BY_ID.put(gate.id(), gate);
		}
		for (JsonElement e : root.getAsJsonArray("items")) {
			ITEMS.add(new ItemDef(e.getAsJsonObject()));
		}
		UltimateSurvival.LOGGER.info("Content loaded: {} gates, {} items", GATES.size(), ITEMS.size());
	}

	public static Gate gate(String id) {
		return GATE_BY_ID.get(id);
	}
}
