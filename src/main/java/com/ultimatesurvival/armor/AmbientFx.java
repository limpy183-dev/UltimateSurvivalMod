package com.ultimatesurvival.armor;

import com.ultimatesurvival.particle.UsmParticles;
import com.ultimatesurvival.util.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** The faint show a full armor set puts on around its wearer: leaves, cloud, embers, sparks, blood, ticking motes, void, light and everything. */
public final class AmbientFx {
	private AmbientFx() {
	}

	/** Called every server tick for a player wearing the whole set. Cheap: three ticks apart, a handful of particles. */
	public static void tick(ServerPlayer p, String kind, int color) {
		if (kind.isEmpty() || p.tickCount % 3 != 0 || p.isSpectator() || !(p.level() instanceof ServerLevel l)) {
			return;
		}
		RandomSource r = p.getRandom();
		Vec3 at = p.position();
		double t = p.tickCount * 0.11;
		double h = p.getBbHeight();
		switch (kind) {
			case "leaf" -> scatter(l, at, h, ParticleTypes.COMPOSTER, 1, 0.5, r);
			case "cloud" -> {
				scatter(l, at, 0.3, Fx.dust(color, 0.7f), 2, 0.45, r);                                 // vanilla cloud puffs are half a block wide up close
				orbit(l, at, h * 0.55, 1.15, t, Fx.dust(color, 0.9f));
			}
			case "ember" -> {
				scatter(l, at, h, UsmParticles.get("ember_spark", ParticleTypes.FLAME), 2, 0.45, r);
				scatter(l, at, 0.1, ParticleTypes.SMALL_FLAME, 1, 0.3, r);
			}
			case "storm" -> {
				scatter(l, at, h * 0.8, UsmParticles.get("storm_arc", ParticleTypes.ELECTRIC_SPARK), 2, 0.6, r);
				orbit(l, at, h * 0.9, 1.05, -t * 1.6, UsmParticles.get("storm_arc", ParticleTypes.ELECTRIC_SPARK));
			}
			case "blood" -> {
				Vec3 s = shoulder(at, p, h, r);
				l.sendParticles(UsmParticles.get("blood_drop", ParticleTypes.DRIPPING_LAVA), s.x, s.y, s.z, 1, 0.05, 0.0, 0.05, 0.0);
				if (r.nextInt(3) == 0) {
					Vec3 s2 = shoulder(at, p, h, r);
					l.sendParticles(ParticleTypes.CRIMSON_SPORE, s2.x, s2.y - 0.3, s2.z, 1, 0.1, 0.1, 0.1, 0.0);
				}
			}
			case "time" -> {
				orbit(l, at, h * 0.5, 1.3, t, UsmParticles.get("chrono_tick", ParticleTypes.SOUL_FIRE_FLAME));
				orbit(l, at, h * 0.75, 1.1, -t * 1.3, UsmParticles.get("chrono_tick", ParticleTypes.SOUL_FIRE_FLAME));
			}
			case "void" -> {
				orbit(l, at, h * 0.5, 1.25, t, UsmParticles.get("void_mote", ParticleTypes.REVERSE_PORTAL));
				orbit(l, at, h * 0.5, 1.25, t + Math.PI, UsmParticles.get("void_mote", ParticleTypes.REVERSE_PORTAL));
				scatter(l, at, h * 0.5, ParticleTypes.REVERSE_PORTAL, 1, 0.6, r);
			}
			case "light" -> {
				for (int i = 0; i < 4; i++) {                                                  // a halo above the head
					orbit(l, at, h + 0.25, 0.32, t * 1.4 + i * Math.PI / 2, UsmParticles.get("radiant_spark", ParticleTypes.END_ROD));
				}
				scatter(l, at, h, UsmParticles.get("radiant_spark", ParticleTypes.END_ROD), 1, 0.5, r);
			}
			case "apex" -> {
				orbit(l, at, h * 0.3 + 0.4 * Math.sin(t), 1.25, t, UsmParticles.get("void_mote", ParticleTypes.REVERSE_PORTAL));
				orbit(l, at, h * 0.3 + 0.4 * Math.sin(t), 1.25, t + Math.PI, UsmParticles.get("radiant_spark", ParticleTypes.END_ROD));
				orbit(l, at, h * 0.75 - 0.4 * Math.sin(t), 1.25, -t, UsmParticles.get("apex_glimmer", ParticleTypes.ENCHANT));
				scatter(l, at, h, UsmParticles.get("apex_glimmer", ParticleTypes.ENCHANT), 1, 0.7, r);
			}
			case "frost" -> {
				orbit(l, at, h * 0.45, 1.3, t, UsmParticles.get("frost_flake", ParticleTypes.SNOWFLAKE));
				orbit(l, at, h * 0.8, 1.1, -t * 1.2, UsmParticles.get("frost_flake", ParticleTypes.SNOWFLAKE));
				scatter(l, at, h + 0.6, ParticleTypes.SNOWFLAKE, 2, 0.8, r);
			}
			case "plague" -> {
				scatter(l, at, h, UsmParticles.get("plague_spore", ParticleTypes.SPORE_BLOSSOM_AIR), 2, 0.7, r);
				orbit(l, at, h * 0.6 + 0.3 * Math.sin(t), 1.2, t * 0.8, Fx.dust(color, 1.0f));
			}
			case "cosmic" -> {
				for (int i = 0; i < 3; i++) {                                                  // three small stars circling at different heights
					orbit(l, at, h * (0.25 + 0.25 * i) + 0.2 * Math.sin(t + i), 1.3 + 0.15 * i, t * (1.0 + 0.3 * i) + i * 2.1, UsmParticles.get("cosmic_star", ParticleTypes.END_ROD));
				}
				scatter(l, at, h, Fx.dust(color, 0.8f), 1, 1.0, r);
			}
			case "eternal" -> {
				for (int i = 0; i < 6; i++) {                                                  // a ring of every colour
					orbit(l, at, h * 0.5 + 0.35 * Math.sin(t * 1.3 + i), 1.4, t + i * Math.PI / 3, Fx.dust(Fx.rainbow(t * 0.2 + i / 6.0), 1.1f));
				}
				scatter(l, at, h + 0.5, UsmParticles.get("eternal_prism", ParticleTypes.FIREWORK), 1, 0.8, r);
			}
			default -> {
			}
		}
	}

	private static void scatter(ServerLevel l, Vec3 at, double height, ParticleOptions p, int count, double spread, RandomSource r) {
		for (int i = 0; i < Fx.n(count); i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = 0.6 + r.nextDouble() * spread;                                       // never right in front of the camera
			l.sendParticles(p, at.x + Math.cos(a) * d, at.y + r.nextDouble() * height, at.z + Math.sin(a) * d, 1, 0.02, 0.05, 0.02, 0.0);
		}
	}

	private static void orbit(ServerLevel l, Vec3 at, double y, double radius, double angle, ParticleOptions p) {
		l.sendParticles(p, at.x + Math.cos(angle) * radius, at.y + y, at.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
	}

	private static Vec3 shoulder(Vec3 at, ServerPlayer p, double h, RandomSource r) {
		double yaw = Math.toRadians(p.yBodyRot);
		double side = r.nextBoolean() ? 1 : -1;
		return at.add(-Math.cos(yaw) * 0.38 * side, h * 0.82, -Math.sin(yaw) * 0.38 * side);
	}
}
