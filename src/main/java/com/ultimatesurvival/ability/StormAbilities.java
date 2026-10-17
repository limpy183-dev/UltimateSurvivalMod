package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Tasks;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Tier 4 (Voltaris) abilities: stuns, called lightning and a worn storm. */
final class StormAbilities {
	private StormAbilities() {
	}

	static void register() {
		Abilities.onHit("shock", (c, t) -> {
			if (c.player().getRandom().nextDouble() > c.p().d("chance", 0.3)) {
				return;
			}
			ServerLevel l = c.level();
			Effects.give(t, "slowness", c.p().i("stun", 20), 4);
			Fx.sparks(l, t.position().add(0, t.getBbHeight() * 0.5, 0), 18);
			LivingEntity other = Aim.nearest(l, c.player(), t.position(), 5.5, Set.of(t));
			if (other != null) {
				Fx.line(l, t.getBoundingBox().getCenter(), other.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.35);
				Dmg.magic(l, c.player(), other, (float) c.p().d("dmg", 8));
				Fx.sparks(l, other.position().add(0, other.getBbHeight() * 0.5, 0), 10);
			}
			Fx.sound(l, t.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.8f);
		});

		// the staff: lightning falls where you look
		Abilities.active("call_lightning", 180, c -> {
			ServerLevel l = c.level();
			double range = c.p().d("range", 32);
			Vec3 eye = c.eye();
			LivingEntity t = Aim.target(l, c.player(), range, 0.9);
			Vec3 pos;
			if (t != null) {
				pos = t.position();
			} else {
				Vec3 end = eye.add(c.look().scale(range));
				var hit = l.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player()));
				pos = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
			}
			if (pos.distanceTo(eye) < 2.0) {
				return false;
			}
			final Vec3 at = pos;
			int strikes = c.p().i("strikes", 3);
			double radius = c.p().d("radius", 3.5);
			float dmg = (float) c.p().d("dmg", 14);
			Fx.sound(l, c.player().position(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
			for (int k = 0; k < strikes; k++) {
				Tasks.later(k * 6, () -> {
					Fx.bolt(l, at);
					Fx.burst(l, at.add(0, 0.5, 0), ParticleTypes.ELECTRIC_SPARK, 30, radius * 0.4, 0.4);
					Fx.shockwave(l, at, radius, 3, Fx.dust(c.color(0xFFE94D), 1.4f), 24);
					for (LivingEntity e : Dmg.enemiesAround(l, c.player(), at, radius)) {
						Dmg.magic(l, c.player(), e, dmg);
						Effects.give(e, "slowness", 30, 3);
					}
				});
			}
			return true;
		});

		// worn: every few seconds lightning picks the nearest enemies
		Abilities.passive("storm_aura").tick = (p, r) -> {
			if (p.tickCount % Math.max(10, r.i("every", 60)) != 0 || !(p.level() instanceof ServerLevel l)) {
				return;
			}
			List<LivingEntity> near = Dmg.enemiesAround(l, p, p.position(), r.d("radius", 8)).stream()
					.sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).limit(r.i("targets", 3)).toList();
			for (LivingEntity e : near) {
				Fx.bolt(l, e.position());
				Dmg.magic(l, p, e, (float) r.d("dmg", 12));
				Effects.give(e, "slowness", 20, 2);
			}
		};
	}
}
