package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 4 (Hemorath) abilities: everything that turns damage into life. */
final class BloodAbilities {
	private BloodAbilities() {
	}

	static void register() {
		// worn: a share of all damage you deal comes back as health
		Abilities.passive("bloodlust").outgoing = (p, victim, src, dmg, r) -> {
			float heal = (float) Math.min(dmg * r.d("pct", 0.08), r.d("cap", 6));
			if (heal > 0.05f && p.getHealth() < p.getMaxHealth()) {
				p.heal(heal);
				if (p.level() instanceof ServerLevel l && p.tickCount % 3 == 0) {
					l.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.7, p.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
				}
			}
			return dmg;
		};

		// worn: the weaker you are, the harder you are to kill
		Abilities.passive("crimson_veil").incoming = (p, src, d, r) ->
				p.getHealth() / p.getMaxHealth() < r.d("thr", 0.5) ? d * (float) (1.0 - r.d("pct", 0.25)) : d;

		// the scythe: a crimson arc that heals you for part of what it deals
		Abilities.active("blood_reap", 200, c -> {
			ServerLevel l = c.level();
			double rad = c.p().d("radius", 5);
			float dmg = (float) c.p().d("dmg", 18);
			Vec3 at = c.player().position();
			float total = 0;
			for (LivingEntity e : Dmg.enemiesAround(l, c.player(), at, rad)) {
				Dmg.magic(l, c.player(), e, dmg);
				Dmg.shove(e, at, 0.5, 0.25);
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.DAMAGE_INDICATOR, 4, 0.3, 0.1);
				total += dmg;
			}
			if (total > 0) {
				c.player().heal((float) (total * c.p().d("heal_pct", 0.4)));
				Fx.burst(l, at.add(0, 1.6, 0), ParticleTypes.HEART, 6, 0.5, 0.0);
			}
			Fx.shockwave(l, at, rad, 5, Fx.dust(0xD0102A, 1.6f), 40);
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.SWEEP_ATTACK, 5, rad * 0.4, 0.0);
			Fx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
			Fx.sound(l, at, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.2f);
			return true;
		});
	}
}
