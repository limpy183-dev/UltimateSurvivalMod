package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.LightZones;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 5 (Omnarch) abilities: a cleave across the world, the nova that heals its caster, and an aura of both halves. */
final class ApexAbilities {
	private ApexAbilities() {
	}

	static void register() {
		// a great crescent slash: everything in the arc takes the hit and is thrown aside
		Abilities.active("cleave", 140, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 14);
			double half = Math.toRadians(c.p().d("arc", 150)) / 2;
			float dmg = (float) c.p().d("dmg", 300);
			Vec3 look = c.look();
			Vec3 flat = new Vec3(look.x, 0, look.z);
			if (flat.lengthSqr() < 1e-4) {
				return false;
			}
			flat = flat.normalize();
			Vec3 origin = p.position();
			for (LivingEntity e : Dmg.enemiesAround(l, p, origin.add(0, 1, 0), radius)) {
				Vec3 to = e.position().subtract(origin);
				Vec3 tf = new Vec3(to.x, 0, to.z);
				if (tf.lengthSqr() > 0.25 && Math.acos(Mth.clamp(flat.dot(tf.normalize()), -1, 1)) > half) {
					continue;
				}
				float f = (float) (1.0 - 0.3 * to.length() / radius);
				Dmg.extra(l, p, e, dmg * f);
				Dmg.shove(e, origin, 1.5, 0.5);
			}
			int col = c.color(0xFF6AD5);
			for (int i = 0; i <= 8; i++) {
				final double ang = -half + 2 * half * i / 8;
				final Vec3 dir = flat.yRot((float) -ang);
				Tasks.later(i / 2, () -> {
					Fx.line(l, origin.add(0, 1.1, 0), origin.add(0, 1.1, 0).add(dir.scale(radius)), Fx.dust(col, 1.7f), 0.6);
					Fx.line(l, origin.add(0, 1.1, 0), origin.add(0, 1.1, 0).add(dir.scale(radius)), ParticleTypes.END_ROD, 1.5);
				});
			}
			Fx.burst(l, origin.add(flat.scale(3)).add(0, 1, 0), ParticleTypes.SWEEP_ATTACK, 4, 2.0, 0);
			Fx.sound(l, origin, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.5f);
			Fx.sound(l, origin, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
			return true;
		});

		// both halves at once: damage out, health and absorption in
		Abilities.active("apex_nova", 360, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 18);
			float dmg = (float) c.p().d("dmg", 360);
			Vec3 at = p.position();
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, radius)) {
				double d = e.position().distanceTo(at);
				Dmg.extra(l, p, e, dmg * (float) (1.0 - 0.4 * d / radius));
				Dmg.shove(e, at, 1.6, 0.6);
				Effects.give(e, "slowness", 60, 2);
			}
			p.heal((float) c.p().d("heal", 40));
			Effects.give(p, "absorption", 400, 4);
			Fx.shockwave(l, at, radius, 10, Fx.dust(0xFF6AD5, 2.2f), 80);
			Fx.shockwave(l, at, radius * 0.7, 8, Fx.dust(0x7FE8FF, 2.0f), 60);
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.END_ROD, 120, 3.0, 0.7);
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 80, 3.0, 0.5);
			Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
			Fx.sound(l, at, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.6f, 0.8f);
			return true;
		});

		// worn: everything near you is worn away and you recover; you shed light too
		Abilities.passive("omni_aura").tick = (p, r) -> {
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
			double rad = r.d("radius", 10);
			int hit = 0;
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.extra(l, p, e, (float) r.d("dmg", 30));
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.ENCHANT, 5, 0.3, 0.2);
				hit++;
			}
			if (hit > 0) {
				p.heal((float) r.d("heal", 3));
				Fx.burst(l, p.position().add(0, 1.6, 0), ParticleTypes.HEART, 1, 0.3, 0);
			}
			Fx.ring(l, p.position(), rad, 32, Fx.dust(0xFF6AD5, 1.3f), 0.15);
			Fx.ring(l, p.position(), rad * 0.8, 24, Fx.dust(0x7FE8FF, 1.2f), 0.3);
		};
	}
}
