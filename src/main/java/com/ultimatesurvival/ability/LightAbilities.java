package com.ultimatesurvival.ability;

import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.LightZones;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Tier 5 (Solareon) abilities: piercing light, flares, the supernova, smiting, and a halo no darkness can eat. */
final class LightAbilities {
	private LightAbilities() {
	}

	/** Undead, and creatures of the void and of blood, burn twice as hard in the light. */
	static boolean smitten(LivingEntity e) {
		return e.is(EntityTypeTags.UNDEAD) || e instanceof UsmMob m && (m.def().style.equals("void") || m.def().style.equals("blood"));
	}

	static void register() {
		// a piercing line of light; terrain stops it
		Abilities.active("sunbeam", 140, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double range = c.p().d("range", 40);
			double width = c.p().d("width", 1.2);
			float dmg = (float) c.p().d("dmg", 60);
			Vec3 eye = c.eye();
			Vec3 from = eye.add(c.look().scale(0.6)).add(0, -0.25, 0);
			Vec3 to = eye.add(c.look().scale(range));
			var hit = l.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
			AABB box = new AABB(from, end).inflate(width + 1);
			for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, e -> Dmg.isEnemy(p, e))) {
				if (Aim.distToSegment(e.getBoundingBox().getCenter(), from, end) <= width + e.getBbWidth() * 0.5) {
					Dmg.extra(l, p, e, dmg * (smitten(e) ? 2f : 1f));
					Effects.give(e, "glowing", 100, 0);
					Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 12, 0.3, 0.2);
				}
			}
			Fx.line(l, from, end, Fx.dust(c.color(0xFFF3B0), 1.6f), 0.5);
			Fx.line(l, from, end, ParticleTypes.END_ROD, 1.4);
			Fx.burst(l, from, ParticleTypes.END_ROD, 10, 0.2, 0.1);
			Fx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 2.0f);
			Fx.sound(l, end, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.8f);
			return true;
		});

		Abilities.active("solar_flare", 240, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 12);
			float dmg = (float) c.p().d("dmg", 40);
			Vec3 at = p.position();
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, radius)) {
				Dmg.extra(l, p, e, dmg * (smitten(e) ? 2f : 1f));
				Effects.give(e, "blindness", c.p().i("blind", 80), 0);
				Effects.give(e, "glowing", 160, 0);
				Dmg.shove(e, at, 0.5, 0.3);
			}
			Fx.shockwave(l, at, radius, 8, Fx.dust(c.color(0xFFF3B0), 2.0f), 60);
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.END_ROD, 80, 1.5, 0.5);
			Fx.sound(l, at, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4f, 1.6f);
			Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
			return true;
		});

		// gather light for a moment, then detonate; you are all but untouchable while it builds
		Abilities.active("supernova", 600, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			int charge = c.p().i("charge", 24);
			double radius = c.p().d("radius", 16);
			float dmg = (float) c.p().d("dmg", 220);
			int col = c.color(0xFFF3B0);
			Effects.give(p, "resistance", charge + 20, 4);
			Fx.sound(l, p.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.6f, 0.5f);
			for (int step = 0; step < charge; step++) {
				final int i = step;
				final double f = (i + 1.0) / charge;
				Tasks.later(i, () -> {
					if (!p.isAlive()) {
						return;
					}
					Vec3 mid = p.position().add(0, 1, 0);
					for (int k = 0; k < 12; k++) {
						Vec3 d = new Vec3(l.getRandom().nextGaussian(), l.getRandom().nextGaussian(), l.getRandom().nextGaussian()).normalize().scale(radius * 0.5 * (1.0 - f) + 0.6);
						l.sendParticles(k % 2 == 0 ? ParticleTypes.END_ROD : Fx.dust(col, 1.6f), mid.x + d.x, mid.y + d.y, mid.z + d.z, 1, 0, 0, 0, 0);
					}
					if (i % 6 == 0) {
						Fx.sound(l, mid, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, (float) (0.5 + f * 1.3));
					}
				});
			}
			Tasks.later(charge, () -> {
				if (!p.isAlive()) {
					return;
				}
				Vec3 at = p.position();
				Fx.burst(l, at.add(0, 1, 0), ParticleTypes.EXPLOSION_EMITTER, 1, 0, 0);
				Fx.burst(l, at.add(0, 1, 0), ParticleTypes.END_ROD, 160, 4.0, 0.9);
				Fx.shockwave(l, at, radius, 10, Fx.dust(col, 2.2f), 80);
				Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 0.6f);
				Fx.sound(l, at, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0f, 0.6f);
				ScreenFlash.to(p, col, 0.5f, 24);
				for (LivingEntity e : Dmg.enemiesAround(l, p, at, radius)) {
					double d = e.position().distanceTo(at);
					float f = (float) (1.0 - 0.5 * d / radius);
					Dmg.extra(l, p, e, dmg * f * (smitten(e) ? 1.5f : 1f));
					Dmg.shove(e, at, 1.8 * f + 0.3, 0.7);
					Effects.give(e, "blindness", 60, 0);
				}
			});
			return true;
		});

		Abilities.onHit("sear", (c, t) -> {
			float extra = (float) c.p().d("dmg", 20) * com.ultimatesurvival.util.AttackStrength.of(c.player()) * (smitten(t) ? 2f : 1f);
			if (extra > 0.5f && t.isAlive()) {
				Dmg.extra(c.level(), c.player(), t, extra);
			}
			Effects.give(t, "glowing", c.p().i("glow", 120), 0);
			c.level().sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), Fx.n(10), 0.3, 0.4, 0.3, 0.1);
		});

		Abilities.modify("smite", (c, t, d) -> {
			if (smitten(t)) {
				c.level().sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), Fx.n(8), 0.3, 0.3, 0.3, 0.1);
				return d * (float) c.p().d("mult", 1.5);
			}
			return d;
		});

		// worn: you shed light - it burns and reveals what is near, and counts as light against the Devourer's eclipse
		Abilities.passive("halo_aura").tick = (p, r) -> {
			if (!(p.level() instanceof ServerLevel l)) {
				return;
			}
			if (p.tickCount % 10 == 0) {
				LightZones.add(l, p.position(), 6, 24);
			}
			int every = Math.max(10, r.i("every", 20));
			if (p.tickCount % every != 0) {
				return;
			}
			double rad = r.d("radius", 8);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.extra(l, p, e, (float) r.d("dmg", 10) * (smitten(e) ? 2f : 1f));
				Effects.give(e, "glowing", every + 20, 0);
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 4, 0.3, 0.1);
			}
			Fx.ring(l, p.position(), rad, 30, Fx.dust(0xFFF3B0, 1.3f), 0.15);
			Fx.burst(l, p.position().add(0, 1.2, 0), ParticleTypes.END_ROD, 3, 0.6, 0.02);
		};

		// a flare: the light stays where you cracked it
		Abilities.active("light_flare", 0, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 14);
			int ticks = c.p().i("ticks", 600);
			Vec3 at = p.position();
			LightZones.add(l, at, radius, ticks);
			Effects.give(p, "night_vision", ticks, 0);
			for (int k = 0; k * 20 < ticks; k++) {
				Tasks.later(k * 20, () -> {
					Fx.ring(l, at, radius, 40, Fx.dust(0xFFF3B0, 1.2f), 0.15);
					Fx.burst(l, at.add(0, 2.5, 0), ParticleTypes.END_ROD, 4, 0.6, 0.02);
				});
			}
			Fx.shockwave(l, at, radius, 8, Fx.dust(0xFFF3B0, 1.8f), 50);
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.END_ROD, 60, 1.0, 0.4);
			Fx.sound(l, at, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
			return true;
		});
	}
}
