package com.ultimatesurvival.ability;

import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Frozen;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import com.ultimatesurvival.util.Tasks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 6 (Eternus) abilities: a storm of slashes in every direction, and a death that does not take. */
public final class EternalAbilities {
	private static final Map<UUID, Long> WIND_READY = new HashMap<>();

	private EternalAbilities() {
	}

	static void register() {
		// a flurry of full-circle slashes, each in its own colour of the rainbow
		Abilities.active("omega_slash", 300, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 16);
			int count = Math.max(2, c.p().i("count", 6));
			float dmg = (float) c.p().d("dmg", 500);
			Vec3 at = p.position();
			Fx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6f, 0.5f);
			for (int i = 0; i < count; i++) {
				final int n = i;
				final boolean last = i == count - 1;
				Tasks.later(i * 3, () -> {
					int col = Fx.rainbow(n / (double) count);
					Vec3 mid = p.position().add(0, 1.0, 0);
					var rnd = l.getRandom();
					double tilt = (rnd.nextDouble() - 0.5) * 1.2;
					for (int k = 0; k < Fx.n(64); k++) {                                                      // a tilted ring of light, drawn in one go
						double a = Math.PI * 2 * k / Fx.n(64);
						Vec3 q = mid.add(Math.cos(a) * radius, Math.sin(a * 2 + n) * tilt * 2.0, Math.sin(a) * radius);
						l.sendParticles(Fx.dust(col, 2.0f), q.x, q.y, q.z, 1, 0, 0, 0, 0);
						if (k % 4 == 0) {
							Fx.line(l, mid, q, ParticleTypes.END_ROD, 3.0);
						}
					}
					Fx.sound(l, mid, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f + n * 0.12f);
					for (LivingEntity e : Dmg.enemiesAround(l, p, mid, radius)) {
						Dmg.extra(l, p, e, last ? dmg * 2f : dmg);
						Dmg.shove(e, mid, last ? 1.6 : 0.4, 0.3);
						Fx.burst(l, e.getBoundingBox().getCenter(), Fx.dust(col, 1.6f), 10, 0.4, 0.2);
					}
					if (last) {
						Fx.shockwave(l, p.position(), radius, 10, Fx.dust(col, 2.6f), 100);
						Fx.burst(l, mid, ParticleTypes.FIREWORK, 80, 3.0, 0.5);
						Shake.at(l, p.position(), radius * 3, 0.7f, 16);
						ScreenFlash.at(l, p.position(), 20, 0xFFFFFF, 0.25f, 8);
					}
				});
			}
			return true;
		});

		// worn: a killing blow is refused: you stand back up whole, everything near you stops, and it will not happen again for a while
		Abilities.passive("second_wind");
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l)) {
				return true;
			}
			AbilityRef r = com.ultimatesurvival.armor.Wearer.active(p).get("second_wind");
			if (r == null || WIND_READY.getOrDefault(p.getUUID(), 0L) > l.getGameTime()) {
				return true;
			}
			WIND_READY.put(p.getUUID(), l.getGameTime() + r.i("cd", 4800));
			p.setHealth((float) (p.getMaxHealth() * r.d("pct", 1.0)));
			p.clearFire();
			Effects.give(p, "resistance", 160, 4);
			Effects.give(p, "absorption", 400, 4);
			Effects.give(p, "regeneration", 100, 3);
			Vec3 at = p.position();
			double rad = r.d("radius", 14);
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, rad)) {
				Frozen.set(e, r.i("ticks", 80));
				Effects.give(e, "slowness", r.i("ticks", 80), 6);
				Dmg.shove(e, at, 1.2, 0.5);
			}
			Fx.shockwave(l, at, rad, 10, Fx.dust(0xFFFFFF, 2.4f), 90);
			for (int i = 0; i < 7; i++) {
				Fx.ring(l, at, 1.4 + i * 0.5, 30, Fx.dust(Fx.rainbow(i / 7.0), 1.8f), 0.2 + i * 0.35);
			}
			Fx.burst(l, at.add(0, 1, 0), ParticleTypes.TOTEM_OF_UNDYING, 100, 1.2, 0.7);
			Fx.sound(l, at, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5f, 0.6f);
			Fx.sound(l, at, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.4f);
			Shake.at(l, at, 40, 0.6f, 14);
			ScreenFlash.to(p, 0xFFFFFF, 0.3f, 12);
			return false;
		});
	}

	static void forget(UUID id) {
		WIND_READY.remove(id);
	}

	/** True when the player's Second Wind is ready (tests / HUD). */
	public static boolean windReady(ServerPlayer p) {
		return WIND_READY.getOrDefault(p.getUUID(), 0L) <= p.level().getGameTime();
	}
}
