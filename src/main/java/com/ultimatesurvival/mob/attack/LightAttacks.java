package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.horizontal;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;
import static com.ultimatesurvival.mob.attack.Attacks.muzzle;
import static com.ultimatesurvival.mob.attack.Attacks.rotateY;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Light-themed attacks: sweeping and spinning beams, the supernova, a blinding flash you can look away from, and a crack that splits the arena. */
final class LightAttacks {
	private LightAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("beam", Beam::new);
		t.put("supernova", Supernova::new);
		t.put("flash", Flash::new);
		t.put("sunder", Sunder::new);
	}

	/** First point along {@code from -> to} blocked by terrain, or {@code to} when the way is clear. */
	static Vec3 stop(ServerLevel sl, Vec3 from, Vec3 to, UsmMob who) {
		var hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who));
		return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
	}

	static boolean clear(ServerLevel sl, Vec3 from, Vec3 to, UsmMob who) {
		return sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who)).getType() == HitResult.Type.MISS;
	}

	// ---- beam: one or more rays sweep across (or spin around) the arena; terrain stops them, so cover works ---------------------------------------------------
	static final class Beam extends Attack {
		Beam(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int dur = p.i("duration", 70);
			int step = Math.max(1, p.i("step", 3));
			int recover = p.i("recover", 24);
			int arms = Math.max(1, p.i("arms", 1));
			double sweep = Math.toRadians(p.d("sweep", 120));
			double spin = Math.toRadians(p.d("spin", 0));
			double reach = p.d("beam", 40);
			double half = p.d("width", 1.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			boolean low = p.b("low", false) && !m.def().fly;              // a low beam is a slab you can jump
			double dirSign = r.level.getRandom().nextBoolean() ? 1 : -1;
			Vec3[] base = {new Vec3(0, 0, 1)};
			r.windup = windup;
			r.total = windup + dur + recover;
			Runnable aim = () -> {
				LivingEntity t = r.target;
				Vec3 at = t != null && t.isAlive() ? t.getBoundingBox().getCenter() : r.aim.add(0, 1, 0);
				Vec3 from = low ? m.position().add(0, 0.6, 0) : muzzle(m);
				Vec3 d = at.subtract(from);
				base[0] = low ? horizontal(from, at) : d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
			};
			r.at(0, () -> {
				Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.5f, 0.8f);
				aim.run();
			});
			r.every(0, windup - 1, 2, i -> {
				LivingEntity t = r.target;
				if (t != null && t.isAlive() && i < windup / 2 - 2) {
					m.getLookControl().setLookAt(t, 40f, 40f);
					aim.run();
				}
				Vec3 from = low ? m.position().add(0, 0.6, 0) : muzzle(m);
				double f = (i * 2.0 + 2) / windup;
				for (int a = 0; a < arms; a++) {
					Vec3 d = rotateY(base[0], a * Math.PI * 2 / arms);
					Fx.line(sl, from, stop(sl, from, from.add(d.scale(reach * Math.min(1.0, 0.25 + f))), m), Styles.dust(style, 0.6f), 1.6);
				}
				Fx.burst(sl, from, Styles.accent(style), 4, 0.25 + 0.5 * f, 0.02);
				Fx.burst(sl, from, Styles.dust(style, 1.0f + (float) f * 1.6f), 3, 0.2, 0.0);
			});
			r.every(windup, windup + dur - 1, 1, i -> {
				Vec3 from = low ? m.position().add(0, 0.6, 0) : muzzle(m);
				double angle = spin > 0 ? spin * i : dirSign * sweep * ((double) i / Math.max(1, dur - 1) - 0.5);
				if (i == 0) {
					Fx.sound(sl, from, Styles.impact(style), SoundSource.HOSTILE, 1.4f, 1.2f);
				}
				if (i % 6 == 0) {
					Fx.sound(sl, from, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0f, 1.6f);
				}
				for (int a = 0; a < arms; a++) {
					Vec3 d = rotateY(base[0], angle + a * Math.PI * 2 / arms);
					Vec3 end = stop(sl, from, from.add(d.scale(reach)), m);
					if (i % 2 == 0 || arms == 1) {
						double pitch = arms > 2 ? 1.1 : 0.7;
						Fx.line(sl, from, end, Styles.accent(style), pitch);
						Fx.line(sl, from, end, Styles.dust(style, 1.3f), pitch * 2.2);
					}
					if (end.distanceToSqr(from.add(d.scale(reach))) > 1.0) {                       // the ray ends on terrain: a splash where it lands
						Fx.burst(sl, end, Styles.accent(style), 3, 0.3, 0.05);
					}
					if (i % step != 0) {
						continue;
					}
					AABB box = new AABB(from, end).inflate(half + 1.5);
					for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, box, Attacks::isVictim)) {
						Vec3 c = v.getBoundingBox().getCenter();
						if (Aim.distToSegment(c, from, end) > half + v.getBbWidth() * 0.5) {
							continue;
						}
						if (low && !(v.getBoundingBox().minY < from.y + 0.45 && v.getBoundingBox().maxY > from.y - 0.4)) {
							continue;                                                                   // jumped it
						}
						hurt(m, v, mult(), style);
						Fx.burst(sl, c, Styles.accent(style), 6, 0.3, 0.1);
					}
				}
			});
		}
	}

	// ---- supernova: a star swells around the boss; only distance or cover saves you, and spending it leaves the boss exhausted ----------------------------------------
	static final class Supernova extends Attack {
		Supernova(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 70);
			int exhaust = p.i("exhaust", 90);
			int recover = p.i("recover", 20);
			double radius = p.d("radius", 34);
			double falloff = p.d("falloff", 0.55);
			double vuln = p.d("vuln", 1.8);
			int blind = p.i("blind", 60);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + exhaust + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0f, 0.5f));
			r.every(0, windup - 1, 2, i -> {
				double f = (i * 2.0 + 2) / windup;
				Vec3 c = m.position().add(0, m.getBbHeight() * 0.55, 0);
				double rr = m.getBbWidth() * 0.7 + 2.5 * f;
				for (int k = 0; k < 14; k++) {
					Vec3 d = new Vec3(sl.getRandom().nextGaussian(), sl.getRandom().nextGaussian(), sl.getRandom().nextGaussian()).normalize().scale(rr);
					sl.sendParticles(k % 2 == 0 ? Styles.accent(style) : Styles.dust(style, 1.6f), c.x + d.x, c.y + d.y, c.z + d.z, 1, 0, 0, 0, 0);
				}
				if (i % 4 == 0) {
					Styles.rim(sl, Attacks.ground(sl, m.position()), radius, style, 70);                       // the edge of the blast: stay beyond it or behind something
					Fx.sound(sl, c, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4f, (float) (0.5 + f * 1.3));
				}
			});
			r.at(windup, () -> {
				Vec3 c = m.position().add(0, m.getBbHeight() * 0.55, 0);
				Fx.burst(sl, c, ParticleTypes.EXPLOSION_EMITTER, 1, 0.0, 0.0);
				Fx.burst(sl, c, ParticleTypes.END_ROD, 160, 4.0, 0.9);
				Fx.burst(sl, c, Styles.dust(style, 3.0f), 160, 5.0, 0.4);
				Fx.shockwave(sl, Attacks.ground(sl, m.position()), radius, 10, Styles.dust(style, 2.2f), 80);
				Fx.sound(sl, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0f, 0.5f);
				Fx.sound(sl, c, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3.0f, 0.5f);
				Shake.at(sl, c, radius * 2, 1.0f, 30);
				ScreenFlash.at(sl, c, radius * 2, 0xFFFFFF, 0.8f, 30);
				for (LivingEntity v : VoidAttacks.sphere(sl, c, radius)) {
					Vec3 to = v.getBoundingBox().getCenter();
					if (!clear(sl, c, to, m)) {
						Fx.burst(sl, to, ParticleTypes.POOF, 6, 0.4, 0.05);                                       // shielded by terrain
						continue;
					}
					double f = 1.0 - (1.0 - falloff) * (c.distanceTo(to) / radius);
					hurt(m, v, mult() * f, style);
					Effects.give(v, "blindness", blind, 0);
					Dmg.shove(v, c, 1.4 * f + 0.3, 0.5);
				}
				m.setVuln(exhaust, vuln);
			});
			r.onEnd(m::clearVuln);
		}
	}

	// ---- flash: a blinding burst; only those looking at it are caught ----------------------------------------------------------------------------------------
	static final class Flash extends Attack {
		Flash(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 40);
			int recover = p.i("recover", 20);
			int blind = p.i("blind", 100);
			double range = p.d("beam", 60);
			double cone = Math.cos(Math.toRadians(p.d("cone", 80)) / 2);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			r.windup = windup;
			r.total = windup + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.8f, 1.0f));
			r.every(0, windup - 1, 2, i -> {
				double f = (i * 2.0 + 2) / windup;
				Vec3 c = muzzle(m);
				Fx.burst(sl, c, Styles.accent(style), 3 + (int) (8 * f), 0.2 + 0.6 * f, 0.02);
				Fx.burst(sl, c, Fx.dust(0xFFFFFF, 1.0f + (float) f * 2.0f), 2 + (int) (6 * f), 0.15 + 0.4 * f, 0.0);
				if (i % 5 == 0) {
					Fx.sound(sl, c, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 1.0f, (float) (0.8 + f));
				}
			});
			r.at(windup, () -> {
				Vec3 c = muzzle(m);
				Fx.burst(sl, c, ParticleTypes.END_ROD, 120, 2.5, 0.6);
				Fx.burst(sl, c, Fx.dust(0xFFFFFF, 3.5f), 100, 3.0, 0.3);
				Fx.sound(sl, c, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0f, 1.8f);
				Fx.sound(sl, c, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0f, 1.5f);
				for (LivingEntity v : VoidAttacks.sphere(sl, c, range)) {
					Vec3 eye = v.getEyePosition();
					Vec3 toBoss = c.subtract(eye);
					if (toBoss.lengthSqr() < 1e-4 || !clear(sl, c, eye, m)) {
						continue;
					}
					if (v.getLookAngle().dot(toBoss.normalize()) < cone) {
						continue;                                                                               // looking away: spared
					}
					hurt(m, v, mult(), style);
					Effects.give(v, "blindness", blind, 0);
					Effects.give(v, "slowness", blind / 2, 1);
					if (v instanceof net.minecraft.server.level.ServerPlayer sp) {
						ScreenFlash.to(sp, 0xFFFFFF, 0.8f, 36);                                               // only those who looked into it
					}
				}
			});
		}
	}

	// ---- sunder: a crack races across the floor, erupts, and keeps burning where it passed ----------------------------------------------------------------------------
	static final class Sunder extends Attack {
		Sunder(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 40);
			int recover = p.i("recover", 26);
			int life = p.i("life", 140);
			double length = p.d("length", 44);
			double width = p.d("width", 3.0);
			double speed = p.d("speed", 2.4);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 start = m.position();
			Vec3 dir = horizontal(start, r.aim);
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			int waveTicks = (int) Math.ceil(length / speed);
			Set<Integer> hit = new HashSet<>();
			r.windup = windup;
			r.total = windup + waveTicks + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.6f));
			r.every(0, windup - 1, 3, i -> {
				double f = (i * 3.0 + 3) / windup;
				for (double d = 2; d < length; d += 2.4) {
					Vec3 c = Attacks.ground(sl, start.add(dir.scale(d)));
					sl.sendParticles(Styles.dust(style, 1.4f), c.x + side.x * width, c.y + 0.15, c.z + side.z * width, 1, 0, 0, 0, 0);
					sl.sendParticles(Styles.dust(style, 1.4f), c.x - side.x * width, c.y + 0.15, c.z - side.z * width, 1, 0, 0, 0, 0);
					if (f > 0.5 && sl.getRandom().nextInt(3) == 0) {
						sl.sendParticles(Styles.accent(style), c.x, c.y + 0.2, c.z, 1, 0.2, 0.1, 0.2, 0.02);
					}
				}
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.8, 0), Styles.accent(style), 5, 0.8, 0.1);
			});
			r.every(windup, windup + waveTicks, 1, i -> {
				double front = (i + 1) * speed;
				Vec3 c = Attacks.ground(sl, start.add(dir.scale(front)));
				Fx.column(sl, c.add(side.scale(width * 0.5)), 5, Styles.dust(style, 2.0f), 8);
				Fx.column(sl, c.subtract(side.scale(width * 0.5)), 5, Styles.accent(style), 6);
				Styles.burst(sl, c.add(0, 0.5, 0), style, width * 0.5, 14);
				if (i % 3 == 0) {
					Fx.sound(sl, c, Styles.impact(style), SoundSource.HOSTILE, 0.8f, 1.2f);
					Shake.at(sl, c, 20, 0.4f, 6);
				}
				for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(width + speed + 1, 5, width + speed + 1), Attacks::isVictim)) {
					Vec3 rel = v.position().subtract(start);
					double along = rel.dot(dir);
					double lateral = Math.abs(rel.dot(side));
					if (along >= front - speed - 0.5 && along <= front + 0.8 && lateral <= width + v.getBbWidth() * 0.5 && hit.add(v.getId())) {
						hurt(m, v, mult(), style);
						Dmg.shove(v, v.position().subtract(side.scale(rel.dot(side) >= 0 ? 1 : -1)), 0.5, 0.9);
					}
				}
			});
			r.at(windup + waveTicks, () -> {
				for (double d = width; d < length; d += width * 1.8) {
					m.addHazard(new Hazard(Attacks.ground(sl, start.add(dir.scale(d))), width * 0.85, life, p.d("tick", 0.3), 10, style));
				}
			});
		}
	}
}
