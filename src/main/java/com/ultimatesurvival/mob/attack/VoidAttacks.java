package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.disc;
import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.horizontalDistSqr;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.LightZones;
import com.ultimatesurvival.util.Shake;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Void-themed attacks: black holes, rifts that birth void spawn, an eclipse that devours the light, and a cage that closes. */
final class VoidAttacks {
	private VoidAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("blackhole", BlackHole::new);
		t.put("rifts", Rifts::new);
		t.put("gloom", Gloom::new);
		t.put("cage", Cage::new);
	}

	private static RandomSource rnd(Run r) {
		return r.level.getRandom();
	}

	/** Everything a boss attack may hurt within a sphere (the disc helper in Attacks is flat). */
	static List<LivingEntity> sphere(ServerLevel sl, Vec3 c, double radius) {
		AABB box = new AABB(c, c).inflate(radius);
		return sl.getEntitiesOfClass(LivingEntity.class, box, e -> Attacks.isVictim(e) && e.getBoundingBox().getCenter().distanceToSqr(c) <= radius * radius);
	}

	// ---- black hole: a singularity opens, drags everything in, burns the core and collapses outward ----------------------------------------------------
	static final class BlackHole extends Attack {
		BlackHole(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			int recover = p.i("recover", 24);
			int count = p.i("count", 1);
			int life = p.i("life", 100);
			double radius = p.d("radius", 12);
			double core = p.d("core", 3.0);
			double pull = p.d("pull", 0.11);
			double blast = p.d("blast", 0.9);
			double rise = p.d("rise", 1.3);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<Vec3> at = new ArrayList<>();
			Vec3 first = ground(sl, r.aim);
			at.add(first.add(0, rise, 0));
			for (int i = 1; i < count; i++) {
				double a = rnd(r).nextDouble() * Math.PI * 2;
				double d = radius * 0.9 + rnd(r).nextDouble() * radius * 0.5;
				at.add(ground(sl, first.add(Math.cos(a) * d, 0, Math.sin(a) * d)).add(0, rise, 0));
			}
			r.windup = windup;
			r.total = windup + 6 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.6f, 0.5f));
			r.every(0, windup - 1, 2, i -> {
				double f = Math.max(0.12, 1.0 - (double) i * 2 / windup);
				for (Vec3 c : at) {
					Fx.ring(sl, c.subtract(0, rise - 0.1, 0), radius * f, 28, Styles.dust(style, 1.4f), 0.1);
					Fx.burst(sl, c, ParticleTypes.REVERSE_PORTAL, 6, radius * 0.25 * f, 0.02);
				}
			});
			r.at(windup, () -> {
				for (Vec3 c : at) {
					m.addHazard(new Singularity(c, radius, core, life, p.d("tick", 0.3), p.i("tick_every", 10), pull, blast, style));
				}
				Fx.sound(sl, m.position(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.4f, 0.6f);
				Shake.at(sl, m.position(), 40, 0.5f, 10);
			});
		}
	}

	/** The hole itself: drags everything toward its centre (hardest at the core, barely at the rim), burns the core, collapses outward at the end. */
	static final class Singularity extends Hazard {
		private final double core;
		private final double pull;
		private final double blast;

		Singularity(Vec3 pos, double radius, double core, int life, double dmg, int interval, double pull, double blast, String style) {
			super(pos, radius, life, dmg, interval, style);
			this.core = core;
			this.pull = pull;
			this.blast = blast;
		}

		@Override
		public boolean tick(UsmMob owner, ServerLevel sl) {
			age++;
			if (age % 2 == 0) {
				for (int k = 0; k < 3; k++) {
					double f = 1.0 - ((age * 0.02 + k / 3.0) % 1.0);
					Fx.ring(sl, pos, Math.max(0.8, radius * f), 14 + (int) (radius), Fx.dust(0x1a0030, 1.6f), 0.0);
				}
				double a = age * 0.31;
				for (int k = 0; k < 6; k++) {
					double ang = a + k * Math.PI / 3;
					double rr = core * (1.2 + 0.5 * Math.sin(age * 0.1 + k));
					sl.sendParticles(Styles.accent(style), pos.x + Math.cos(ang) * rr, pos.y + Math.sin(age * 0.2 + k) * 0.25, pos.z + Math.sin(ang) * rr, 1, 0, 0, 0, 0);
				}
				Fx.burst(sl, pos, Fx.dust(0x07000d, 2.0f), 3, core * 0.3, 0.0);
			}
			if (age % 30 == 1) {
				Fx.sound(sl, pos, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.2f, 0.4f);
			}
			for (LivingEntity v : sphere(sl, pos, radius)) {
				Vec3 to = pos.subtract(v.getBoundingBox().getCenter());
				double d = to.length();
				if (d >= 0.3) {
					double s = pull * (0.12 + (1.0 - d / radius));
					Vec3 dir = to.scale(1.0 / d);
					v.setDeltaMovement(v.getDeltaMovement().add(dir.x * s, dir.y * s * 0.6, dir.z * s));
					v.hurtMarked = true;
					v.fallDistance = 0;
				}
				if (d <= core && age % interval == 0) {
					hurt(owner, v, dmgMul, style);
					Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.DAMAGE_INDICATOR, 3, 0.2, 0.1);
				}
			}
			if (linked != null && !linked.getAsBoolean()) {
				return false;
			}
			if (--life <= 0) {
				Fx.shockwave(sl, pos, radius, 6, Styles.dust(style, 2.0f), 56);
				Styles.burst(sl, pos, style, core, 80);
				Fx.sound(sl, pos, Styles.impact(style), SoundSource.HOSTILE, 1.8f, 0.6f);
				Shake.at(sl, pos, radius * 3, 0.8f, 14);
				for (LivingEntity v : sphere(sl, pos, radius)) {
					double d = v.getBoundingBox().getCenter().distanceTo(pos);
					hurt(owner, v, blast, style);
					Dmg.shove(v, pos, 0.6 + 1.4 * (1.0 - d / radius), 0.6);
				}
				return false;
			}
			return true;
		}
	}

	// ---- rifts: circles on the floor tear open and each releases a void spawn; the rift stays open while its spawn lives -----------------------------------
	static final class Rifts extends Attack {
		Rifts(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 28);
			int recover = p.i("recover", 24);
			int count = p.i("count", 3);
			int cap = p.i("cap", 6);
			int life = p.i("life", 360);
			double radius = p.d("radius", 2.8);
			double near = p.d("near", 9);
			String mob = p.s("mob", "void_spawn");
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<Vec3> pts = new ArrayList<>();
			pts.add(ground(sl, r.aim));
			for (int i = 1; i < count; i++) {
				for (int tries = 0; tries < 12; tries++) {
					double a = rnd(r).nextDouble() * Math.PI * 2;
					double d = radius * 2.0 + rnd(r).nextDouble() * near;
					Vec3 cand = ground(sl, r.aim.add(Math.cos(a) * d, 0, Math.sin(a) * d));
					boolean ok = true;
					for (Vec3 o : pts) {
						ok &= horizontalDistSqr(o, cand) >= radius * radius * 2.25;
					}
					if (ok || tries == 11) {
						pts.add(cand);
						break;
					}
				}
			}
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.3f, 0.7f));
			r.every(0, windup - 1, 3, i -> {
				for (Vec3 pt : pts) {
					Styles.rim(sl, pt, radius, style, 20);
					Fx.burst(sl, pt.add(0, 0.6, 0), ParticleTypes.REVERSE_PORTAL, 6, radius * 0.4, 0.04);
				}
			});
			r.at(windup, () -> {
				for (Vec3 pt : pts) {
					Styles.burst(sl, pt.add(0, 0.5, 0), style, radius * 0.4, 36);
					Fx.sound(sl, pt, Styles.impact(style), SoundSource.HOSTILE, 0.9f, 1.0f);
					for (LivingEntity v : disc(sl, pt, radius, 1.2, 2.5)) {
						hurt(m, v, mult(), style);
						Dmg.shove(v, pt, 0.6, 0.5);
					}
					UsmMob spawn = m.minionCount() < cap ? m.spawnMinion(mob, pt.add(0, 0.3, 0)) : null;
					Hazard h = new Hazard(pt, radius, spawn != null ? life : 100, p.d("tick", 0.2), 10, style, "slowness", 1);
					if (spawn != null) {
						h.linked = () -> spawn.isAlive() && !spawn.isRemoved();
					}
					m.addHazard(h);
				}
			});
		}
	}

	// ---- gloom: the light is devoured; anyone standing in the dark is rotted away until they find a light ------------------------------------------------------
	static final class Gloom extends Attack {
		Gloom(MobDef.AttackSpec s) {
			super(s);
		}

		/** One eclipse at a time. */
		@Override
		public boolean usable(UsmMob self, LivingEntity target) {
			return super.usable(self, target) && self.hazards().stream().noneMatch(h -> h instanceof GloomField);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 20);
			double radius = p.d("radius", 26);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 centre = ground(sl, m.position());
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.8f, 0.4f));
			r.every(0, windup - 1, 3, i -> {
				Styles.rim(sl, centre, radius * Math.min(1.0, (i * 3.0 + 3) / windup), style, 40);
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), ParticleTypes.LARGE_SMOKE, 10, m.getBbWidth(), 0.05);
			});
			r.at(windup, () -> {
				m.addHazard(new GloomField(centre, radius, p.i("duration", 240), p.d("dmg", 0.35), p.i("interval", 20), style));
				Fx.sound(sl, m.position(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.6f, 0.5f);
				for (ServerPlayer pl : sl.players()) {
					if (Attacks.isVictim(pl) && pl.position().distanceTo(centre) <= radius + 8) {
						pl.sendOverlayMessage(Component.translatable("ultimatesurvival.boss.gloom"));
					}
				}
			});
		}
	}

	/** The eclipse dome: everyone inside sees darkness, and anyone not standing in light (a torch, a flare, a halo) is eaten away. */
	static final class GloomField extends Hazard {
		GloomField(Vec3 pos, double radius, int life, double dmg, int interval, String style) {
			super(pos, radius, life, dmg, interval, style);
		}

		@Override
		public boolean tick(UsmMob owner, ServerLevel sl) {
			age++;
			for (LivingEntity v : sphere(sl, pos, radius)) {
				Vec3 at = v.getBoundingBox().getCenter();
				boolean lit = LightZones.lit(sl, v.position().add(0, 0.5, 0));
				if (age % 10 == 0 && v instanceof ServerPlayer) {
					Effects.give(v, "darkness", 60, 0, true);
				}
				if (age % 3 == 0) {
					if (lit) {
						Fx.burst(sl, at.add(0, 0.4, 0), ParticleTypes.END_ROD, 1, 0.5, 0.01);
					} else {
						Fx.burst(sl, at, ParticleTypes.SQUID_INK, 2, 1.2, 0.0);
						Fx.burst(sl, at, ParticleTypes.REVERSE_PORTAL, 2, 1.0, 0.1);
					}
				}
				if (!lit && age % interval == 0) {
					hurt(owner, v, dmgMul, style);
					Effects.give(v, "slowness", interval + 10, 1);
					Fx.burst(sl, at, ParticleTypes.DAMAGE_INDICATOR, 3, 0.3, 0.1);
				}
			}
			if (age % 40 == 5) {
				Fx.sound(sl, pos, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.8f, 0.5f);
			}
			return --life > 0;
		}
	}

	// ---- cage: a ring of void bars closes in on you, turning slowly; the way out is through the gaps ----------------------------------------------------
	static final class Cage extends Attack {
		Cage(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 34);
			int close = p.i("close", 56);
			int recover = p.i("recover", 22);
			int bars = p.i("bars", 20);
			int gaps = Math.max(1, p.i("gaps", 2));
			double radius = p.d("radius", 8.0);
			double minR = p.d("min", 1.5);
			double width = p.d("width", 1.1);
			double gapArc = Math.toRadians(p.d("gap_arc", 52));
			double turn = Math.toRadians(p.d("turn", 0.9));
			double height = p.d("height", 4.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 centre = ground(sl, r.aim);
			double phase0 = rnd(r).nextDouble() * Math.PI * 2;
			Map<Integer, Integer> lastHit = new HashMap<>();
			r.windup = windup;
			r.total = windup + close + 12 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.5f, 0.8f));
			r.every(0, windup - 1, 3, i -> {
				drawBars(sl, centre, radius, phase0, bars, gaps, gapArc, height * 0.5, style);
				Styles.rim(sl, centre, radius, style, 30);
			});
			r.every(windup, windup + close - 1, 1, i -> {
				double cur = radius + (minR - radius) * i / close;
				double rot = phase0 + turn * i;
				if (i % 2 == 0) {
					drawBars(sl, centre, cur, rot, bars, gaps, gapArc, height, style);
					Fx.ring(sl, centre, cur, 24, Styles.dust(style, 1.6f), 0.15);
				}
				for (LivingEntity v : disc(sl, centre, radius + 2, 1.0, height)) {
					double dx = v.getX() - centre.x;
					double dz = v.getZ() - centre.z;
					double d = Math.sqrt(dx * dx + dz * dz);
					if (Math.abs(d - cur) > width + v.getBbWidth() * 0.5 || inGap(Math.atan2(dz, dx) - rot, gaps, gapArc)) {
						continue;
					}
					if (r.t - lastHit.getOrDefault(v.getId(), -99) >= 14) {
						lastHit.put(v.getId(), r.t);
						hurt(m, v, mult(), style);
						Dmg.shove(v, centre.add(dx * 2, 0, dz * 2), 0.5, 0.25);
					}
				}
			});
			r.at(windup + close, () -> {
				Fx.shockwave(sl, centre, minR + 3, 4, Styles.dust(style, 1.8f), 30);
				Styles.burst(sl, centre.add(0, 1, 0), style, 1.2, 50);
				Fx.sound(sl, centre, Styles.impact(style), SoundSource.HOSTILE, 1.4f, 0.8f);
				for (LivingEntity v : disc(sl, centre, minR + 1.5, 1.0, height)) {
					hurt(m, v, mult() * 1.4, style);
					Dmg.shove(v, centre, 1.2, 0.5);
				}
			});
		}

		/** True when angle {@code a} (relative to the cage's rotation) lies inside one of the evenly spaced gaps. */
		static boolean inGap(double a, int gaps, double arc) {
			double step = Math.PI * 2 / gaps;
			return ((a % step) + step) % step < arc;
		}

		private static void drawBars(ServerLevel sl, Vec3 c, double radius, double rot, int bars, int gaps, double arc, double height, String style) {
			for (int k = 0; k < bars; k++) {
				double a = rot + k * Math.PI * 2 / bars;
				if (inGap(a - rot, gaps, arc)) {
					continue;
				}
				Vec3 base = new Vec3(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius);
				Fx.column(sl, base, height, Styles.dust(style, 1.5f), 4);
				Fx.column(sl, base, height, Styles.accent(style), 2);
			}
		}
	}
}
