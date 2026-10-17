package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.disc;
import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.horizontal;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Attacks any boss can use: perching (landing for a vulnerable window), exposing itself, blinking, leaping and the reflect stance. */
final class CommonAttacks {
	private CommonAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("perch", Perch::new);
		t.put("exposed", Exposed::new);
		t.put("blink", Blink::new);
		t.put("leap", Leap::new);
		t.put("reflect", Reflect::new);
	}

	/** A flier dives to the ground, slams it, and sits there stunned and vulnerable before lifting off again. */
	static final class Perch extends Attack {
		Perch(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 36);
			int stun = p.i("stun", 90);
			int recover = p.i("recover", 24);
			double vuln = p.d("vuln", 1.6);
			double radius = p.d("radius", 5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + stun + recover;
			r.at(0, () -> {
				m.perching = true;
				Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.4f, 0.6f);
			});
			r.every(0, windup - 1, 1, i -> {
				double floor = ground(sl, m.position()).y;
				if (i > 4 && m.getY() > floor + 0.05) {                       // fliers keep a stale onGround flag: measure the height instead
					Vec3 v = m.getDeltaMovement();
					m.setDeltaMovement(v.x * 0.5, -Math.min(0.55, (m.getY() - floor) * 0.5 + 0.05), v.z * 0.5);
				}
				if (i % 4 == 0) {
					Styles.rim(sl, ground(sl, m.position()), radius * (i + 4) / windup, style, 16);
				}
			});
			r.at(windup, () -> {
				m.setDeltaMovement(Vec3.ZERO);
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 5, Styles.dust(style, 1.6f), 36);
				Styles.burst(sl, c.add(0, 0.4, 0), style, radius * 0.3, 36);
				Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 1.5f, 0.7f);
				Shake.at(sl, c, 30, 0.6f, 10);
				for (LivingEntity v : disc(sl, c, radius, 1.0, 2.5)) {
					if (!v.onGround() && v.getY() > m.getY() + 0.9) {
						continue;
					}
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, 1.0, 0.5);
				}
				m.setVuln(stun, vuln);
			});
			r.every(windup, windup + stun - 1, 8, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), Styles.haze(style), 4, m.getBbWidth() * 0.5, 0.03));
			r.onEnd(() -> {
				m.perching = false;
				m.clearVuln();
				m.setDeltaMovement(0, 0.55, 0);
			});
		}
	}

	/** The boss drops its guard for a while: it takes extra damage (the "window" the Codex tips talk about). */
	static final class Exposed extends Attack {
		Exposed(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 20);
			int dur = p.i("duration", 90);
			int recover = p.i("recover", 16);
			double mult = p.d("vuln", 2.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + dur + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.2f, 0.5f));
			r.every(0, windup - 1, 3, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), Styles.accent(style), 5, m.getBbWidth() * 0.6, 0.06));
			r.at(windup, () -> {
				m.setVuln(dur, mult);
				Styles.burst(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), style, m.getBbWidth(), 40);
				Fx.sound(sl, m.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4f, 0.6f);
			});
			r.every(windup, windup + dur - 1, 6, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.55, 0), Styles.haze(style), 3, m.getBbWidth() * 0.5, 0.02));
			r.onEnd(m::clearVuln);
		}
	}

	/** Vanishes and reappears behind the target, then strikes the spot. */
	static final class Blink extends Attack {
		Blink(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 18);
			int delay = p.i("hit_delay", 6);
			int recover = p.i("recover", 14);
			double radius = p.d("radius", 3.5);
			double behind = p.d("behind", 3.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			LivingEntity t = r.target;
			Vec3 look = t == null ? new Vec3(0, 0, 1) : horizontal(Vec3.ZERO, t.getLookAngle());
			Vec3 dest = ground(sl, (t == null ? r.aim : t.position()).subtract(look.scale(behind)));
			r.windup = windup;
			r.total = windup + delay + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.2f, 1.4f));
			r.every(0, windup - 1, 3, i -> Styles.rim(sl, dest, radius, style, 16));
			r.at(windup, () -> {
				m.blinkTo(dest);
				if (t != null) {
					m.faceYaw(horizontal(dest, t.position()));
				}
			});
			r.at(windup + delay, () -> {
				Styles.burst(sl, dest.add(0, 0.8, 0), style, radius * 0.4, 40);
				Fx.sound(sl, dest, Styles.impact(style), SoundSource.HOSTILE, 1.1f, 1.2f);
				for (LivingEntity v : disc(sl, dest, radius, 1.2, 2.5)) {
					hurt(m, v, mult(), style);
					Dmg.shove(v, dest, 0.9, 0.4);
				}
			});
		}
	}

	/** A ballistic jump onto the spot the target stood on, ending in a slam. */
	static final class Leap extends Attack {
		Leap(MobDef.AttackSpec s) {
			super(s);
		}

		/** Initial velocity that lands on {@code to} after {@code ticks} ticks under vanilla air drag and gravity. */
		static Vec3 velocity(Vec3 from, Vec3 to, int ticks) {
			double h = Math.hypot(to.x - from.x, to.z - from.z);
			double sum = 0;
			double f = 1;
			for (int i = 0; i < ticks; i++) {
				sum += f;
				f *= 0.91;
			}
			double vh = h / sum;
			double dy = to.y - from.y;
			double lo = -3;
			double hi = 4;
			for (int it = 0; it < 40; it++) {
				double mid = (lo + hi) / 2;
				double y = 0;
				double v = mid;
				for (int i = 0; i < ticks; i++) {
					y += v;
					v = (v - 0.08) * 0.98;
				}
				if (y < dy) {
					lo = mid;
				} else {
					hi = mid;
				}
			}
			Vec3 dir = horizontal(from, to);
			return new Vec3(dir.x * vh, (lo + hi) / 2, dir.z * vh);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 26);
			int air = p.i("air", 18);
			int recover = p.i("recover", 16);
			double radius = p.d("radius", 6.5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 dest = ground(sl, r.aim);
			boolean[] landed = {false};
			r.windup = windup;
			r.total = windup + air + 16 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.8f));
			r.every(0, windup - 1, 3, i -> {
				Styles.rim(sl, dest, radius, style, 18);
				Styles.rim(sl, dest, radius * Math.max(0.15, 1.0 - (double) i * 3 / windup), style, 12);
			});
			r.at(windup, () -> {
				m.faceYaw(horizontal(m.position(), dest));
				m.setDeltaMovement(velocity(m.position(), dest, air));
				m.hurtMarked = true;
				Fx.sound(sl, m.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.3f, 1.2f);
			});
			Runnable land = () -> {
				if (landed[0]) {
					return;
				}
				landed[0] = true;
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 5, Styles.dust(style, 1.8f), 40);
				Styles.burst(sl, c.add(0, 0.3, 0), style, radius * 0.3, 44);
				Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 1.6f, 0.8f);
				Shake.at(sl, c, radius * 3, 0.8f, 12);
				for (LivingEntity v : disc(sl, c, radius, 1.0, 2.5)) {
					if (!v.onGround() && v.getY() > m.getY() + 0.9) {
						continue;
					}
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, 1.2, 0.5);
				}
			};
			r.every(windup + 6, windup + air + 14, 1, i -> {
				if (m.onGround()) {
					land.run();
				}
			});
			r.at(windup + air + 15, land);
		}
	}

	/** Stance in which damage is refused and a share of it thrown back at the attacker. */
	static final class Reflect extends Attack {
		Reflect(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 14);
			int dur = p.i("duration", 80);
			int recover = p.i("recover", 14);
			double pct = p.d("pct", 0.5);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + dur + recover;
			r.movable = p.b("move", true);
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 1.3f, 0.5f));
			r.every(0, windup - 1, 2, i -> Styles.rim(sl, m.position().add(0, m.getBbHeight() * 0.4, 0), m.getBbWidth() * (1.8 - i * 0.05), style, 16));
			r.at(windup, () -> {
				m.setShield(dur, pct);
				Styles.burst(sl, m.position().add(0, m.getBbHeight() * 0.5, 0), style, m.getBbWidth(), 40);
			});
			r.onEnd(m::clearShield);
		}
	}
}
