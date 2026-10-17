package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.disc;
import static com.ultimatesurvival.mob.attack.Attacks.fireBolt;
import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.horizontalDistSqr;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;
import static com.ultimatesurvival.mob.attack.Attacks.muzzle;

import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.PosHistory;
import com.ultimatesurvival.util.Shake;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Attacks that belong to one boss theme: storm (lightning, gust, orbs), blood (leech), time (rewind, time stop). */
final class ThemedAttacks {
	private ThemedAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("lightning", Lightning::new);
		t.put("gust", Gust::new);
		t.put("orbs", Orbs::new);
		t.put("leech", Leech::new);
		t.put("rewind", Rewind::new);
		t.put("stop", TimeStop::new);
	}

	private static RandomSource rnd(Run r) {
		return r.level.getRandom();
	}

	// ---- lightning: yellow circles flash three times, then a real bolt falls ---------------------------------------------------
	static final class Lightning extends Attack {
		Lightning(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 20);
			int count = p.i("count", 6);
			int delay = p.i("delay", 27);
			int gap = p.i("interval", 8);
			double radius = p.d("radius", 2.4);
			double area = p.d("area", 9);
			int aimed = p.i("aimed", 2);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + (count - 1) * gap + delay + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2f, 1.4f));
			r.every(0, windup - 1, 3, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() + 0.5, 0), Styles.accent(style), 8, 1.4, 0.2));
			for (int k = 0; k < count; k++) {
				final int idx = k;
				r.at(windup + k * gap, () -> {
					LivingEntity t = r.target;
					Vec3 base = t != null && t.isAlive() ? t.position().add(t.getDeltaMovement().multiply(1, 0, 1).scale(delay * 0.6)) : r.aim;
					if (idx >= aimed) {
						double a = rnd(r).nextDouble() * Math.PI * 2;
						double d = rnd(r).nextDouble() * area;
						base = (t != null ? t.position() : r.aim).add(Math.cos(a) * d, 0, Math.sin(a) * d);
					}
					final Vec3 at = ground(sl, base);
					for (int pulse = 0; pulse < 3; pulse++) {                     // three flashes, the third one is the strike
						final int pp = pulse;
						r.at(r.t + (delay - 18) + pulse * 9, () -> {
							Styles.rim(sl, at, radius, style, 18 + pp * 8);
							Styles.beam(sl, at, 1.0 + pp * 1.5, style, 6 + pp * 6);
							Fx.sound(sl, at, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 0.8f, 0.8f + pp * 0.35f);
						});
					}
					r.every(r.t, r.t + delay - 20, 4, i -> Styles.rim(sl, at, radius, style, 12));
					r.at(r.t + delay, () -> {
						LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(sl, EntitySpawnReason.TRIGGERED);
						if (bolt != null) {
							bolt.snapTo(at.x, at.y, at.z);
							bolt.setVisualOnly(true);
							sl.addFreshEntity(bolt);
						}
						Styles.burst(sl, at.add(0, 0.6, 0), style, radius * 0.5, 40);
						Shake.at(sl, at, 14, 0.4f, 6);
						for (LivingEntity v : disc(sl, at, radius, 1.5, 6.0)) {
							hurt(m, v, mult(), style);
							Effects.give(v, "slowness", 30, 2);
							Dmg.shove(v, at, 0.5, 0.45);
						}
					});
				});
			}
		}
	}

	// ---- gust: a wall of wind that shoves (or drags) everything away from the boss ---------------------------------------------------------
	static final class Gust extends Attack {
		Gust(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 24);
			int recover = p.i("recover", 20);
			double radius = p.d("radius", 14);
			double kb = p.d("kb", 2.0);
			boolean pull = p.b("pull", false);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + 6 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.5f, 0.6f));
			r.every(0, windup - 1, 2, i -> {
				double f = pull ? (i * 2.0 + 2) / windup : 1.0 - (i * 2.0) / windup;
				Styles.rim(sl, m.position(), radius * Math.max(0.1, f), style, 24);
				Fx.ring(sl, m.position(), radius * Math.max(0.1, f), 20, ParticleTypes.CLOUD, 0.6);
			});
			r.at(windup, () -> {
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 6, ParticleTypes.CLOUD, 50);
				Fx.shockwave(sl, c, radius, 6, Styles.dust(style, 1.8f), 40);
				Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 1.5f, 0.8f);
				Shake.at(sl, c, radius * 2, 0.5f, 10);
				double drop = Math.max(0, c.y - ground(sl, c).y);              // a hovering boss still reaches the ground below it
				for (LivingEntity v : disc(sl, c, radius, drop + 3.0, 5.0)) {
					hurt(m, v, mult(), style);
					Dmg.shove(v, c, pull ? -kb : kb, 0.5);
				}
			});
		}
	}

	// ---- orbs: slow homing spheres of energy ----------------------------------------------------------------------------------------------------
	static final class Orbs extends Attack {
		Orbs(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 24);
			int recover = p.i("recover", 20);
			int count = p.i("count", 3);
			int gap = p.i("interval", 14);
			double speed = p.d("speed", 0.45);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + (count - 1) * gap + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.2f, 1.0f));
			r.every(0, windup - 1, 3, i -> Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.7, 0), Styles.accent(style), 6, 1.2, 0.1));
			for (int k = 0; k < count; k++) {
				final int idx = k;
				r.at(windup + k * gap, () -> {
					double a = idx * Math.PI * 2 / Math.max(1, count) + rnd(r).nextDouble();
					Vec3 from = m.position().add(Math.cos(a) * 2.2, m.getBbHeight() * 0.75, Math.sin(a) * 2.2);
					BoltSpec spec = new BoltSpec(style, (float) m.rawDmg(mult()), (float) speed, p.i("life", 150), 0, true, 0.0, false,
							(float) p.d("aoe", 2.0), p.s("trail", "dust"), (float) p.d("size", 1.9));
					LivingEntity tg = r.target;
					Vec3 toward = tg != null && tg.isAlive() ? tg.getBoundingBox().getCenter().subtract(from).normalize() : new Vec3(Math.cos(a), 0, Math.sin(a));
					Bolt.fire(sl, m, from, new Vec3(Math.cos(a) * 0.5 + toward.x, 0.45, Math.sin(a) * 0.5 + toward.z).normalize(), spec);
					Fx.sound(sl, from, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0f, 1.3f);
				});
			}
		}
	}

	// ---- leech: a beam that drains health into the boss; breaking line of sight breaks the beam ------------------------------------------------------------
	static final class Leech extends Attack {
		Leech(MobDef.AttackSpec s) {
			super(s);
		}

		static boolean clear(ServerLevel sl, Vec3 from, Vec3 to, UsmMob who) {
			return sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who)).getType() == HitResult.Type.MISS;
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 24);
			int dur = p.i("duration", 60);
			int step = p.i("step", 4);
			int recover = p.i("recover", 20);
			double range = p.d("beam", 20);                     // beam length ("range" is the usable-distance band)
			double heal = p.d("heal", 0.7);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + dur + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.3f, 0.7f));
			r.every(0, windup - 1, 2, i -> {
				LivingEntity v = r.target;
				if (v != null && v.isAlive()) {
					m.getLookControl().setLookAt(v, 40f, 40f);
					Fx.line(sl, muzzle(m), v.getBoundingBox().getCenter(), Styles.dust(style, 0.8f), 0.9);
				}
			});
			r.every(windup, windup + dur - 1, step, i -> {
				LivingEntity v = r.target;
				if (v == null || !v.isAlive()) {
					return;
				}
				m.getLookControl().setLookAt(v, 40f, 40f);
				Vec3 from = muzzle(m);
				Vec3 to = v.getBoundingBox().getCenter();
				boolean los = clear(sl, from, to, m);
				Fx.line(sl, from, to, Styles.dust(style, los ? 1.3f : 0.7f), los ? 0.35 : 1.0);
				if (los) {
					Fx.line(sl, from, to, Styles.accent(style), 0.8);
				}
				if (los && from.distanceTo(to) <= range && Attacks.isVictim(v)) {
					double dealt = m.dmg(mult());
					hurt(m, v, mult(), style);
					m.heal((float) (dealt * heal));
					Fx.burst(sl, to, ParticleTypes.DAMAGE_INDICATOR, 3, 0.2, 0.1);
					Fx.burst(sl, from, ParticleTypes.HEART, 1, 0.3, 0.0);
					Fx.sound(sl, to, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.9f, 1.4f);
				}
			});
		}
	}

	// ---- rewind: everyone is snapped back to where they stood three seconds ago --------------------------------------------------------------------------------
	static final class Rewind extends Attack {
		Rewind(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 40);
			int recover = p.i("recover", 20);
			int back = p.i("back", 60);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Map<ServerPlayer, Vec3> marks = new HashMap<>();
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> {
				Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.4f, 0.5f);
				for (ServerPlayer pl : sl.players()) {
					if (Attacks.isVictim(pl) && pl.distanceToSqr(m) < 48 * 48) {
						Vec3 old = PosHistory.get(pl, back);
						if (old != null) {
							marks.put(pl, old);
						}
					}
				}
			});
			r.every(0, windup - 1, 3, i -> {
				for (Map.Entry<ServerPlayer, Vec3> e : marks.entrySet()) {
					Styles.rim(sl, e.getValue(), 1.2, style, 14);
					Styles.beam(sl, e.getValue(), 2.2, style, 8);
					Fx.line(sl, e.getKey().position().add(0, 1, 0), e.getValue().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 0.8);
				}
			});
			r.at(windup, () -> {
				for (Map.Entry<ServerPlayer, Vec3> e : marks.entrySet()) {
					ServerPlayer pl = e.getKey();
					Vec3 to = e.getValue();
					if (!pl.isAlive() || !sl.noCollision(pl, pl.getBoundingBox().move(to.subtract(pl.position())))) {
						continue;
					}
					Fx.burst(sl, pl.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 30, 0.4, 0.3);
					pl.teleportTo(to.x, to.y, to.z);
					pl.fallDistance = 0;
					Effects.give(pl, "slowness", 20, 1);
					Fx.burst(sl, to.add(0, 1, 0), Styles.accent(style), 24, 0.4, 0.2);
					Fx.sound(sl, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);
					hurt(m, pl, mult(), style);
				}
				Shake.at(sl, m.position(), 40, 0.6f, 10);
			});
		}
	}

	// ---- time stop: a ring of bolts freezes around you, then all of them fly at once through the one gap ----------------------------------------------------
	static final class TimeStop extends Attack {
		TimeStop(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int hold = p.i("hold", 50);
			int recover = p.i("recover", 30);
			int count = p.i("count", 14);
			double radius = p.d("radius", 6.5);
			double speed = p.d("speed", 0.9);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + hold + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.6f, 0.5f));
			r.every(0, windup - 1, 3, i -> Styles.rim(sl, m.position(), 3 + i * 0.4, style, 24));
			r.at(windup, () -> {
				Fx.sound(sl, m.position(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.6f, 0.6f);
				List<ServerPlayer> victims = sl.players().stream().filter(pl -> Attacks.isVictim(pl) && pl.distanceToSqr(m) < 40 * 40).toList();
				for (ServerPlayer v : victims) {
					Effects.give(v, "slowness", hold, 3);
					int gapAt = rnd(r).nextInt(count);
					for (int k = 0; k < count; k++) {
						if (k == gapAt || k == (gapAt + 1) % count) {
							continue;                                           // the way out
						}
						double a = k * Math.PI * 2 / count;
						Vec3 at = v.position().add(Math.cos(a) * radius, 1.0, Math.sin(a) * radius);
						Vec3 dir = new Vec3(-Math.cos(a), 0, -Math.sin(a));
						Bolt b = fireBolt(m, at, dir, style, mult(), speed, hold + 70, 0.8, 1.2, "dust");
						b.hold(hold, v);
					}
					Styles.rim(sl, v.position(), radius, style, 40);
					Fx.burst(sl, v.position().add(0, 1, 0), ParticleTypes.SOUL_FIRE_FLAME, 30, 1.0, 0.02);
				}
			});
			r.every(windup, windup + hold - 1, 6, i -> {
				for (ServerPlayer v : sl.players()) {
					if (Attacks.isVictim(v) && v.distanceToSqr(m) < 40 * 40) {
						Fx.burst(sl, v.position().add(0, 1, 0), ParticleTypes.ENCHANTED_HIT, 3, 0.7, 0.01);
					}
				}
			});
			r.at(windup + hold, () -> Fx.sound(sl, m.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6f, 0.5f));
		}
	}

	@SuppressWarnings("unused")
	private static boolean near(Vec3 a, Vec3 b, double r) {
		return horizontalDistSqr(a, b) <= r * r;
	}
}
