package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Frozen;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 6 (Boreus) abilities: freezing solid, shattering what is frozen, blizzards, and a cold that follows you. */
final class FrostAbilities {
	private FrostAbilities() {
	}

	static void register() {
		// everything near you freezes solid
		Abilities.active("absolute_zero", 300, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 14);
			int ticks = c.p().i("ticks", 80);
			float dmg = (float) c.p().d("dmg", 120);
			int col = c.color(0x7FD8FF);
			Vec3 at = p.position();
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, radius)) {
				double d = e.position().distanceTo(at);
				Dmg.extra(l, p, e, dmg * (float) (1.0 - 0.4 * d / radius));
				Frozen.set(e, ticks);
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 24, 0.5, 0.12);
				Fx.burst(l, e.getBoundingBox().getCenter(), Fx.dust(col, 1.6f), 12, 0.5, 0.05);
			}
			Fx.shockwave(l, at, radius, 10, Fx.dust(col, 2.4f), 90);
			Fx.shockwave(l, at, radius * 0.7, 8, ParticleTypes.SNOWFLAKE, 70);
			Fx.shockwave(l, at, radius * 0.4, 6, ParticleTypes.END_ROD, 40);
			Fx.burst(l, at.add(0, 1.2, 0), ParticleTypes.SNOWFLAKE, 200, 3.2, 0.5);
			Fx.column(l, at, 7, Fx.dust(0xFFFFFF, 1.6f), 30);
			Fx.sound(l, at, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6f, 0.5f);
			Fx.sound(l, at, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.4f, 0.6f);
			Fx.sound(l, at, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
			Shake.at(l, at, radius * 2.5, 0.5f, 14);
			return true;
		});

		// a storm over the place you look: it chews and freezes everything inside until it blows out
		Abilities.active("blizzard", 360, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			Vec3 at = Aim.point(l, p, c.p().d("range", 40));
			if (at == null) {
				return false;
			}
			final Vec3 centre = com.ultimatesurvival.mob.attack.Attacks.ground(l, at);
			double radius = c.p().d("radius", 10);
			int ticks = c.p().i("ticks", 120);
			float dmg = (float) c.p().d("dmg", 40);
			int col = c.color(0x7FD8FF);
			Fx.sound(l, centre, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.6f, 0.5f);
			for (int k = 0; k < ticks; k++) {
				final int age = k;
				Tasks.later(k, () -> {
					var rnd = l.getRandom();
					for (int i = 0; i < Fx.n(10); i++) {
						double a = rnd.nextDouble() * Math.PI * 2;
						double d = Math.sqrt(rnd.nextDouble()) * radius;
						l.sendParticles(i % 3 == 0 ? Fx.dust(col, 1.5f) : ParticleTypes.SNOWFLAKE, centre.x + Math.cos(a) * d, centre.y + 0.3 + rnd.nextDouble() * 5.5,
								centre.z + Math.sin(a) * d, 1, 0.2, 0.2, 0.2, 0.05);
					}
					if (age % 6 == 0) {
						Fx.ring(l, centre, radius, 40, Fx.dust(0xFFFFFF, 1.4f), 0.2);
					}
					if (age % 5 == 0) {
						for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius)) {
							Dmg.extra(l, p, e, dmg);
							Effects.give(e, "slowness", 30, 3);
							if (age % 20 == 0) {
								Frozen.set(e, 24);
							}
						}
					}
					if (age % 24 == 0) {
						Fx.sound(l, centre, SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 1.2f, 0.6f);
					}
				});
			}
			Tasks.later(ticks, () -> {                                           // the storm breaks: whatever is still frozen shatters
				Fx.shockwave(l, centre, radius * 1.3, 6, Fx.dust(col, 2.0f), 60);
				Fx.burst(l, centre.add(0, 1.5, 0), ParticleTypes.SNOWFLAKE, 120, radius * 0.4, 0.5);
				Fx.sound(l, centre, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6f, 0.6f);
				for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius)) {
					Dmg.extra(l, p, e, dmg * (Frozen.is(e) ? 5f : 2f));
					Frozen.thaw(e);
				}
				Shake.at(l, centre, 40, 0.4f, 10);
			});
			return true;
		});

		// each hit may lock the target in ice
		Abilities.onHit("frostbite", (c, t) -> {
			if (c.player().getRandom().nextDouble() > c.p().d("chance", 0.35)) {
				return;
			}
			Frozen.set(t, c.p().i("ticks", 50));
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 20, 0.5, 0.1);
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), Fx.dust(c.color(0x7FD8FF), 1.4f), 10, 0.5, 0.05);
			Fx.sound(c.level(), t.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.8f, 1.2f);
		});

		// frozen enemies burst: a blow against ice lands several times as hard (and breaks it)
		Abilities.modify("shatter", (c, t, d) -> {
			if (!Frozen.is(t)) {
				return d;
			}
			Frozen.thaw(t);
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 40, 0.6, 0.35);
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), ParticleTypes.CRIT, 20, 0.5, 0.4);
			Fx.sound(c.level(), t.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
			return d * (float) c.p().d("mult", 3.0);
		});

		// worn: the cold you carry slows and wears down everything near you, and sometimes locks it solid
		Abilities.passive("frost_aura").tick = (p, r) -> {
			int every = Math.max(10, r.i("every", 20));
			if (!(p.level() instanceof ServerLevel l)) {
				return;
			}
			if (p.tickCount % 4 == 0) {
				double a = p.tickCount * 0.25;
				l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX() + Math.cos(a) * 1.4, p.getY() + 0.3 + (p.tickCount % 24) / 12.0, p.getZ() + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
			}
			if (p.tickCount % every != 0) {
				return;
			}
			double rad = r.d("radius", 9);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.extra(l, p, e, (float) r.d("dmg", 20));
				Effects.give(e, "slowness", every + 10, 2);
				if (p.getRandom().nextDouble() < r.d("freeze", 0.2)) {
					Frozen.set(e, 30);
				}
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 6, 0.3, 0.1);
			}
			Fx.ring(l, p.position(), rad, 36, Fx.dust(0x9FE8FF, 1.2f), 0.15);
		};

		// worn: whoever strikes you may be frozen on the spot, and your skin of ice takes the edge off the blow
		Abilities.passive("rime_shell").incoming = (p, src, d, r) -> {
			if (src.getEntity() instanceof LivingEntity att && att != p && p.level() instanceof ServerLevel l && p.getRandom().nextDouble() < r.d("chance", 0.5)) {
				Frozen.set(att, r.i("ticks", 40));
				Fx.burst(l, att.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 16, 0.4, 0.1);
				Fx.sound(l, p.position(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.7f, 1.4f);
			}
			return d * (float) (1.0 - r.d("pct", 0.1));
		};
	}
}
