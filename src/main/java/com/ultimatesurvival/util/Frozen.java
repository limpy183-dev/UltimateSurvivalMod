package com.ultimatesurvival.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/**
 * Who is frozen solid right now. The freeze itself is plain slowness + weakness (like Time Freeze); this only remembers it so "shatter" can reward a hit on a
 * frozen target. Bosses shrug effects off, so they are never marked.
 */
public final class Frozen {
	private static final Map<UUID, Long> UNTIL = new HashMap<>();

	private Frozen() {
	}

	public static void set(LivingEntity e, int ticks) {
		if (e instanceof com.ultimatesurvival.mob.UsmBoss) {
			return;
		}
		long now = e.level().getGameTime();
		UNTIL.merge(e.getUUID(), now + ticks, Math::max);
		Effects.give(e, "slowness", ticks, 6);
		Effects.give(e, "weakness", ticks, 3);
		e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 10));
		if (UNTIL.size() > 512) {
			UNTIL.values().removeIf(t -> t < now);
		}
	}

	public static boolean is(LivingEntity e) {
		Long t = UNTIL.get(e.getUUID());
		return t != null && t > e.level().getGameTime();
	}

	/** The ice is broken: it no longer counts as frozen (the slowness runs out on its own). */
	public static void thaw(LivingEntity e) {
		UNTIL.remove(e.getUUID());
	}

	public static void clear() {
		UNTIL.clear();
	}
}
