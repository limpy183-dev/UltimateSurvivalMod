package com.ultimatesurvival.mob;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.mob.attack.Attacks;
import com.ultimatesurvival.progress.Advancements;
import com.ultimatesurvival.sound.UsmSounds;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Boss Rush: all seven Sovereigns, one after another, around the player who started it - no altars, no rituals. Between fights there is a
 * ten second breather and a partial heal. Falling, leaving the arena or changing dimension ends the run. The kills count as normal
 * progression, and finishing pays out and earns an advancement.
 */
public final class BossRush {
	public static final List<String> ORDER = List.of("ignarok", "voltaris", "hemorath", "chronos", "nihilus", "solareon", "omnarch", "boreus", "morbane", "astraeus", "eternus");
	private static final int FIRST_WAIT = 100;
	private static final int BREATHER = 200;

	private static final class Run {
		final UUID player;
		final Vec3 arena;
		final ResourceKey<Level> dim;
		final long started;
		int index = -1;
		UsmBoss boss;
		int wait = FIRST_WAIT;

		Run(ServerPlayer p, ServerLevel level) {
			this.player = p.getUUID();
			this.arena = p.position();
			this.dim = level.dimension();
			this.started = level.getGameTime();
		}
	}

	private static final Map<UUID, Run> RUNS = new HashMap<>();

	private BossRush() {
	}

	public static boolean active(ServerPlayer p) {
		return RUNS.containsKey(p.getUUID());
	}

	/** How many Sovereigns of the active run have been summoned so far (0 before the first), or -1 without a run. */
	public static int stage(ServerPlayer p) {
		Run r = RUNS.get(p.getUUID());
		return r == null ? -1 : r.index + 1;
	}

	public static UsmBoss current(ServerPlayer p) {
		Run r = RUNS.get(p.getUUID());
		return r == null ? null : r.boss;
	}

	/** Test hook: skip the countdown. */
	public static void hurry(ServerPlayer p) {
		Run r = RUNS.get(p.getUUID());
		if (r != null && r.boss == null) {
			r.wait = 1;
		}
	}

