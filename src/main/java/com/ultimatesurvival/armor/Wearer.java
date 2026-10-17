package com.ultimatesurvival.armor;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.item.Accessories;
import com.ultimatesurvival.util.Fx;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * Everything a player gets just by wearing / carrying things: armor-set bonuses and accessory
 * bonuses are both collected here into one {id -> ability ref} map per player, so a hook (tick,
 * incoming damage) is written once and works for both sources. Equip / remove edges fire once.
 */
public final class Wearer {
	private static final Map<UUID, Map<String, AbilityRef>> ACTIVE = new HashMap<>();
	private static final Map<UUID, Set<String>> FULL_SETS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_HURT = new HashMap<>();

	private Wearer() {
	}

	public static Map<String, AbilityRef> active(ServerPlayer p) {
		return ACTIVE.getOrDefault(p.getUUID(), Map.of());
	}

	public static boolean has(ServerPlayer p, String abilityId) {
		return active(p).containsKey(abilityId);
	}

	public static void forget(UUID id) {
		ACTIVE.remove(id);
		FULL_SETS.remove(id);
		LAST_HURT.remove(id);
	}

	public static void clearAll() {
		ACTIVE.clear();
		FULL_SETS.clear();
		LAST_HURT.clear();
	}

	public static void markHurt(ServerPlayer p) {
		LAST_HURT.put(p.getUUID(), p.tickCount);
	}

	/** Ticks since the player last took damage (large when never hurt). */
	public static int sinceHurt(ServerPlayer p) {
		Integer t = LAST_HURT.get(p.getUUID());
		return t == null ? Integer.MAX_VALUE : p.tickCount - t;
	}

	/** Server tick for one player. */
	public static void tick(ServerPlayer p) {
		Map<String, AbilityRef> now = new LinkedHashMap<>();
		Set<String> fullBefore = FULL_SETS.getOrDefault(p.getUUID(), Set.of());
		Set<String> fullNow = new HashSet<>();
		for (ArmorSets.SetDef set : ArmorSets.all()) {
			int worn = ArmorSets.worn(p, set);
			if (worn == 0) {
				continue;
			}
			if (worn >= set.pieces().size()) {
				fullNow.add(set.id());
				if (!fullBefore.contains(set.id())) {
					equipFlourish(p, set.name(), set.color());
				}
				AmbientFx.tick(p, set.ambient(), set.color());
			}
			for (ArmorSets.Bonus b : set.bonuses()) {
				if (worn >= b.need()) {
					now.put(b.ref().id(), b.ref());
				}
			}
		}
		Accessories.collect(p, now);

		Map<String, AbilityRef> before = ACTIVE.getOrDefault(p.getUUID(), Map.of());
		for (Map.Entry<String, AbilityRef> e : now.entrySet()) {
			Ability a = Abilities.get(e.getKey());
			if (a == null) {
				continue;
			}
			if (!before.containsKey(e.getKey()) && a.onEquip != null) {
				a.onEquip.run(p, e.getValue());
			}
			if (a.tick != null) {
				a.tick.run(p, e.getValue());
			}
		}
		for (Map.Entry<String, AbilityRef> e : before.entrySet()) {
			if (!now.containsKey(e.getKey())) {
				Ability a = Abilities.get(e.getKey());
				if (a != null && a.onRemove != null) {
					a.onRemove.run(p, e.getValue());
				}
			}
		}
		if (fullNow.isEmpty()) {
			FULL_SETS.remove(p.getUUID());
		} else {
			FULL_SETS.put(p.getUUID(), fullNow);
		}
		if (now.isEmpty()) {
			ACTIVE.remove(p.getUUID());
		} else {
			ACTIVE.put(p.getUUID(), now);
		}
		// flight is only ever granted by the "flight" bonus: a player who logs in without it (the abilities are saved with the player) loses it at once
		if (!now.containsKey("flight") && p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) {
			p.getAbilities().mayfly = false;
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
		}
	}

	/** Rescales damage the player is about to take. */
	public static float incoming(ServerPlayer p, DamageSource src, float dmg) {
		for (AbilityRef r : active(p).values()) {
			Ability a = Abilities.get(r);
			if (a != null && a.incoming != null) {
				dmg = a.incoming.apply(p, src, dmg, r);
			}
		}
		return dmg;
	}

	/** Rescales / reacts to damage the player is about to deal. */
	public static float outgoing(ServerPlayer p, net.minecraft.world.entity.LivingEntity victim, DamageSource src, float dmg) {
		for (AbilityRef r : active(p).values()) {
			Ability a = Abilities.get(r);
			if (a != null && a.outgoing != null) {
				dmg = a.outgoing.apply(p, victim, src, dmg, r);
			}
		}
		return dmg;
	}

	/** Product of every "cd_mult" parameter among worn bonuses (0.75 = abilities recharge 25% faster). */
	public static double cdMult(ServerPlayer p) {
		if (com.ultimatesurvival.config.UsmConfig.get().noAbilityCooldowns) {
			return 0.0;                                                                                  // the weapon clamps the result to one tick
		}
		double m = 1.0;
		for (AbilityRef r : active(p).values()) {
			m *= r.d("cd_mult", 1.0);
		}
		return Math.max(0.2, m);
	}

	/** Visual + audio "your set is active" moment. */
	public static void equipFlourish(ServerPlayer p, String title, int color) {
		if (!(p.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 at = p.position();
		Fx.ring(level, at, 0.9, 20, Fx.dust(color, 1.3f), 0.1);
		Fx.ring(level, at, 0.9, 20, Fx.dust(color, 1.3f), 1.0);
		Fx.ring(level, at, 0.9, 20, Fx.dust(color, 1.3f), 1.9);
		for (int i = 0; i < Fx.n(14); i++) {                                                         // a helix around the wearer - never a particle through the camera
			double ang = i * 0.9;
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x + Math.cos(ang) * 0.9, at.y + i * 0.16, at.z + Math.sin(ang) * 0.9, 1, 0.0, 0.03, 0.0, 0.0);
		}
		Fx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.6f);
		Fx.sound(level, at, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.8f, 1.2f);
		p.sendSystemMessage(Component.translatable("ultimatesurvival.msg.set_bonus", title).withStyle(ChatFormatting.GOLD), true);
	}
}
