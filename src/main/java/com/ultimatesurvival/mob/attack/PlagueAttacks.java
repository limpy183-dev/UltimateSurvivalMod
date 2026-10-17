package com.ultimatesurvival.mob.attack;

import static com.ultimatesurvival.mob.attack.Attacks.ground;
import static com.ultimatesurvival.mob.attack.Attacks.hurt;
import static com.ultimatesurvival.mob.attack.Attacks.muzzle;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Plague-themed attacks (Morbane): spore pods lobbed over the arena that leave poison clouds, and an infection that bursts unless it is washed or drunk away. */
final class PlagueAttacks {
	private PlagueAttacks() {
	}

	static void register(Map<String, Function<MobDef.AttackSpec, Attack>> t) {
		t.put("spores", Spores::new);
		t.put("infest", Infest::new);
	}

	// ---- spores: pods arc over the arena and burst into clouds that linger ----------------------------------------------------------------------------------------
	static final class Spores extends Attack {
		Spores(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 30);
			int recover = p.i("recover", 28);
			int count = Math.max(1, p.i("count", 5));
			int flight = Math.max(8, p.i("flight", 28));
			double radius = p.d("radius", 3.6);
			int life = p.i("life", 220);
			double spread = p.d("spread", 9);
			double arc = p.d("arc", 5.0);
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<Vec3> pts = new ArrayList<>();
			pts.add(ground(sl, r.aim));
			for (int i = 1; i < count; i++) {
				double a = sl.getRandom().nextDouble() * Math.PI * 2;
				double d = radius + sl.getRandom().nextDouble() * spread;
				pts.add(ground(sl, r.aim.add(Math.cos(a) * d, 0, Math.sin(a) * d)));
			}
			r.windup = windup;
			r.total = windup + (count - 1) * 4 + flight + recover;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.4f, 0.6f));
			r.every(0, windup - 1, 3, i -> {
				Vec3 mz = muzzle(m);
				Fx.burst(sl, mz, Styles.accent(style), 6, 0.5 + i * 0.03, 0.02);
				Fx.burst(sl, mz, ParticleTypes.SPORE_BLOSSOM_AIR, 4, 0.6, 0.02);
			});
			for (int k = 0; k < count; k++) {
				final Vec3 to = pts.get(k);
				int launch = windup + k * 4;
				Vec3[] from = {Vec3.ZERO};
				r.at(launch, () -> {
					from[0] = muzzle(m);
					Fx.sound(sl, from[0], SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 1.2f, 0.6f);
				});
				r.every(launch, launch + flight - 1, 1, i -> {
					double f = (i + 1.0) / flight;
					Vec3 q = from[0].add(to.subtract(from[0]).scale(f)).add(0, arc * Math.sin(Math.PI * f), 0);
					sl.sendParticles(Styles.dust(style, 2.0f), q.x, q.y, q.z, 3, 0.1, 0.1, 0.1, 0.0);
					sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, q.x, q.y, q.z, 2, 0.15, 0.15, 0.15, 0.0);
					if (i % 3 == 0) {
						Styles.rim(sl, to, radius, style, 16);
					}
				});
				r.at(launch + flight, () -> {
					Styles.burst(sl, to.add(0, 0.6, 0), style, radius * 0.4, 44);
					Fx.shockwave(sl, to, radius, 3, Styles.dust(style, 1.8f), 28);
					Fx.sound(sl, to, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.2f, 0.5f);
					Shake.at(sl, to, 20, 0.25f, 5);
					for (LivingEntity v : Attacks.disc(sl, to, radius * 0.8, 1.2, 2.5)) {
						hurt(m, v, mult(), style);
						Dmg.shove(v, to, 0.5, 0.4);
					}
					m.addHazard(new Hazard(to, radius, life, p.d("tick", 0.3), p.i("tick_every", 10), style, p.s("effect", "poison"), p.i("effect_amp", 2)));
				});
			}
		}
	}

	// ---- infest: everyone in the arena is infected; unless the poison is washed off or cured, it bursts out of them and calls pods ---------------------------------------------
	static final class Infest extends Attack {
		Infest(MobDef.AttackSpec s) {
			super(s);
		}

		@Override
		public void begin(Run r) {
			int windup = p.i("windup", 24);
			int fuse = Math.max(30, p.i("fuse", 90));
			int recover = p.i("recover", 26);
			double reach = p.d("reach", 48);
			double radius = p.d("radius", 6);
			int pods = p.i("pods", 2);
			String pod = p.s("mob", "");
			String style = style();
			UsmMob m = r.self;
			ServerLevel sl = r.level;
			List<LivingEntity> carriers = new ArrayList<>();
			r.windup = windup;
			r.total = windup + fuse + recover;
			r.movable = true;
			r.at(0, () -> Fx.sound(sl, m.position(), Styles.warn(style), SoundSource.HOSTILE, 1.6f, 0.5f));
			r.every(0, windup - 1, 3, i -> Fx.burst(sl, muzzle(m), Styles.accent(style), 8, 0.9, 0.05));
			r.at(windup, () -> {
				for (LivingEntity v : Attacks.disc(sl, m.position(), reach, 10, 20)) {
					carriers.add(v);
					Effects.give(v, "poison", fuse + 20, 0);
					if (v instanceof ServerPlayer sp) {
						sp.sendOverlayMessage(Component.translatable("ultimatesurvival.boss.infested"));
					}
					Fx.line(sl, muzzle(m), v.getBoundingBox().getCenter(), Styles.dust(style, 1.0f), 0.9);
					Fx.burst(sl, v.position().add(0, v.getBbHeight() + 0.6, 0), Styles.dust(style, 1.2f), 10, 0.6, 0.05);
				}
				Fx.sound(sl, m.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.6f, 0.5f);
			});
			r.every(windup, windup + fuse - 1, 2, i -> {
				for (LivingEntity v : carriers) {
					if (!v.isAlive()) {
						continue;
					}
					if (v.isInWater() && v.hasEffect(MobEffects.POISON)) {                                    // washed off
						v.removeEffect(MobEffects.POISON);
						Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.SPLASH, 20, 0.4, 0.1);
						Fx.sound(sl, v.position(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 0.8f, 1.3f);
					}
					if (!v.hasEffect(MobEffects.POISON)) {
						continue;
					}
					double f = (double) i / fuse;
					Vec3 head = v.position().add(0, v.getBbHeight() + 0.4, 0);
					double a = i * 0.5;
					sl.sendParticles(Styles.dust(style, 1.3f), head.x + Math.cos(a) * 0.4, head.y, head.z + Math.sin(a) * 0.4, 1, 0, 0, 0, 0);
					sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, head.x, head.y - 0.5, head.z, 1, 0.3, 0.4, 0.3, 0);
					if (f > 0.7 && i % 6 == 0) {
						Styles.rim(sl, ground(sl, v.position()), radius * f, style, 20);
						Fx.sound(sl, v.position(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.HOSTILE, 0.8f, (float) (0.6 + f));
					}
				}
			});
			r.at(windup + fuse, () -> {
				for (LivingEntity v : carriers) {
					if (!v.isAlive()) {
						continue;
					}
					Vec3 at = v.position();
					if (!v.hasEffect(MobEffects.POISON)) {
						Fx.burst(sl, v.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 20, 0.5, 0.2);          // cured in time
						continue;
					}
					v.removeEffect(MobEffects.POISON);
					Styles.burst(sl, at.add(0, 1, 0), style, 1.5, 70);
					Fx.shockwave(sl, ground(sl, at), radius, 5, Styles.dust(style, 2.0f), 50);
					Fx.sound(sl, at, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.6f, 0.4f);
					Shake.at(sl, at, 30, 0.4f, 8);
					for (LivingEntity t : Attacks.disc(sl, at, radius, 1.5, 3.0)) {
						hurt(m, t, mult(), style);
					}
					if (!pod.isEmpty() && m.minionCount() < p.i("cap", 8)) {
						for (int k = 0; k < pods; k++) {
							double a = sl.getRandom().nextDouble() * Math.PI * 2;
							m.spawnMinion(pod, ground(sl, at.add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5)));
						}
					}
				}
			});
		}
	}
}
