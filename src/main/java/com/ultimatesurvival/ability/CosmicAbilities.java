package com.ultimatesurvival.ability;

import com.ultimatesurvival.mob.attack.Attacks;
import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 6 (Astraeus) abilities: falling stars, a beam from orbit, a ring of stars that cuts what touches it, and plain flight. */
final class CosmicAbilities {
	private CosmicAbilities() {
	}

	/** A star comes down: a streak from the sky, then a blast. */
	static void meteor(ServerLevel l, ServerPlayer p, Vec3 at, double blast, float dmg, int col) {
		Vec3 top = at.add(7, 34, 3);
		Fx.line(l, top, at, Fx.dust(col, 1.8f), 0.55);
		Fx.line(l, top.add(0.4, 0.3, 0), at, ParticleTypes.END_ROD, 1.4);
		Tasks.later(2, () -> {
			Fx.burst(l, at.add(0, 0.5, 0), ParticleTypes.EXPLOSION, 2, blast * 0.3, 0);
			Fx.burst(l, at.add(0, 0.8, 0), Fx.dust(col, 2.4f), 40, blast * 0.35, 0.3);
			Fx.burst(l, at.add(0, 0.8, 0), ParticleTypes.END_ROD, 16, blast * 0.3, 0.3);
			Fx.shockwave(l, at, blast, 4, Fx.dust(col, 1.8f), 36);
			Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 0.7f);
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, blast)) {
				Dmg.extra(l, p, e, dmg);
				Dmg.shove(e, at, 0.9, 0.5);
			}
		});
	}

	static void register() {
		// stars rain over the place you look
		Abilities.active("meteor_shower", 420, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			Vec3 at = Aim.point(l, p, c.p().d("range", 44));
			if (at == null) {
				return false;
			}
			final Vec3 centre = Attacks.ground(l, at);
			double radius = c.p().d("radius", 12);
			int count = c.p().i("count", 16);
			int ticks = c.p().i("ticks", 80);
			double blast = c.p().d("blast", 4.5);
			float dmg = (float) c.p().d("dmg", 220);
			int col = c.color(0x7B7BFF);
			Fx.sound(l, centre, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6f, 0.5f);
			Fx.ring(l, centre, radius, 80, Fx.dust(col, 1.6f), 0.2);
			for (int i = 0; i < count; i++) {
				Tasks.later(8 + (int) ((double) i / count * ticks), () -> {
					var rnd = l.getRandom();
					double a = rnd.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(rnd.nextDouble()) * radius;
					meteor(l, p, Attacks.ground(l, centre.add(Math.cos(a) * d, 0, Math.sin(a) * d)), blast, dmg, col);
				});
			}
			return true;
		});

		// one enormous beam from orbit, after a moment's warning
		Abilities.active("orbital_strike", 500, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			Vec3 at = Aim.point(l, p, c.p().d("range", 50));
			if (at == null) {
				return false;
			}
			final Vec3 centre = Attacks.ground(l, at);
			double radius = c.p().d("radius", 8);
			float dmg = (float) c.p().d("dmg", 1200);
			int col = c.color(0x7B7BFF);
			int delay = c.p().i("delay", 22);
			Fx.sound(l, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.8f, 0.5f);
			for (int k = 0; k < delay; k += 2) {
				final double f = 1.0 - (double) k / delay;
				Tasks.later(k, () -> {
					Fx.ring(l, centre, Math.max(0.8, radius * f), 36, Fx.dust(col, 1.5f), 0.15);
					Fx.column(l, centre, 4 + 20 * (1 - f), Fx.dust(0xFFFFFF, 1.0f), 8);
				});
			}
			Tasks.later(delay, () -> {
				Fx.column(l, centre, 44, Fx.dust(col, 3.0f), 90);
				Fx.column(l, centre, 44, ParticleTypes.END_ROD, 60);
				Fx.burst(l, centre.add(0, 1, 0), ParticleTypes.EXPLOSION_EMITTER, 1, 0, 0);
				Fx.burst(l, centre.add(0, 1, 0), ParticleTypes.END_ROD, 120, radius * 0.4, 0.6);
				Fx.shockwave(l, centre, radius * 1.6, 8, Fx.dust(col, 2.4f), 80);
				Fx.sound(l, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.5f);
				Fx.sound(l, centre, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.8f, 0.6f);
				Shake.at(l, centre, 60, 0.8f, 20);
				ScreenFlash.at(l, centre, 24, col, 0.35f, 10);
				for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius * 1.6)) {
					double d = e.position().distanceTo(centre);
					Dmg.extra(l, p, e, dmg * (float) (d <= radius ? 1.0 : 0.4));
					Dmg.shove(e, centre, 1.4 - 0.5 * d / (radius * 1.6), 0.8);
				}
			});
			return true;
		});

		// each hit may call a star down on the target
		Abilities.onHit("starfall", (c, t) -> {
			if (c.player().getRandom().nextDouble() > c.p().d("chance", 0.3)) {
				return;
			}
			meteor(c.level(), c.player(), t.position(), c.p().d("radius", 4), (float) c.p().d("dmg", 120), c.color(0x7B7BFF));
		});

		// worn: stars circle you and cut whatever they touch
		Abilities.passive("star_aura").tick = (p, r) -> {
			if (!(p.level() instanceof ServerLevel l)) {
				return;
			}
			int count = Math.max(1, r.i("count", 5));
			double orbit = r.d("orbit", 2.8);
			double t = p.tickCount * 0.16;
			Vec3[] stars = new Vec3[count];
			for (int i = 0; i < count; i++) {
				double a = t + i * Math.PI * 2 / count;
				stars[i] = p.position().add(Math.cos(a) * orbit, 1.0 + 0.5 * Math.sin(t * 1.7 + i * 1.3), Math.sin(a) * orbit);
				if (p.tickCount % 2 == 0) {
					l.sendParticles(ParticleTypes.END_ROD, stars[i].x, stars[i].y, stars[i].z, 1, 0, 0, 0, 0);
					l.sendParticles(Fx.dust(0x7B7BFF, 1.6f), stars[i].x, stars[i].y, stars[i].z, 2, 0.08, 0.08, 0.08, 0);
				}
			}
			if (p.tickCount % 5 != 0) {
				return;
			}
			float dmg = (float) r.d("dmg", 60);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), orbit + 3)) {
				Vec3 c = e.getBoundingBox().getCenter();
				for (Vec3 s : stars) {
					if (s.distanceToSqr(c) < 2.2 * 2.2) {
						Dmg.extra(l, p, e, dmg);
						Fx.burst(l, c, ParticleTypes.END_ROD, 6, 0.3, 0.15);
						break;
					}
				}
			}
		};

		// worn: you fly (survival flight, no falling damage while it lasts)
		Ability flight = Abilities.passive("flight");
		flight.tick = (p, r) -> {
			if (!p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) {
				p.getAbilities().mayfly = true;
				p.onUpdateAbilities();
			}
			if (p.getAbilities().flying && p.level() instanceof ServerLevel l && p.tickCount % 3 == 0) {
				l.sendParticles(Fx.dust(0x7B7BFF, 1.3f), p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.25, 0.05, 0.25, 0);
			}
		};
		flight.onRemove = (p, r) -> {
			if (!p.isCreative() && !p.isSpectator()) {
				p.getAbilities().mayfly = false;
				p.getAbilities().flying = false;
				p.onUpdateAbilities();
				p.fallDistance = 0;
			}
		};
		flight.incoming = (p, src, d, r) -> src.is(net.minecraft.tags.DamageTypeTags.IS_FALL) ? 0 : d;
	}
}
