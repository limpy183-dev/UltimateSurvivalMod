package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Frost-themed attacks (Boreus): lines of ice that erupt outward, a whiteout that punishes standing still, and a deep freeze that is followed by spears. */
final class FrostAttacks {
	private FrostAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("icicles", Icicles::new);
		t.put("whiteout", Whiteout::new);
		t.put("glaciate", Glaciate::new);
	}

	/** Slows a victim to a crawl and frosts their screen (the vanilla powder-snow overlay) for {@code ticks}. */
	static void chill(LivingEntity v, int ticks, int slowAmp) {
		Effects.give(v, "slowness", ticks, slowAmp);
		v.setTicksFrozen(Math.max(v.getTicksFrozen(), v.getTicksRequiredToFreeze() + Math.min(ticks, 60)));
	}

	// ---- icicles: cracks race across the floor in every direction, then spikes erupt along them - stand in a gap ---------------------------------------------------
	static final class Icicles extends Attack {
		Icicles(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 34);
			int recover = p.i("recover", 22);
			int lines = Math.max(3, p.i("lines", 6));
			double length = p.d("length", 30);
			double speed = p.d("speed", 1.5);
			double width = p.d("width", 1.5);
			int waves = Math.max(1, p.i("waves", 1));
			int gap = p.i("gap", 14);
			int freeze = p.i("freeze", 40);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Vec3 origin = m.position();
			Vec3 toward = Attacks.horizontal(origin, r.aim);
			double base = Math.atan2(toward.z, toward.x) + Math.PI / lines;             // the first line misses the target: it begins in a gap
			int travel = (int) Math.ceil(length / speed);
			r.windup = windup;
			r.total = windup + (waves - 1) * gap + travel + recover;
			r.at(0, () -> Fx.sound(sl, origin, Styles.warn(style), SoundSource.HOSTILE, 1.5f, 0.6f));
			r.every(0, windup - 1, 2, i -> {
				double f = Math.min(1.0, (i * 2.0 + 2) / windup);
				for (int w = 0; w < waves; w++) {
					for (int k = 0; k < lines; k++) {
						double a = base + w * Math.PI / lines + k * Math.PI * 2 / lines;
						Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
						Vec3 from = ground(sl, origin.add(d.scale(2)));
						Vec3 to = ground(sl, origin.add(d.scale(2 + (length - 2) * f)));
						Fx.line(sl, from.add(0, 0.15, 0), to.add(0, 0.15, 0), w == 0 ? Styles.dust(style, 0.9f) : Styles.accent(style), 1.4);
					}
				}
				Fx.burst(sl, origin.add(0, m.getBbHeight() * 0.8, 0), ParticleTypes.SNOWFLAKE, 6, 1.0, 0.1);
			});
			for (int w = 0; w < waves; w++) {
				final int wave = w;
				Set<Long> hit = new HashSet<>();
				int t0 = windup + w * gap;
				r.every(t0, t0 + travel, 1, i -> {
					double prev = speed * i;
					double front = speed * (i + 1);
					for (int k = 0; k < lines; k++) {
						double a = base + wave * Math.PI / lines + k * Math.PI * 2 / lines;
						Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
						for (double dist = Math.max(2, prev); dist < front; dist += 1.0) {
							Vec3 c = ground(sl, origin.add(d.scale(dist)));
							Fx.column(sl, c, 3.2, Styles.dust(style, 1.8f), 7);
							Fx.burst(sl, c.add(0, 0.6, 0), ParticleTypes.SNOWFLAKE, 3, 0.4, 0.1);
						}
						Vec3 mid = origin.add(d.scale((prev + front) * 0.5));
						for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, new AABB(mid, mid).inflate(width + speed + 1, 4, width + speed + 1), Attacks::isVictim)) {
							Vec3 rel = v.position().subtract(origin);
							double along = rel.x * d.x + rel.z * d.z;
							double lateral = Math.abs(-rel.x * d.z + rel.z * d.x);
							long key = (long) v.getId() * 64 + k;
							if (along >= prev - 0.6 && along <= front + 0.9 && lateral <= width + v.getBbWidth() * 0.5 && hit.add(key)) {
								hurt(m, v, mult(), style);
								chill(v, freeze, 3);
								Dmg.shove(v, v.position().subtract(new Vec3(-d.z, 0, d.x)), 0.4, 0.8);
								Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 20, 0.4, 0.2);
							}
						}
					}
					if (i % 3 == 0) {
						Fx.sound(sl, origin, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8f, 0.7f + (float) (i % 5) * 0.1f);
						Shake.at(sl, origin, 40, 0.15f, 4);
					}
				});
			}
		}
	}

	// ---- whiteout: a blizzard fills the arena; anyone who stops moving is bitten by the cold -------------------------------------------------------------------------
	static final class Whiteout extends Attack {
		Whiteout(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int dur = p.i("duration", 160);
			int recover = p.i("recover", 24);
			double radius = p.d("radius", 40);
			int every = Math.max(4, p.i("interval", 10));
			int grace = p.i("grace", 30);
			double move = p.d("move", 1.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			Map<Integer, Vec3> last = new HashMap<>();
			Map<Integer, Integer> still = new HashMap<>();
			r.windup = windup;
			r.total = windup + dur + recover;
			r.at(0, () -> {
				Fx.sound(sl, m.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 2.0f, 0.5f);
				for (LivingEntity v : Attacks.disc(sl, m.position(), radius, 12, 24)) {
					if (v instanceof ServerPlayer sp) {
						sp.sendOverlayMessage(Component.translatable("ultimatesurvival.boss.whiteout"));
					}
				}
			});
			r.every(0, windup - 1, 2, i -> {
				double f = (i * 2.0 + 2) / windup;
				Fx.ring(sl, ground(sl, m.position()), radius * f, (int) (40 + 60 * f), Styles.dust(style, 1.8f), 0.4);
				Fx.burst(sl, m.position().add(0, m.getBbHeight() * 0.6, 0), ParticleTypes.SNOWFLAKE, 14, 2.0, 0.3);
			});
			r.every(windup, windup + dur - 1, 3, i -> {
				for (LivingEntity v : Attacks.disc(sl, m.position(), radius, 12, 24)) {
					var rnd = sl.getRandom();
					for (int k = 0; k < Fx.n(6); k++) {
						sl.sendParticles(k % 2 == 0 ? ParticleTypes.SNOWFLAKE : Styles.dust(style, 1.3f), v.getX() + (rnd.nextDouble() - 0.5) * 9, v.getY() + rnd.nextDouble() * 5,
								v.getZ() + (rnd.nextDouble() - 0.5) * 9, 1, 0.1, 0.1, 0.1, 0.02);
					}
				}
			});
			r.every(windup, windup + dur - 1, every, i -> {
				if (i == 0) {
					Fx.sound(sl, m.position(), Styles.impact(style), SoundSource.HOSTILE, 1.6f, 0.6f);
				}
				for (LivingEntity v : Attacks.disc(sl, m.position(), radius, 12, 24)) {
					int id = v.getId();
					Vec3 now = v.position();
					Vec3 prev = last.put(id, now);
					boolean moved = prev == null || Math.sqrt(Attacks.horizontalDistSqr(prev, now)) > move * every / 10.0 || Math.abs(prev.y - now.y) > 0.3;
					int st = moved ? Math.max(0, still.getOrDefault(id, 0) - every * 2) : still.getOrDefault(id, 0) + every;
					still.put(id, st);
					v.setTicksFrozen(Math.min(v.getTicksRequiredToFreeze() + 20, Math.max(v.getTicksFrozen(), st * 4)));      // the screen frosts over as the cold gathers
					if (st >= grace) {
						hurt(m, v, mult(), style);
						chill(v, 30, 1);
						Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 14, 0.4, 0.1);
						Fx.sound(sl, v.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0f, 1.0f);
					}
				}
			});
		}
	}

	// ---- glaciate: a freezing wave roots everyone it touches, and spears of ice drop on whoever is stuck -----------------------------------------------------------------
	static final class Glaciate extends Attack {
		Glaciate(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 34);
			int recover = p.i("recover", 24);
			double radius = p.d("radius", 11);
			int delay = p.i("delay", 30);
			int shards = Math.max(1, p.i("shards", 3));
			double shardRadius = p.d("shard_radius", 2.2);
			double shardMult = p.d("shard_dmg", 1.1);
			int freeze = p.i("freeze", 70);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<LivingEntity> rooted = new ArrayList<>();
			r.windup = windup;
			r.total = windup + delay + shards * 6 + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.8f, 0.5f));
			r.every(0, windup - 2, 2, i -> {
				double f = Math.min(1.0, (i * 2.0 + 2) / windup);
				Styles.rim(sl, m.position(), radius * f, style, (int) (14 + 22 * f));
				Fx.burst(sl, m.position().add(0, 0.4, 0), ParticleTypes.SNOWFLAKE, 6, radius * 0.3 * f, 0.05);
			});
			r.at(windup, () -> {
				Vec3 c = m.position();
				Fx.shockwave(sl, c, radius, 7, Styles.dust(style, 2.0f), 70);
				Fx.burst(sl, c.add(0, 0.6, 0), ParticleTypes.SNOWFLAKE, 120, radius * 0.4, 0.5);
				Fx.sound(sl, c, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
				Shake.at(sl, c, radius * 3, 0.7f, 12);
				for (LivingEntity v : Attacks.disc(sl, c, radius, 1.0, 2.5)) {
					if (!v.onGround() && v.getY() > m.getY() + 0.9) {
						continue;                                                                       // jumped it
					}
					hurt(m, v, mult(), style);
					chill(v, freeze, 6);
					rooted.add(v);
					Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 30, 0.5, 0.15);
				}
			});
			for (int s = 0; s < shards; s++) {
				final int idx = s;
				r.every(windup + delay - 12 + s * 6, windup + delay + s * 6 - 1, 3, i -> {
					for (LivingEntity v : rooted) {
						if (v.isAlive()) {
							Vec3 at = ground(sl, v.position());
							Styles.rim(sl, at, shardRadius, style, 14);
							Fx.column(sl, at, 1.0 + i, Styles.dust(style, 1.2f), 4);
						}
					}
				});
				r.at(windup + delay + s * 6, () -> {
					for (LivingEntity v : rooted) {
						if (!v.isAlive()) {
							continue;
						}
						Vec3 at = ground(sl, v.position());
						Fx.column(sl, at, 7, Styles.dust(style, 2.0f), 36);
						Fx.burst(sl, at.add(0, 0.6, 0), ParticleTypes.SNOWFLAKE, 30, 0.6, 0.3);
						Fx.sound(sl, at, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.6f + idx * 0.15f);
						for (LivingEntity t : Attacks.disc(sl, at, shardRadius, 1.0, 5.0)) {
							hurt(m, t, shardMult, style);
						}
					}
					Shake.at(sl, m.position(), 40, 0.3f, 6);
				});
			}
		}
	}
}
