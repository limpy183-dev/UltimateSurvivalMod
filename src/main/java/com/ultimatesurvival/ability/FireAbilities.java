package com.ultimatesurvival.ability;

import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
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

/** Tier 3 (Ignarok) abilities: fire in every shape - novas, ignition, auras and a second life. */
public final class FireAbilities {
	/** Game time at which each player's phoenix charge is ready again. */
	private static final Map<UUID, Long> PHOENIX_READY = new HashMap<>();

	private FireAbilities() {
	}

	static void register() {
		Abilities.active("fire_nova", 140, c -> {
			double r = c.p().d("radius", 6);
			float dmg = (float) c.p().d("dmg", 14);
			int ignite = c.p().i("ignite", 6);
			Vec3 at = c.player().position();
			ServerLevel l = c.level();
			for (LivingEntity e : Dmg.enemiesAround(l, c.player(), at, r)) {
				Dmg.magic(l, c.player(), e, dmg);
				e.igniteForSeconds(ignite);
				Dmg.shove(e, at, 0.7, 0.35);
			}
			Fx.shockwave(l, at, r, 8, ParticleTypes.FLAME, 48);
			Fx.shockwave(l, at, r * 0.8, 6, Fx.dust(0xFF7A1F, 1.6f), 36);
			Fx.burst(l, at.add(0, 0.4, 0), ParticleTypes.LAVA, 18, r * 0.25, 0.4);
			Fx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
			Fx.sound(l, at, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
			return true;
		});

		Abilities.onHit("ignite", (c, t) -> {
			t.igniteForSeconds(c.p().i("ignite", 5));
			float bonus = (float) c.p().d("dmg", 3);
			if (bonus > 0 && t.isAlive()) {
				Dmg.magic(c.level(), c.player(), t, bonus * com.ultimatesurvival.util.AttackStrength.of(c.player()));
			}
			c.level().sendParticles(ParticleTypes.FLAME, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), Fx.n(8), 0.25, 0.4, 0.25, 0.04);
		});

		Abilities.modify("burning_bonus", (c, t, d) -> {
			if (t.isOnFire()) {
				c.level().sendParticles(ParticleTypes.LAVA, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), Fx.n(4), 0.2, 0.3, 0.2, 0.0);
				return d * (float) c.p().d("mult", 1.5);
			}
			return d;
		});

		// worn: enemies close to you burn
		Abilities.passive("flame_aura").tick = (p, r) -> {
			if (p.tickCount % 20 != 0 || !(p.level() instanceof ServerLevel l)) {
				return;
			}
			double rad = r.d("radius", 4);
			float dmg = (float) r.d("dmg", 3);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.magic(l, p, e, dmg);
				e.igniteForSeconds(3);
			}
			Fx.ring(l, p.position(), rad, 28, ParticleTypes.FLAME, 0.15);
			Fx.burst(l, p.position().add(0, 1, 0), ParticleTypes.SMALL_FLAME, 4, 0.5, 0.02);
		};

		// worn: the first killing blow of every few minutes is refused
		Abilities.passive("phoenix");
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l)) {
				return true;
			}
			AbilityRef r = Wearer.active(p).get("phoenix");
			if (r == null || PHOENIX_READY.getOrDefault(p.getUUID(), 0L) > l.getGameTime()) {
				return true;
			}
			PHOENIX_READY.put(p.getUUID(), l.getGameTime() + r.i("cd", 6000));
			p.setHealth((float) (p.getMaxHealth() * r.d("pct", 0.5)));
			p.clearFire();
			Effects.give(p, "resistance", 100, 2);
			Effects.give(p, "fire_resistance", 200, 0);
			Fx.burst(l, p.position().add(0, 1, 0), ParticleTypes.FLAME, 80, 1.0, 0.25);
			Fx.burst(l, p.position().add(0, 1, 0), ParticleTypes.TOTEM_OF_UNDYING, 40, 0.6, 0.5);
			Fx.sound(l, p.position(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 0.7f);
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), 6)) {
				Dmg.magic(l, p, e, 20);
				e.igniteForSeconds(6);
				Dmg.shove(e, p.position(), 1.2, 0.5);
			}
			return false;
		});
	}

	static void forget(UUID id) {
		PHOENIX_READY.remove(id);
	}

	/** True when the player's Phoenix charge is available (tests / HUD). */
	public static boolean phoenixCharged(ServerPlayer p) {
		return PHOENIX_READY.getOrDefault(p.getUUID(), 0L) <= p.level().getGameTime();
	}
}
