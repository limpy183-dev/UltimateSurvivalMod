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

/** Screen shake for players near an impact: a payload the client turns into a camera wobble (config: screenShakeStrength, off with reducedEffects). */
public final class Shake {
	private record Active(float strength, long until) {
	}

	private static final Map<UUID, Active> SENT = new HashMap<>();

	private Shake() {
	}

	/** Shake every player within {@code range} blocks of {@code at}; strength falls off with distance. */
	public static void at(ServerLevel level, Vec3 at, double range, float strength, int ticks) {
		UsmConfig c = UsmConfig.get();
		if (c.reducedEffects || c.screenShakeStrength <= 0 || strength <= 0) {
			return;
		}
		long now = level.getGameTime();
		for (ServerPlayer p : level.players()) {
			double d = p.position().distanceTo(at);
			if (d > range) {
				continue;
			}
			float s = (float) (strength * (1.0 - d / range) * c.screenShakeStrength);
			Active cur = SENT.get(p.getUUID());
			if (s < 0.03f || (cur != null && cur.until > now && cur.strength >= s)) {
				continue;                                                     // something at least as strong is already rattling this player
			}
			SENT.put(p.getUUID(), new Active(s, now + ticks));
			ServerPlayNetworking.send(p, new Payloads.Shake(s, ticks));
		}
	}

	public static void clear() {
		SENT.clear();
	}
}
