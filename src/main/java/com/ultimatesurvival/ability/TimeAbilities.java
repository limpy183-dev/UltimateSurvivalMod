package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.PosHistory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 4 (Chronos) abilities: slowing, freezing and rewinding. */
final class TimeAbilities {
	private TimeAbilities() {
	}

	static void register() {
		Abilities.marker("quicken");                       // read by Wearer.cdMult through its cd_mult parameter
		Abilities.onHit("slow_hit", (c, t) -> {
			Effects.give(t, "slowness", c.p().i("ticks", 60), c.p().i("amp", 1));
			c.level().sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), Fx.n(6), 0.25, 0.3, 0.25, 0.1);
		});

		// everything near you stops
		Abilities.active("time_freeze", 500, c -> {
			ServerLevel l = c.level();
			int ticks = c.p().i("ticks", 80);
			Vec3 at = c.player().position();
			for (LivingEntity e : Dmg.enemiesAround(l, c.player(), at, c.p().d("radius", 12))) {
				Effects.give(e, "slowness", ticks, 6);
				Effects.give(e, "weakness", ticks, 2);
				Fx.burst(l, e.getBoundingBox().getCenter(), ParticleTypes.SOUL_FIRE_FLAME, 8, 0.3, 0.02);
			}
			Fx.shockwave(l, at, c.p().d("radius", 12), 8, ParticleTypes.ENCHANT, 48);
			Fx.shockwave(l, at, c.p().d("radius", 12), 8, Fx.dust(c.color(0x3EC6B0), 1.5f), 40);
			Fx.sound(l, at, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.4f, 0.6f);
			return true;
		});

		// step back to where you were a few seconds ago
		Abilities.active("rewind", 360, c -> {
			var p = c.player();
			Vec3 old = PosHistory.get(p, c.p().i("back", 60));
			if (old == null || old.distanceToSqr(p.position()) < 9 || !c.level().noCollision(p, p.getBoundingBox().move(old.subtract(p.position())))) {
				return false;
			}
			Fx.burst(c.level(), p.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 36, 0.4, 0.3);
			Fx.line(c.level(), p.position().add(0, 1, 0), old.add(0, 1, 0), ParticleTypes.ENCHANT, 0.4);
			p.teleportTo(old.x, old.y, old.z);
			p.fallDistance = 0;
			p.heal((float) c.p().d("heal", 6));
			Fx.burst(c.level(), old.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 36, 0.4, 0.3);
			Fx.sound(c.level(), old, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);
			Fx.sound(c.level(), old, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
			return true;
		});

		// worn: getting hit sometimes slows the whole room
		Abilities.passive("time_dilation").incoming = (p, src, d, r) -> {
			if (src.getEntity() != null && p.getRandom().nextDouble() < r.d("chance", 0.2) && p.level() instanceof ServerLevel l) {
				for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), r.d("radius", 6))) {
					Effects.give(e, "slowness", r.i("ticks", 60), 3);
				}
				Fx.shockwave(l, p.position(), r.d("radius", 6), 5, ParticleTypes.ENCHANT, 30);
				Fx.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
			}
			return d;
		};
	}
}
