package com.ultimatesurvival.mob;

import com.ultimatesurvival.mob.attack.Styles;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Marks a boss's treasure bag: a thin column of theme-coloured sparks rises from it until it is picked up (or five minutes pass). */
public final class LootBeacon {
	private static final int PERIOD = 6;
	private static final int LIFETIME = 20 * 300 / PERIOD;
	private static int live;

	private LootBeacon() {
	}

	/** How many beacons are burning right now (tests). */
	public static int live() {
		return live;
	}

	public static void mark(ItemEntity item, String style) {
		live++;
		step(item, style, LIFETIME);
	}

	private static void step(ItemEntity item, String style, int left) {
		Tasks.later(PERIOD, () -> {
			if (item.isRemoved() || left <= 0 || !(item.level() instanceof ServerLevel sl)) {
				live = Math.max(0, live - 1);
				return;
			}
			Vec3 at = item.position();
			int n = Fx.n(6);
			for (int k = 0; k < n; k++) {
				sl.sendParticles(k % 2 == 0 ? Styles.accent(style) : ParticleTypes.END_ROD, at.x, at.y + 0.4 + k * 0.55, at.z, 1, 0.06, 0.0, 0.06, 0.0);
			}
			step(item, style, left - 1);
		});
	}

	public static void clear() {
		live = 0;
	}
}
