package com.ultimatesurvival.progress;

import com.ultimatesurvival.UltimateSurvival;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Grants the mod's advancements (data/ultimatesurvival/advancement/*.json; the "code" ones use the impossible trigger and are awarded from here). */
public final class Advancements {
	private Advancements() {
	}

	public static void award(ServerPlayer p, String id) {
		MinecraftServer server = p.level().getServer();
		if (server == null) {
			return;
		}
		var holder = server.getAdvancements().get(UltimateSurvival.id(id));
		if (holder != null) {
			p.getAdvancements().award(holder, "done");
		}
	}

	public static void awardAll(MinecraftServer server, String id) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			award(p, id);
		}
	}

	public static boolean has(ServerPlayer p, String id) {
		MinecraftServer server = p.level().getServer();
		var holder = server == null ? null : server.getAdvancements().get(UltimateSurvival.id(id));
		return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
	}
}
