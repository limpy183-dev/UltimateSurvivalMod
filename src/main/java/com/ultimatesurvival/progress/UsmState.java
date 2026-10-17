package com.ultimatesurvival.progress;

import net.minecraft.server.MinecraftServer;

/**
 * Process-wide handle on the current world's {@link Progress}. Singleplayer only: the integrated
 * server and the client share one JVM, so the client reads progression straight from here
 * instead of needing a sync protocol. When no world is loaded this is an empty throwaway.
 */
public final class UsmState {
	private static volatile Progress progress = new Progress();
	private static volatile MinecraftServer server;

	private UsmState() {
	}

	public static Progress progress() {
		return progress;
	}

	/** The running (integrated) server, or null between worlds. */
	public static MinecraftServer server() {
		return server;
	}

	public static void attach(MinecraftServer s) {
		server = s;
		progress = s.overworld().getDataStorage().computeIfAbsent(Progress.TYPE);
	}

	public static void detach() {
		server = null;
		progress = new Progress();
	}
}
