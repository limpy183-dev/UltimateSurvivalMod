package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Tier 5 (Nihilus) abilities: black holes, gravity, tearing away health, and a cold that wears everything down. */
final class VoidAbilities {
	private VoidAbilities() {
	}

	static void register() {
		// a black hole where you look: drags enemies in, burns the core, collapses outward
		Abilities.active("singularity", 300, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double range = c.p().d("range", 30);
			Vec3 eye = c.eye();
			LivingEntity t = Aim.target(l, p, range, 0.9);
			Vec3 at;
			if (t != null) {
				at = t.getBoundingBox().getCenter();
			} else {
				Vec3 end = eye.add(c.look().scale(range));
				var hit = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
				at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().add(0, 1.3, 0);
			}
			if (at.distanceTo(eye) < 2.0) {
				return false;
			}
			final Vec3 centre = at;
			double radius = c.p().d("radius", 10);
			double core = c.p().d("core", 3.2);
			double pull = c.p().d("pull", 0.16);
			int ticks = c.p().i("ticks", 80);
			float dmg = (float) c.p().d("dmg", 14);
			int col = c.color(0xB04BFF);
			Fx.sound(l, centre, SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.4f, 0.5f);
			for (int k = 0; k < ticks; k++) {
				final int age = k;
				Tasks.later(k, () -> {
					if (age % 2 == 0) {
						for (int r = 0; r < 3; r++) {
							double f = 1.0 - ((age * 0.025 + r / 3.0) % 1.0);
							Fx.ring(l, centre, Math.max(0.8, radius * f), 14 + (int) radius, Fx.dust(col, 1.6f), 0.0);
						}
						Fx.burst(l, centre, Fx.dust(0x07000d, 2.0f), 3, core * 0.3, 0.0);
						Fx.burst(l, centre, ParticleTypes.REVERSE_PORTAL, 4, core * 0.8, 0.1);
					}
					for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius)) {
						Vec3 to = centre.subtract(e.getBoundingBox().getCenter());
						double d = to.length();
						if (d >= 0.3) {
							double resist = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
							double s = pull * (0.12 + (1.0 - d / radius)) * resist;
							e.setDeltaMovement(e.getDeltaMovement().add(to.scale(s / d)));
							e.hurtMarked = true;
							e.fallDistance = 0;
						}
						if (d <= core && age % 8 == 0) {
							Dmg.extra(l, p, e, dmg);
							Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.DAMAGE_INDICATOR, 3, 0.2, 0.1);
						}
					}
				});
			}
			Tasks.later(ticks, () -> {
				Fx.shockwave(l, centre, radius, 6, Fx.dust(col, 2.0f), 56);
				Fx.burst(l, centre, ParticleTypes.REVERSE_PORTAL, 80, core, 0.4);
				Fx.sound(l, centre, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.4f, 0.7f);
				for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius)) {
					double d = e.getBoundingBox().getCenter().distanceTo(centre);
					Dmg.extra(l, p, e, dmg * 2f);
					Dmg.shove(e, centre, 0.6 + 1.4 * (1.0 - d / radius), 0.6);
				}
			});
			return true;
		});

		// everything near you is hauled to your feet
		Abilities.active("gravity_pull", 220, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 12);
			float dmg = (float) c.p().d("dmg", 30);
			Vec3 flat = new Vec3(c.look().x, 0, c.look().z);
			Vec3 dest = p.position().add(flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize().scale(2.4));
			int hit = 0;
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), radius)) {
				Vec3 to = dest.subtract(e.position());
				double d = to.length();
				double resist = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
				if (d > 0.5) {
					Vec3 v = to.scale(1.0 / d).scale(Math.min(3.2, d * 0.45) * resist);          // ground friction eats ~55% a tick: this lands them at your feet
					e.setDeltaMovement(v.x, 0.25 * resist, v.z);
					e.hurtMarked = true;
				}
				Dmg.magic(l, p, e, dmg);
				Effects.give(e, "slowness", c.p().i("slow", 60), 2);
				hit++;
			}
			int col = c.color(0xB04BFF);
			for (int i = 0; i < 6; i++) {
				final double r = radius * (1.0 - i / 6.0);
				Tasks.later(i, () -> Fx.ring(l, p.position(), Math.max(0.6, r), 36, Fx.dust(col, 1.6f), 0.2));
			}
			Fx.burst(l, dest.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 40, 0.8, 0.2);
			Fx.sound(l, p.position(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.2f, 0.6f);
			Fx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 0.5f);
			return hit >= 0;
		});

		// each hit tears away a slice of the target's maximum health
		Abilities.onHit("rend", (c, t) -> {
			float extra = (float) Math.min(c.p().d("cap", 80), t.getMaxHealth() * c.p().d("pct", 0.03)) * com.ultimatesurvival.util.AttackStrength.of(c.player());
			if (extra > 0.5f && t.isAlive()) {
				Dmg.extra(c.level(), c.player(), t, extra);
				c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), Fx.n(14), 0.3, 0.4, 0.3, 0.2);
			}
		});

		Abilities.onKill("devour", (c, victim) -> {
			c.player().heal((float) c.p().d("amount", 6));
			Effects.give(c.player(), "absorption", 200, c.p().i("absorb", 1));
			c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, c.player().getX(), c.player().getY() + 1, c.player().getZ(), Fx.n(12), 0.4, 0.6, 0.4, 0.1);
		});

		// worn: a cold that slows and wears down everything near you
		Abilities.passive("entropy_aura").tick = (p, r) -> {
			int every = Math.max(10, r.i("every", 20));
			if (p.tickCount % every != 0 || !(p.level() instanceof ServerLevel l)) {
				return;
			}
			double rad = r.d("radius", 8);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.extra(l, p, e, (float) r.d("dmg", 10));
				Effects.give(e, "slowness", every + 10, 1);
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.REVERSE_PORTAL, 4, 0.3, 0.1);
			}
			Fx.ring(l, p.position(), rad, 30, Fx.dust(0xB04BFF, 1.3f), 0.15);
		};
	}
}
