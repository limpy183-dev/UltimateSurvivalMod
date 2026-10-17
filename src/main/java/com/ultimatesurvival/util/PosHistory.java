package com.ultimatesurvival.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** The last ~6 seconds of every player's positions, one per tick - lets a boss "rewind" you to where you stood. */
public final class PosHistory {
	private static final int SIZE = 128;
	private static final Map<UUID, Ring> RINGS = new HashMap<>();

	private static final class Ring {
		final Vec3[] pos = new Vec3[SIZE];
		int head;
		int count;
	}

	private PosHistory() {
	}

	public static void record(ServerPlayer p) {
		Ring r = RINGS.computeIfAbsent(p.getUUID(), k -> new Ring());
		r.pos[r.head] = p.position();
		r.head = (r.head + 1) % SIZE;
		r.count = Math.min(SIZE, r.count + 1);
	}

	/** Where the player stood {@code ticksAgo} ticks ago, or the oldest known position (null if never recorded). */
	public static Vec3 get(ServerPlayer p, int ticksAgo) {
		Ring r = RINGS.get(p.getUUID());
		if (r == null || r.count == 0) {
			return null;
		}
		int back = Math.min(Math.max(ticksAgo, 0), r.count - 1);
		return r.pos[(r.head - 1 - back + SIZE * 2) % SIZE];
	}

	public static void forget(UUID id) {
		RINGS.remove(id);
	}

	public static void clear() {
		RINGS.clear();
	}
}