	public static boolean start(ServerPlayer p) {
		if (RUNS.containsKey(p.getUUID()) || !(p.level() instanceof ServerLevel sl)) {
			return false;
		}
		if (sl.getDifficulty() == Difficulty.PEACEFUL && !UsmConfig.get().bossesOnPeaceful) {
			p.sendSystemMessage(Component.translatable("ultimatesurvival.rush.peaceful").withStyle(ChatFormatting.RED));
			return false;
		}
		RUNS.put(p.getUUID(), new Run(p, sl));
		title(p, Component.translatable("ultimatesurvival.rush.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable("ultimatesurvival.rush.sub").withStyle(ChatFormatting.GRAY));
		Fx.sound(sl, p.position(), UsmSounds.get("rush.start", SoundEvents.ENDER_DRAGON_GROWL), SoundSource.MASTER, 1.5f, 1.0f);
		return true;
	}

	public static void stop(MinecraftServer server, UUID id, String reasonKey) {
		Run r = RUNS.remove(id);
		if (r == null) {
			return;
		}
		if (r.boss != null && r.boss.isAlive()) {
			r.boss.discard();
		}
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null && reasonKey != null) {
			p.sendSystemMessage(Component.translatable(reasonKey).withStyle(ChatFormatting.GRAY));
		}
	}

	public static void clear() {
		RUNS.clear();
	}

	public static void tick(MinecraftServer server) {
		if (RUNS.isEmpty()) {
			return;
		}
		for (Run r : new ArrayList<>(RUNS.values())) {
			step(server, r);
		}
	}

	private static void step(MinecraftServer server, Run r) {
		ServerPlayer p = server.getPlayerList().getPlayer(r.player);
		if (p == null || !p.isAlive() || !p.level().dimension().equals(r.dim) || p.position().distanceToSqr(r.arena) > 140 * 140) {
			stop(server, r.player, "ultimatesurvival.rush.failed");
			return;
		}
		ServerLevel sl = (ServerLevel) p.level();
		if (r.boss == null) {
			if (--r.wait > 0) {
				if (r.wait % 20 == 0 && r.wait <= 100) {
					p.sendOverlayMessage(Component.translatable("ultimatesurvival.rush.next", nextName(r), r.wait / 20).withStyle(ChatFormatting.GOLD));
				}
				return;
			}
			r.index++;
			if (r.index >= ORDER.size()) {
				win(server, sl, p, r);
				return;
			}
			summon(sl, p, r);
			return;
		}
		if (r.boss.isDeadOrDying() || r.boss.getRemovalReason() == Entity.RemovalReason.KILLED) {
			r.boss = null;
			r.wait = r.index + 1 >= ORDER.size() ? 60 : BREATHER;
			p.heal(p.getMaxHealth() * 0.6f);
			p.sendSystemMessage(Component.translatable("ultimatesurvival.rush.down", r.index + 1, ORDER.size()).withStyle(ChatFormatting.GREEN));
		} else if (r.boss.isRemoved()) {
			stop(server, r.player, "ultimatesurvival.rush.failed");
		}
	}

	private static Component nextName(Run r) {
		int next = r.index + 1;
		return next >= ORDER.size() ? Component.translatable("ultimatesurvival.rush.finish") : Component.translatable("entity.ultimatesurvival." + ORDER.get(next));
	}

	private static void summon(ServerLevel sl, ServerPlayer p, Run r) {
		String id = ORDER.get(r.index);
		@SuppressWarnings("unchecked")
		EntityType<? extends UsmBoss> type = (EntityType<? extends UsmBoss>) Mobs.TYPES.get(id);
		UsmBoss b = type.create(sl, EntitySpawnReason.TRIGGERED);
		if (b == null) {
			stop(sl.getServer(), r.player, "ultimatesurvival.rush.failed");
			return;
		}
		double yaw = Math.toRadians(p.getYRot());
		Vec3 spot = Attacks.ground(sl, p.position().add(-Math.sin(yaw) * 26, 0, Math.cos(yaw) * 26));
		b.snapTo(spot.x, spot.y + 0.3, spot.z, p.getYRot() + 180f, 0f);
		sl.addFreshEntity(b);
		b.beginSpawn();
		b.setTarget(p);
		r.boss = b;
		title(p, Component.translatable("entity.ultimatesurvival." + id + ".short").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.translatable("entity.ultimatesurvival." + id + ".epithet").append(" - ")
						.append(Component.translatable("ultimatesurvival.rush.count", r.index + 1, ORDER.size())).withStyle(ChatFormatting.GRAY));
	}

	private static void win(MinecraftServer server, ServerLevel sl, ServerPlayer p, Run r) {
		RUNS.remove(r.player);
		long ticks = sl.getGameTime() - r.started;
		String time = String.format(java.util.Locale.ROOT, "%d:%02d", ticks / 1200, (ticks / 20) % 60);
		title(p, Component.translatable("ultimatesurvival.rush.won").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Component.literal(time).withStyle(ChatFormatting.GOLD));
		Fx.sound(sl, p.position(), UsmSounds.get("rush.won", SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), SoundSource.MASTER, 1.5f, 1.0f);
		ExperienceOrb.award(sl, p.position(), 6000);
		ScreenFlash.to(p, 0xFFD45E, 0.5f, 30);
		for (String reward : new String[] {"omni_core", "weave_core", "titan_heart", "minecraft:nether_star", "minecraft:enchanted_golden_apple"}) {
			ItemStack st = Mobs.stack(reward, reward.endsWith("_core") ? 2 : reward.contains("apple") ? 3 : 1);
			if (!st.isEmpty() && !p.getInventory().add(st)) {
				p.drop(st, false);
			}
		}
		Advancements.award(p, "boss_rush");
		p.sendSystemMessage(Component.translatable("ultimatesurvival.rush.time", time).withStyle(ChatFormatting.GOLD));
	}

	private static void title(ServerPlayer p, Component title, Component sub) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
		p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
	}
}
