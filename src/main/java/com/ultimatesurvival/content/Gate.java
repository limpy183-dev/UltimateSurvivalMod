package com.ultimatesurvival.content;

/** A progression gate: cleared once its boss has been defeated. {@code boss} is an entity type id or null. */
public record Gate(String id, int order, int tier, String name, String title, String boss) {
	public boolean isStart() {
		return order == 0;
	}
}
