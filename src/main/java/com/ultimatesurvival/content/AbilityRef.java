package com.ultimatesurvival.content;

import com.google.gson.JsonObject;

/** An ability id plus its tuning parameters, exactly as written in the content tables. */
public record AbilityRef(String id, JsonObject params) {
	public double d(String key, double def) {
		return params.has(key) ? params.get(key).getAsDouble() : def;
	}

	public int i(String key, int def) {
		return params.has(key) ? params.get(key).getAsInt() : def;
	}

	public String s(String key, String def) {
		return params.has(key) ? params.get(key).getAsString() : def;
	}

	public boolean b(String key, boolean def) {
		return params.has(key) ? params.get(key).getAsBoolean() : def;
	}

	/** A two-number array parameter such as {@code "range": [0, 9]}. */
	public double[] pair(String key, double lo, double hi) {
		if (params.has(key) && params.get(key).isJsonArray() && params.getAsJsonArray(key).size() >= 2) {
			var a = params.getAsJsonArray(key);
			return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble()};
		}
		return new double[] {lo, hi};
	}
}
