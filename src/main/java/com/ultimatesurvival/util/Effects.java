package com.ultimatesurvival.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Status-effect shortcuts that take vanilla effect ids as plain strings (so content tables can use them). */
public final class Effects {
	private Effects() {
	}

	public static Holder<MobEffect> holder(String id) {
		Identifier key = id.contains(":") ? Identifier.parse(id) : Identifier.withDefaultNamespace(id);
		return BuiltInRegistries.MOB_EFFECT.get(key).orElseThrow(() -> new IllegalArgumentException("unknown effect " + id));
	}

	/** Applies an effect, keeping whichever of (existing, new) lasts longer so refreshes never shorten it. */
	public static void give(LivingEntity e, String id, int ticks, int amp) {
		give(e, id, ticks, amp, false);
	}

	public static void give(LivingEntity e, String id, int ticks, int amp, boolean hidden) {
		e.addEffect(new MobEffectInstance(holder(id), ticks, amp, hidden, !hidden, !hidden));
	}
}
