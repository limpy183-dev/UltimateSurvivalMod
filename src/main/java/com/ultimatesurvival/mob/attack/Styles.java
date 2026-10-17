package com.ultimatesurvival.mob.attack;

import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.particle.UsmParticles;
import com.ultimatesurvival.sound.UsmSounds;
import com.ultimatesurvival.util.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Per-theme visuals for boss attacks: one style id ("ember", "storm", "blood", "chrono", "void", "radiant", "apex", "frost", "plague", "cosmic", "eternal")
 * picks the colour, the accent particle and the sounds, so every attack looks like it belongs to its boss.
 */
public final class Styles {
	private Styles() {
	}

	public static int color(String style) {
		if (style.equals("eternal")) {                                      // the Final Thread has every colour: each call picks a new one
			return Fx.rainbow(System.currentTimeMillis() / 2400.0 + Math.random() * 0.5);
		}
		return BoltSpec.colorOf(style);
	}

	public static ParticleOptions dust(String style, float size) {
		return Fx.dust(color(style), size);
	}

	/** Secondary, characteristic particle of a theme. */
	public static ParticleOptions accent(String style) {
		return switch (style) {
			case "storm" -> UsmParticles.get("storm_arc", ParticleTypes.ELECTRIC_SPARK);
			case "blood" -> UsmParticles.get("blood_drop", ParticleTypes.CRIMSON_SPORE);
			case "chrono" -> UsmParticles.get("chrono_tick", ParticleTypes.SOUL_FIRE_FLAME);
			case "void" -> UsmParticles.get("void_mote", ParticleTypes.REVERSE_PORTAL);
			case "radiant" -> UsmParticles.get("radiant_spark", ParticleTypes.END_ROD);
			case "apex" -> UsmParticles.get("apex_glimmer", ParticleTypes.ENCHANT);
			case "frost" -> UsmParticles.get("frost_flake", ParticleTypes.SNOWFLAKE);
			case "plague" -> UsmParticles.get("plague_spore", ParticleTypes.SPORE_BLOSSOM_AIR);
			case "cosmic" -> UsmParticles.get("cosmic_star", ParticleTypes.END_ROD);
			case "eternal" -> UsmParticles.get("eternal_prism", ParticleTypes.FIREWORK);
			default -> UsmParticles.get("ember_spark", ParticleTypes.FLAME);
		};
	}

	/** Heavy, lingering particle (smoke / ash / mist) of a theme. */
	public static ParticleOptions haze(String style) {
		return switch (style) {
			case "storm" -> ParticleTypes.CLOUD;
			case "blood" -> ParticleTypes.DAMAGE_INDICATOR;
			case "chrono" -> ParticleTypes.ENCHANTED_HIT;
			case "void" -> ParticleTypes.PORTAL;
			case "radiant" -> ParticleTypes.GLOW;
			case "apex" -> ParticleTypes.FIREWORK;
			case "frost" -> ParticleTypes.SNOWFLAKE;
			case "plague" -> ParticleTypes.SPORE_BLOSSOM_AIR;
			case "cosmic" -> ParticleTypes.GLOW_SQUID_INK;
			case "eternal" -> ParticleTypes.FIREWORK;
			default -> ParticleTypes.LARGE_SMOKE;
		};
	}

	public static SoundEvent warn(String style) {
		return UsmSounds.get("style." + style + ".warn", vanillaWarn(style));
	}

	public static SoundEvent roar(String style) {
		return UsmSounds.get("style." + style + ".roar", SoundEvents.RAVAGER_ROAR);
	}

	private static SoundEvent vanillaWarn(String style) {
		return switch (style) {
			case "storm" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
			case "blood" -> SoundEvents.WARDEN_HEARTBEAT;
			case "chrono" -> SoundEvents.BELL_BLOCK;
			case "void" -> SoundEvents.ENDERMAN_STARE;
			case "radiant", "cosmic", "eternal" -> SoundEvents.BEACON_POWER_SELECT;
			case "frost" -> SoundEvents.PLAYER_HURT_FREEZE;
			case "plague" -> SoundEvents.SLIME_BLOCK_PLACE;
			default -> SoundEvents.BLAZE_SHOOT;
		};
	}

	public static SoundEvent impact(String style) {
		return UsmSounds.get("style." + style + ".impact", vanillaImpact(style));
	}

	private static SoundEvent vanillaImpact(String style) {
		return switch (style) {
			case "storm" -> SoundEvents.LIGHTNING_BOLT_IMPACT;
			case "blood" -> SoundEvents.WARDEN_SONIC_BOOM;
			case "chrono" -> SoundEvents.AMETHYST_BLOCK_RESONATE;
			case "void" -> SoundEvents.ENDER_DRAGON_GROWL;
			case "radiant", "cosmic", "eternal" -> SoundEvents.BEACON_DEACTIVATE;
			case "frost" -> SoundEvents.GLASS_BREAK;
			case "plague" -> SoundEvents.SLIME_BLOCK_BREAK;
			default -> SoundEvents.GENERIC_EXPLODE.value();
		};
	}

	/** Ring of themed particles lying on the ground. */
	public static void rim(ServerLevel sl, Vec3 c, double radius, String style, int count) {
		Fx.ring(sl, c, radius, count, dust(style, 1.5f), 0.12);
		Fx.ring(sl, c, radius, Math.max(6, count / 3), accent(style), 0.18);
	}

	/** Scatter particles over a disc - marks a hazard zone. */
	public static void fill(ServerLevel sl, Vec3 c, double radius, String style, int count) {
		int n = Fx.n(count);
		var rnd = sl.getRandom();
		for (int i = 0; i < n; i++) {
			double a = rnd.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(rnd.nextDouble()) * radius;
			double x = c.x + Math.cos(a) * d;
			double z = c.z + Math.sin(a) * d;
			ParticleOptions p = rnd.nextInt(3) == 0 ? accent(style) : i % 2 == 0 ? dust(style, 1.2f) : haze(style);
			sl.sendParticles(p, x, c.y + 0.1, z, 1, 0.05, 0.25, 0.05, 0.01);
		}
	}

	/** Explosion-style burst: themed dust + accent + a smoke puff. */
	public static void burst(ServerLevel sl, Vec3 c, String style, double spread, int count) {
		Fx.burst(sl, c, dust(style, 1.8f), count, spread, 0.12);
		Fx.burst(sl, c, accent(style), count / 2, spread * 0.8, 0.1);
		Fx.burst(sl, c, haze(style), count / 4, spread * 0.6, 0.03);
	}

	/** Tall beam of particles - telegraphs where a pillar / meteor / bolt will land. */
	public static void beam(ServerLevel sl, Vec3 base, double height, String style, int count) {
		Fx.column(sl, base, height, dust(style, 1.4f), count);
		Fx.column(sl, base, height, accent(style), count / 3);
	}
}
