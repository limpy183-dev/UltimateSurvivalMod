package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;
import static com.ultimatesurvival.mob.attack.Attacks.muzzle;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Cosmic attacks: worlds that orbit the boss and sweep the arena (Astraeus), a web of starlight laid across the floor, and the threads of the Final Thread that reel you in. */
final class CosmicAttacks {
	private CosmicAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("planets", Planets::new);
		t.put("constellation", Constellation::new);
		t.put("tether", Tether::new);
	}

	/** A glowing ball: two crossed rings of dust round a bright core. */
	private static void orb(ServerLevel sl, Vec3 c, double radius, String style) {
		for (int k = 0; k < 8; k++) {
			double a = Math.PI * 2 * k / 8;
			sl.sendParticles(Styles.dust(style, 1.5f), c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
			sl.sendParticles(Styles.dust(style, 1.5f), c.x + Math.cos(a) * radius, c.y + Math.sin(a) * radius, c.z, 1, 0, 0, 0, 0);
		}
		sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 2, 0.1, 0.1, 0.1, 0.0);
	}

	// ---- planets: bodies circle the boss and swing outward; the middle of the ring is safe, so is the far side of it ---------------------------------------------------
	static final class Planets extends Attack {
		Planets(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			int dur = p.i("duration", 140);
			int recover = p.i("recover", 28);
			int count = Math.max(1, p.i("count", 4));
			double r0 = p.d("r0", 5);
			double r1 = p.d("r1", 20);
			double omega = p.d("omega", 0.085);
			double size = p.d("size", 1.7);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			double dir = sl.getRandom().nextBoolean() ? 1 : -1;
			Vec3 centre = ground(sl, m.position());
			Map<Integer, Integer> lastHit = new HashMap<>();
			r.windup = windup;
			r.total = windup + dur + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.6f, 0.5f));
			r.every(0, windup - 1, 3, i -> {
				double f = Math.min(1.0, (i * 3.0 + 3) / windup);
				Styles.rim(sl, centre, r0, style, 30);
				Styles.rim(sl, centre, r0 + (r1 - r0) * f, style, 40);
				for (int k = 0; k < count; k++) {
					orb(sl, centre.add(Math.cos(k * Math.PI * 2 / count) * r0, 1.3, Math.sin(k * Math.PI * 2 / count) * r0), size * 0.5 * f, style);
				}
			});
			r.every(windup, windup + dur - 1, 1, i -> {
				double f = (double) i / dur;
				double rad = r0 + (r1 - r0) * f;
				if (i % 8 == 0) {
					Fx.sound(sl, m.position(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.5f + (float) f);
				}
				for (int k = 0; k < count; k++) {
					double a = dir * omega * i + k * Math.PI * 2 / count;
					Vec3 c = centre.add(Math.cos(a) * rad, 1.3, Math.sin(a) * rad);
					if (i % 2 == 0) {
						orb(sl, c, size * 0.7, style);
					}
					for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(size + 1.0), Attacks::isVictim)) {
						double d = v.getBoundingBox().getCenter().distanceTo(c);
						if (d <= size + v.getBbWidth() * 0.5 && i - lastHit.getOrDefault(v.getId(), -99) > 14) {
							lastHit.put(v.getId(), i);
							hurt(m, v, mult(), style);
							Dmg.shove(v, centre, 1.5, 0.5);
							Fx.burst(sl, c, ParticleTypes.END_ROD, 14, 0.5, 0.3);
							Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 1.0f, 1.1f);
						}
					}
				}
			});
		}
	}

	// ---- constellation: stars appear around the arena and are joined by lines of light; the lines are walls of damage ----------------------------------------------------
	static final class Constellation extends Attack {
		Constellation(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 44);
			int hold = p.i("hold", 44);
			int recover = p.i("recover", 28);
			int n = Math.max(4, p.i("points", 6));
			int skip = Math.max(1, p.i("skip", 2));
			double radius = p.d("radius", 18);
			double width = p.d("width", 1.3);
			int pulse = Math.max(2, p.i("pulse", 5));
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 centre = ground(sl, m.position());
			Vec3 toward = Attacks.horizontal(centre, r.aim);
			double phase = Math.atan2(toward.z, toward.x) + Math.PI / n;               // the first star sits half a step off the target's bearing: whoever is standing there starts outside the web
			Vec3[] pts = new Vec3[n];
			for (int k = 0; k < n; k++) {
				double a = phase + k * Math.PI * 2 / n;
				pts[k] = ground(sl, centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius));
			}
			Map<Integer, Integer> lastHit = new HashMap<>();
			r.windup = windup;
			r.total = windup + hold + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.8f, 0.5f));
			r.every(0, windup - 1, 2, i -> {
				double f = Math.min(1.0, (i * 2.0 + 2) / windup);
				for (int k = 0; k < n; k++) {
					Fx.column(sl, pts[k], 1.0 + 6 * f, ParticleTypes.END_ROD, 6);
					Fx.burst(sl, pts[k].add(0, 4.5 * f, 0), Styles.dust(style, 2.0f), 4, 0.3, 0.02);
					Vec3 a = pts[k];
					Vec3 b = pts[(k + skip) % n];
					Fx.line(sl, a.add(0, 0.2, 0), a.add(b.subtract(a).scale(f)).add(0, 0.2, 0), Styles.dust(style, 0.7f), 1.5);
				}
			});
			r.every(windup, windup + hold - 1, 1, i -> {
				if (i == 0) {
					Fx.sound(sl, centre, Styles.impact(style), SoundSource.HOSTILE, 2.0f, 0.9f);
					Shake.at(sl, centre, 50, 0.5f, 10);
				}
				for (int k = 0; k < n; k++) {
					Vec3 a = pts[k];
					Vec3 b = pts[(k + skip) % n];
					if (i % 2 == 0) {
						for (double h = 0.3; h < 3.6; h += 1.1) {
							Fx.line(sl, a.add(0, h, 0), b.add(0, h, 0), h < 1 ? Styles.dust(style, 1.6f) : ParticleTypes.END_ROD, 1.8);
						}
					}
				}
				if (i % pulse != 0) {
					return;
				}
				for (LivingEntity v : Attacks.disc(sl, centre, radius + 6, 3, 6)) {
					Vec3 c = v.position();
					for (int k = 0; k < n; k++) {
						Vec3 a = pts[k];
						Vec3 b = pts[(k + skip) % n];
						if (Aim.distToSegment(new Vec3(c.x, a.y, c.z), a, b) <= width + v.getBbWidth() * 0.5 && c.y < a.y + 4 && i - lastHit.getOrDefault(v.getId(), -99) > 8) {
							lastHit.put(v.getId(), i);
							hurt(m, v, mult(), style);
							Dmg.shove(v, c.add(c.subtract(a).multiply(1, 0, 1)), 0.5, 0.5);
							Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 12, 0.4, 0.2);
							break;
						}
					}
				}
			});
		}
	}

	// ---- tether: glowing threads reach for everyone in the arena and haul them in; cover breaks a thread -------------------------------------------------------------------
	static final class Tether extends Attack {
		Tether(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 40);
			int recover = p.i("recover", 30);
			double reach = p.d("reach", 48);
			double pull = p.d("pull", 2.0);
			double radius = p.d("radius", 5.5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Map<Integer, LivingEntity> bound = new HashMap<>();
			r.windup = windup;
			r.total = windup + 16 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.8f, 0.6f));
			r.every(0, windup - 1, 2, i -> {
				Vec3 from = muzzle(m);
				for (LivingEntity v : Attacks.disc(sl, m.position(), reach, 12, 20)) {
					Vec3 to = v.getBoundingBox().getCenter();
					if (LightAttacks.clear(sl, from, to, m)) {
						double f = Math.min(1.0, (i * 2.0 + 2) / windup);
						Fx.line(sl, from, from.add(to.subtract(from).scale(f)), Styles.dust(style, 1.5f), 0.8);
						Fx.line(sl, from, from.add(to.subtract(from).scale(f)), ParticleTypes.END_ROD, 3.0);
						bound.put(v.getId(), v);
					} else {
						bound.remove(v.getId());
					}
				}
			});
			r.at(windup, () -> Fx.sound(sl, m.position(), Styles.impact(style), SoundSource.HOSTILE, 1.8f, 1.4f));
			r.every(windup, windup + 13, 1, i -> {                                                       // the threads reel in for fourteen ticks, then the burst
				Vec3 c = m.position();
				for (LivingEntity v : bound.values()) {
					if (!v.isAlive()) {
						continue;
					}
					Vec3 to = c.subtract(v.position());
					Vec3 h = new Vec3(to.x, 0, to.z);
					double d = h.length();
					if (d > 2.5) {
						double resist = 1.0 - v.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
						Vec3 vel = h.scale(1.0 / d).scale(Math.min(pull * 0.95, 0.4 + d * 0.15) * resist);
						v.setDeltaMovement(vel.x, Math.max(v.getDeltaMovement().y, 0.12), vel.z);
						v.hurtMarked = true;
					}
					if (i % 3 == 0) {
						Fx.line(sl, muzzle(m), v.getBoundingBox().getCenter(), Styles.dust(style, 1.6f), 0.7);
					}
				}
			});
			r.at(windup + 14, () -> {
				Vec3 c = ground(sl, m.position());                                                    // the burst lands on the floor beneath a hovering boss
				Fx.shockwave(sl, c, radius, 6, Styles.dust(style, 2.0f), 56);
				Styles.burst(sl, c.add(0, 0.8, 0), style, radius * 0.4, 60);
				Fx.sound(sl, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
				Shake.at(sl, c, 40, 0.6f, 10);
				for (LivingEntity v : Attacks.disc(sl, c, radius, 2.0, m.getBbHeight() + 3.0)) {
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, 1.0, 0.6);
				}
			});
		}
	}
}
