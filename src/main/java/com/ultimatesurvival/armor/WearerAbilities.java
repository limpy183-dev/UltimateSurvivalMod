package com.ultimatesurvival.armor;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bonuses granted by wearing armor sets / carrying accessories. Tiers differ by parameters in the content tables. */
public final class WearerAbilities {
	private static final Map<UUID, Integer> JUMPS_USED = new HashMap<>();
	private static boolean reflecting;

	private WearerAbilities() {
	}

	public static void register() {
		// regenerate when nobody has hurt you for a while
		Ability regen = Abilities.passive("regen_ooc");
		regen.tick = (p, r) -> {
			if (p.tickCount % 20 == 0 && Wearer.sinceHurt(p) > r.i("delay", 100) && p.getHealth() < p.getMaxHealth()) {
				p.heal((float) r.d("amount", 1));
				if (p.level() instanceof ServerLevel l) {
					l.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.3, 0.4, 0.3, 0);
				}
			}
		};

		Abilities.passive("feather_fall").incoming = (p, src, d, r) -> src.is(DamageTypeTags.IS_FALL) ? 0 : d;

		// extra mid-air jumps (client sends a DoubleJump payload on a mid-air jump press)
		Ability dj = Abilities.passive("double_jump");
		dj.tick = (p, r) -> {
			if (p.onGround() || p.isInWater()) {
				JUMPS_USED.remove(p.getUUID());
			}
		};
		dj.onRemove = (p, r) -> JUMPS_USED.remove(p.getUUID());

		// hold sneak in the air to slow your fall
		Abilities.passive("glide_sneak").tick = (p, r) -> {
			Vec3 dm = p.getDeltaMovement();
			if (!p.onGround() && p.isShiftKeyDown() && dm.y < -0.12 && !p.getAbilities().flying) {
				p.setDeltaMovement(dm.x, -0.12, dm.z);
				p.hurtMarked = true;
				p.fallDistance = 0;
			}
		};

		Abilities.passive("damage_reduction").incoming = (p, src, d, r) -> d * (float) (1.0 - r.d("pct", 0.1));

		Abilities.passive("dodge").incoming = (p, src, d, r) -> {
			if (src.getEntity() != null && p.getRandom().nextDouble() < r.d("chance", 0.1)) {
				if (p.level() instanceof ServerLevel l) {
					l.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1.0, p.getZ(), Fx.n(8), 0.3, 0.5, 0.3, 0.05);
					Fx.sound(l, p.position(), SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
				}
				return 0;
			}
			return d;
		};

		Abilities.passive("thorns").incoming = (p, src, d, r) -> {
			if (!reflecting && src.getEntity() instanceof LivingEntity att && att != p && p.level() instanceof ServerLevel l) {
				reflecting = true;
				try {
					att.hurtServer(l, l.damageSources().thorns(p), d * (float) r.d("pct", 0.2));
					if (r.d("ignite", 0) > 0) {
						att.igniteForSeconds((float) r.d("ignite", 0));
					}
					if (r.d("shock", 0) > 0) {
						Effects.give(att, "slowness", (int) (r.d("shock", 0) * 20), 3);
						Fx.sparks(l, att.position().add(0, att.getBbHeight() * 0.5, 0), 14);
					}
				} finally {
					reflecting = false;
				}
			}
			return d;
		};

		// pulls dropped items toward you
		Abilities.passive("magnet").tick = (p, r) -> {
			if (p.tickCount % 2 != 0) {
				return;
			}
			double rad = r.d("radius", 6);
			Vec3 target = p.position().add(0, 0.6, 0);
			for (ItemEntity it : p.level().getEntitiesOfClass(ItemEntity.class, new AABB(target, target).inflate(rad))) {
				Vec3 d = target.subtract(it.position());
				double len = d.length();
				if (len > 0.8) {
					it.setDeltaMovement(it.getDeltaMovement().scale(0.6).add(d.scale(0.22 / len)));
				}
			}
		};

		// effects refreshed every second so they never flicker
		Abilities.passive("fire_res").tick = refresh("fire_resistance", 0);
		Abilities.passive("night_vision").tick = refresh("night_vision", 0);
		Abilities.passive("water_breathing").tick = refresh("water_breathing", 0);
		Abilities.passive("haste_aura").tick = refresh("haste", 0);
	}

	private static Ability.PlayerTick refresh(String effect, int amp) {
		return (p, r) -> {
			if (p.tickCount % 10 == 0) {                                                       // twice a second: a freshly worn set takes hold at once
				Effects.give(p, effect, 260, r.i("amp", amp), true);
			}
		};
	}

	/** Server side of the double-jump key. */
	public static void doubleJump(ServerPlayer p) {
		AbilityRef r = Wearer.active(p).get("double_jump");
		if (r == null || p.onGround() || p.isInWater() || p.isInLava() || p.getAbilities().flying || p.isFallFlying() || p.onClimbable()) {
			return;
		}
		int used = JUMPS_USED.getOrDefault(p.getUUID(), 0);
		if (used >= r.i("jumps", 1)) {
			return;
		}
		JUMPS_USED.put(p.getUUID(), used + 1);
		Vec3 dm = p.getDeltaMovement();
		p.setDeltaMovement(dm.x, r.d("power", 0.62), dm.z);
		p.hurtMarked = true;
		p.fallDistance = 0;
		if (p.level() instanceof ServerLevel l) {
			Vec3 at = p.position();
			Fx.ring(l, at, 0.6, 14, ParticleTypes.CLOUD, 0.1);
			Fx.burst(l, at, ParticleTypes.POOF, 8, 0.25, 0.04);
			Fx.sound(l, at, SoundEvents.ENDER_DRAGON_FLAP, 0.45f, 1.6f);
		}
	}

	public static void forget(UUID id) {
		JUMPS_USED.remove(id);
	}
}
