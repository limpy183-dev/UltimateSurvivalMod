package com.ultimatesurvival.util;

import com.ultimatesurvival.config.UsmConfig;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Server-side particle and sound helpers. Counts honour the reduced-effects config toggle and the per-effect cap. */
public final class Fx {
	private Fx() {
	}

	/** Scales a particle count by config (effect intensity up, reduced effects halves it) and applies the hard cap. */
	public static int n(int count) {
		UsmConfig c = UsmConfig.get();
		double v = count * c.effectIntensity * (c.reducedEffects ? 0.5 : 1.0);
		return (int) Math.max(1, Math.min(v, c.maxParticlesPerEffect));
	}

	public static DustParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(rgb, size);
	}

	/** A colour from the rainbow: {@code t} wraps around 0..1 (the Final Thread's signature shimmer). */
	public static int rainbow(double t) {
		double h = ((t % 1.0) + 1.0) % 1.0 * 6.0;
		double x = 1.0 - Math.abs(h % 2.0 - 1.0);
		double[] c = switch ((int) h) {
			case 0 -> new double[] {1, x, 0};
			case 1 -> new double[] {x, 1, 0};
			case 2 -> new double[] {0, 1, x};
			case 3 -> new double[] {0, x, 1};
			case 4 -> new double[] {x, 0, 1};
			default -> new double[] {1, 0, x};
		};
		return ((int) (60 + c[0] * 195) << 16) | ((int) (60 + c[1] * 195) << 8) | (int) (60 + c[2] * 195);
	}

	public static DustColorTransitionOptions fade(int from, int to, float size) {
		return new DustColorTransitionOptions(from, to, size);
	}

	/** How close to a player's eyes a particle may be placed: a dust speck that near fills the screen, so bigger effects must stop short of the camera. */
	private static final double CAMERA_CLEARANCE = 1.5;

	/** False when {@code (x, y, z)} is inside the clearance of some player's eyes. */
	public static boolean visible(ServerLevel l, double x, double y, double z) {
		for (var pl : l.players()) {
			double dx = pl.getX() - x;
			double dy = pl.getEyeY() - y;
			double dz = pl.getZ() - z;
			if (dx * dx + dy * dy + dz * dz < CAMERA_CLEARANCE * CAMERA_CLEARANCE) {
				return false;
			}
		}
		return true;
	}

	/** Sends one particle unless it would land in somebody's face. */
	public static void put(ServerLevel l, ParticleOptions type, double x, double y, double z) {
		if (visible(l, x, y, z)) {
			l.sendParticles(type, x, y, z, 1, 0, 0, 0, 0);
		}
	}

	public static void burst(ServerLevel l, Vec3 p, ParticleOptions type, int count, double spread, double speed) {
		int n = n(count);
		for (var pl : l.players()) {
			if (pl.distanceToSqr(p.x, p.y, p.z) < (spread * 3 + CAMERA_CLEARANCE) * (spread * 3 + CAMERA_CLEARANCE)) {
				var rnd = l.getRandom();                                      // somebody stands inside the burst: place each particle by hand so none lands in their face
				for (int i = 0; i < n; i++) {
					double x = p.x + rnd.nextGaussian() * spread;
					double y = p.y + rnd.nextGaussian() * spread;
					double z = p.z + rnd.nextGaussian() * spread;
					if (visible(l, x, y, z)) {
						l.sendParticles(type, x, y, z, 1, 0, 0, 0, speed);
					}
				}
				return;
			}
		}
		l.sendParticles(type, p.x, p.y, p.z, n, spread, spread, spread, speed);
	}

	public static void at(ServerLevel l, Vec3 p, ParticleOptions type, int count) {
		l.sendParticles(type, p.x, p.y, p.z, n(count), 0.15, 0.15, 0.15, 0.01);
	}

	/** Horizontal ring of particles (count is the total around the circle). */
	public static void ring(ServerLevel l, Vec3 c, double radius, int count, ParticleOptions type, double yOff) {
		int total = n(count);
		for (int i = 0; i < total; i++) {
			double a = Math.PI * 2 * i / total;
			put(l, type, c.x + Math.cos(a) * radius, c.y + yOff, c.z + Math.sin(a) * radius);
		}
	}

	/** Ring that moves outward by one radius step per tick - a visible shockwave. */
	public static void shockwave(ServerLevel l, Vec3 c, double maxRadius, int ticks, ParticleOptions type, int perRing) {
		for (int i = 1; i <= ticks; i++) {
			final double r = maxRadius * i / ticks;
			Tasks.later(i - 1, () -> ring(l, c, r, Math.max(8, (int) (perRing * r / maxRadius)), type, 0.15));
		}
	}

	public static void line(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions type, double step) {
		Vec3 d = b.subtract(a);
		double len = d.length();
		if (len < 1e-3) {
			return;
		}
		int steps = (int) Math.min(len / step, UsmConfig.get().maxParticlesPerEffect * 2L);
		Vec3 dir = d.scale(1.0 / len);
		for (int i = 0; i <= steps; i++) {
			Vec3 q = a.add(dir.scale(i * step));
			put(l, type, q.x, q.y, q.z);
		}
	}

	/** Vertical column of rising particles - telegraphs where something will land. */
	public static void column(ServerLevel l, Vec3 base, double height, ParticleOptions type, int count) {
		int total = n(count);
		for (int i = 0; i < total; i++) {
			put(l, type, base.x + (i % 2 == 0 ? 0.1 : -0.1), base.y + height * i / total, base.z);
		}
	}

	public static void sound(ServerLevel l, Vec3 p, SoundEvent s, float vol, float pitch) {
		l.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
	}

	public static void sound(ServerLevel l, Vec3 p, SoundEvent s, SoundSource src, float vol, float pitch) {
		l.playSound(null, p.x, p.y, p.z, s, src, vol, pitch);
	}

	/** A real lightning bolt for show only: no fire, no damage (the caller deals the damage). */
	public static void bolt(ServerLevel l, Vec3 p) {
		net.minecraft.world.entity.LightningBolt b = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(l, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (b != null) {
			b.snapTo(p.x, p.y, p.z);
			b.setVisualOnly(true);
			l.addFreshEntity(b);
		}
	}

	/** Colour of a tier's generic effects (used when an ability names no style of its own). */
	public static int tierColor(int tier) {
		return switch (tier) {
			case 1 -> 0x6FD26A;
			case 2 -> 0x4FD0FF;
			case 3 -> 0xFF7A1F;
			case 4 -> 0xFFE94D;
			case 5 -> 0xFF6AD5;
			default -> 0xBFF3FF;
		};
	}

	/**
	 * The flourish every right-click ability gets on top of its own effect: a ground ring, a rising column of light and a camera rattle that all grow
	 * with the item's tier, so a Tier 6 cast fills the screen and a Tier 1 cast is a polite flick.
	 */
	public static void cast(ServerLevel l, net.minecraft.server.level.ServerPlayer p, int rgb, int tier) {
		Vec3 at = p.position();
		double r = 1.4 + tier * 0.6;
		ring(l, at, r, 16 + tier * 6, dust(rgb, 1.1f + tier * 0.1f), 0.1);
		ring(l, at, r * 0.55, 10 + tier * 4, ParticleTypes.END_ROD, 0.25);
		if (tier >= 2) {
			column(l, at, 1.8 + tier * 0.5, dust(rgb, 1.2f), 8 + tier * 3);
		}
		if (tier >= 4) {
			shockwave(l, at, r * 1.8, 5, dust(rgb, 1.0f + tier * 0.1f), 20 + tier * 4);
			Shake.at(l, at, 20 + tier * 4, 0.08f + tier * 0.035f, 5 + tier);
		}
		if (tier >= 6) {
			burst(l, at.add(0, 1.0, 0), ParticleTypes.FIREWORK, 30, 1.2, 0.25);
		}
	}

	/** A weapon landing a blow: sparks and a ring at the point of impact, bigger per tier. */
	public static void hit(ServerLevel l, Vec3 at, int rgb, int tier) {
		burst(l, at, dust(rgb, 0.9f + tier * 0.12f), 4 + tier * 3, 0.35, 0.1);
		if (tier >= 2) {
			burst(l, at, ParticleTypes.CRIT, 3 + tier * 2, 0.3, 0.3);
		}
		if (tier >= 4) {
			burst(l, at, ParticleTypes.END_ROD, 2 + tier, 0.35, 0.15);
			ring(l, at, 0.9 + tier * 0.2, 10 + tier * 3, dust(rgb, 1.0f), 0.0);
		}
		if (tier >= 6) {
			burst(l, at, ParticleTypes.FIREWORK, 8, 0.5, 0.2);
		}
	}

	/** Convenience: a lightning-style spark burst without the actual lightning bolt entity. */
	public static void sparks(ServerLevel l, Vec3 p, int count) {
		l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, n(count), 0.4, 0.4, 0.4, 0.3);
	}
}
