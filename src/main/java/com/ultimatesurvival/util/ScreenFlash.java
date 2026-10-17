package com.ultimatesurvival.util;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.net.Payloads;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A full-screen colour flash for the biggest moments (a boss awakening or dying, a supernova, a blinding flash, a ritual's lightning). Kept gentle on purpose:
 * never brighter than {@link #MAX_ALPHA}, never more often than once a second per player, scaled by config {@code screenFlashStrength}, and off with
 * {@code reducedEffects}.
 */
public final class ScreenFlash {
	public static final float MAX_ALPHA = 0.8f;
	private static final int COOLDOWN = 20;
	private static final Map<UUID, Long> LAST = new HashMap<>();

	private ScreenFlash() {
	}

	/** Flash every player within {@code range} blocks of {@code at}; the effect is a little weaker at the edge. */
	public static void at(ServerLevel level, Vec3 at, double range, int rgb, float strength, int ticks) {
		for (ServerPlayer p : level.players()) {
			double d = p.position().distanceTo(at);
			if (d <= range) {
				to(p, rgb, (float) (strength * (1.0 - 0.5 * d / range)), ticks);
			}
		}
	}

	public static void to(ServerPlayer p, int rgb, float strength, int ticks) {
		UsmConfig c = UsmConfig.get();
		float s = Math.min(MAX_ALPHA, strength * (float) c.screenFlashStrength);
		if (c.reducedEffects || s < 0.04f || p.isSpectator()) {
			return;
		}
		long now = p.level().getGameTime();
		Long last = LAST.get(p.getUUID());
		if (last != null && now - last < COOLDOWN) {
			return;                                                       // never strobes
		}
		LAST.put(p.getUUID(), now);
		ServerPlayNetworking.send(p, new Payloads.ScreenFlash(rgb & 0xFFFFFF, Math.max(4, ticks), s));
	}

	public static void clear() {
		LAST.clear();
	}
}
