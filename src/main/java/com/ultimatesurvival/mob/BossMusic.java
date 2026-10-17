package com.ultimatesurvival.mob;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.sound.UsmSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Each Sovereign fights to its own theme (a vanilla track re-pitched under a mod sound event). Players near a live boss hear it; it stops when the boss dies or they leave. */
public final class BossMusic {
	private static final long TRACK_TICKS = 20 * 200;
	private static final Map<UUID, Long> STARTED = new HashMap<>();
	private static final Map<UUID, String> TRACK = new HashMap<>();

	private BossMusic() {
	}

	/** About once a second for every live boss. */
	public static void tick(UsmBoss boss, ServerLevel level) {
		if (!UsmConfig.get().bossMusic) {
			return;
		}
		String id = "music.boss." + boss.def().style;
		Holder<SoundEvent> track = UsmSounds.holder(id);
		if (track == null) {
			return;
		}
		long now = level.getGameTime();
		for (ServerPlayer p : level.players()) {
			String cur = TRACK.get(p.getUUID());
			boolean near = !p.isSpectator() && p.distanceToSqr(boss) < 72 * 72;
			if (near && (!id.equals(cur) || now - STARTED.getOrDefault(p.getUUID(), 0L) > TRACK_TICKS)) {
				if (cur != null) {
					stopFor(p, cur);
				}
				p.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));                      // hush the usual background music
				p.connection.send(new ClientboundSoundEntityPacket(track, SoundSource.MUSIC, p, 0.85f, 1.0f, p.getRandom().nextLong()));
				STARTED.put(p.getUUID(), now);
				TRACK.put(p.getUUID(), id);
			} else if (!near && cur != null) {
				stopFor(p, cur);
			}
		}
	}

	/** The boss is gone: nobody keeps listening to its theme. */
	public static void end(UsmBoss boss, ServerLevel level) {
		String id = "music.boss." + boss.def().style;
		for (ServerPlayer p : level.players()) {
			if (id.equals(TRACK.get(p.getUUID()))) {
				stopFor(p, id);
			}
		}
	}

	private static void stopFor(ServerPlayer p, String id) {
		p.connection.send(new ClientboundStopSoundPacket(UltimateSurvival.id(id), SoundSource.MUSIC));
		TRACK.remove(p.getUUID());
		STARTED.remove(p.getUUID());
	}

	/** The boss track a player is hearing right now, or null (tests). */
	public static String trackOf(ServerPlayer p) {
		return TRACK.get(p.getUUID());
	}

	public static void clear() {
		TRACK.clear();
		STARTED.clear();
	}
}
