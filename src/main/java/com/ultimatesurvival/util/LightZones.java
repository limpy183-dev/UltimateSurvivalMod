package com.ultimatesurvival.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Places that count as "lit" against the Devourer's eclipse: ordinary block light (torches, lanterns, glowstone), a lumen flare, or a radiant halo.
 * Zones are plain data (no blocks are ever placed) and expire on their own.
 */
public final class LightZones {
	private record Zone(ResourceKey<Level> dim, Vec3 at, double radius, long until) {
	}

	/** Block-light level at which a spot is considered lit. */
	public static final int MIN_LIGHT = 8;
	private static final List<Zone> ZONES = new ArrayList<>();

	private LightZones() {
	}

	public static void add(ServerLevel l, Vec3 at, double radius, int ticks) {
		ZONES.add(new Zone(l.dimension(), at, radius, l.getGameTime() + ticks));
	}

	public static boolean lit(ServerLevel l, Vec3 p) {
		long now = l.getGameTime();
		ZONES.removeIf(z -> z.until < now);
		for (Zone z : ZONES) {
			if (z.dim == l.dimension() && z.at.distanceToSqr(p) <= z.radius * z.radius) {
				return true;
			}
		}
		return l.getBrightness(LightLayer.BLOCK, BlockPos.containing(p)) >= MIN_LIGHT;
	}

	public static void clear() {
		ZONES.clear();
	}
}
