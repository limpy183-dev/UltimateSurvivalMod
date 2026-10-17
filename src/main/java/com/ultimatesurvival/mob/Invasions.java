package com.ultimatesurvival.mob;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Invasions: every few minutes a wave of creatures suited to the player's tier closes in from all sides. Kill them all for
 * experience and loot; if the player dies or flees the survivors melt away. Waves come from the content table ("invasions")
 * and are scaled up by the config's invaderScalingPerTier.
 */
public final class Invasions {
	private record Entry(String mob, int min, int max) {
	}

	private record Def(int tier, List<Entry> mobs, int xp, List<MobDef.Drop> reward, String title) {
	}

	private static final class Active {
		final UUID player;
		final List<UsmMob> mobs = new ArrayList<>();
		final Def def;
		int age;

		Active(UUID player, Def def) {
			this.player = player;
			this.def = def;
		}
	}

	private static final List<Def> DEFS = new ArrayList<>();
	private static final Map<UUID, Long> NEXT = new HashMap<>();
	private static final List<Active> ACTIVE = new ArrayList<>();
	private static final int GIVE_UP_TICKS = 20 * 60 * 5;
	public static final UUID MOD = UUID.nameUUIDFromBytes("ultimatesurvival:invader".getBytes());

	private Invasions() {
	}

	public static void load() {
		DEFS.clear();
		if (!Content.root.has("invasions")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("invasions")) {
			JsonObject o = e.getAsJsonObject();
			List<Entry> mobs = new ArrayList<>();
			for (JsonElement m : o.getAsJsonArray("mobs")) {
				JsonObject mo = m.getAsJsonObject();
				mobs.add(new Entry(mo.get("mob").getAsString(), mo.get("min").getAsInt(), mo.get("max").getAsInt()));
			}
			List<MobDef.Drop> reward = new ArrayList<>();
			for (JsonElement r : o.getAsJsonArray("reward")) {
				JsonObject ro = r.getAsJsonObject();
				reward.add(new MobDef.Drop(ro.get("item").getAsString(), ro.get("min").getAsInt(), ro.get("max").getAsInt(), ro.get("chance").getAsDouble()));
			}
			DEFS.add(new Def(o.get("tier").getAsInt(), mobs, o.get("xp").getAsInt(), reward, o.has("title") ? o.get("title").getAsString() : ""));
		}
	}

	public static void clear() {
		NEXT.clear();
		ACTIVE.clear();
	}

	public static int activeCount() {
		return ACTIVE.size();
	}

	/** The wave for the current tier (the highest defined wave not above it). */
	private static Def defFor(int tier) {
		Def best = null;
		for (Def d : DEFS) {
			if (d.tier() <= tier && (best == null || d.tier() > best.tier())) {
				best = d;
			}
		}
		return best;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0 || !UsmConfig.get().invasionsEnabled) {
			trackActive(server);
			return;
		}
		trackActive(server);
		long now = server.overworld().getGameTime();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator() || p.level().getDifficulty() == Difficulty.PEACEFUL) {
				continue;
			}
			Long due = NEXT.get(p.getUUID());
			if (due == null) {
				NEXT.put(p.getUUID(), now + interval(p));
				continue;
			}
			if (now >= due && ACTIVE.stream().noneMatch(a -> a.player.equals(p.getUUID()))) {
				NEXT.put(p.getUUID(), now + interval(p));
				Def d = defFor(Gates.currentTier());
				if (d != null && p.level() instanceof ServerLevel sl) {
					start(sl, p, d);
				}
			}
		}
	}

	private static long interval(ServerPlayer p) {
		int base = Math.max(30, UsmConfig.get().invasionIntervalSeconds) * 20;
		return (long) (base * (0.75 + p.getRandom().nextDouble() * 0.5));
	}

	/** Test / command hook: start the wave for the player's current tier right now. Returns the number of invaders spawned. */
	public static int startNow(ServerPlayer p) {
		Def d = defFor(Gates.currentTier());
		return d != null && p.level() instanceof ServerLevel sl ? start(sl, p, d) : 0;
	}

	private static int start(ServerLevel sl, ServerPlayer p, Def d) {
		Active a = new Active(p.getUUID(), d);
		double scale = 1.0 + UsmConfig.get().invaderScalingPerTier * (Gates.currentTier() - 1);
		for (Entry e : d.mobs()) {
			EntityType<? extends UsmMob> type = Mobs.TYPES.get(e.mob());
			if (type == null) {
				continue;
			}
			int n = e.min() + (e.max() > e.min() ? p.getRandom().nextInt(e.max() - e.min() + 1) : 0);
			for (int i = 0; i < n; i++) {
				Vec3 at = findSpot(sl, p, type);
				if (at == null) {
					continue;
				}
				UsmMob m = type.create(sl, EntitySpawnReason.EVENT);
				if (m == null) {
					continue;
				}
				m.snapTo(at.x, at.y, at.z, sl.getRandom().nextFloat() * 360f, 0f);
				scaleUp(m, scale);
				m.setPersistenceRequired();
				m.setTarget(p);
				sl.addFreshEntity(m);
				a.mobs.add(m);
			}
		}
		if (a.mobs.isEmpty()) {
			return 0;
		}
		ACTIVE.add(a);
		Component title = Component.translatable("ultimatesurvival.invasion.start").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
		p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("ultimatesurvival.invasion.sub", d.title(), a.mobs.size()).withStyle(ChatFormatting.GRAY)));
		sl.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3.0f, 0.9f);
		UltimateSurvival.LOGGER.info("Invasion '{}' begins around {}: {} invaders (x{} scaling)", d.title(), p.getName().getString(), a.mobs.size(), scale);
		return a.mobs.size();
	}

	static void scaleUp(UsmMob m, double scale) {
		if (scale <= 1.0) {
			return;
		}
		var hp = m.getAttribute(Attributes.MAX_HEALTH);
		var atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
		if (hp != null) {
			hp.addPermanentModifier(new AttributeModifier(UltimateSurvival.id("invader_health"), scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			m.setHealth(m.getMaxHealth());
		}
		if (atk != null) {
			atk.addPermanentModifier(new AttributeModifier(UltimateSurvival.id("invader_damage"), (scale - 1.0) * 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		m.damageScale = 1.0 + (scale - 1.0) * 0.6;
	}

	/** A spot 18-30 blocks from the player where the type could stand: a free column on solid ground (or open air for fliers). */
	private static Vec3 findSpot(ServerLevel sl, ServerPlayer p, EntityType<? extends UsmMob> type) {
		for (int tries = 0; tries < 40; tries++) {
			double a = sl.getRandom().nextDouble() * Math.PI * 2;
			double d = 18 + sl.getRandom().nextDouble() * 12;
			int x = (int) Math.floor(p.getX() + Math.cos(a) * d);
			int z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
			BlockPos column = new BlockPos(x, (int) p.getY(), z);
			if (!sl.hasChunkAt(column)) {
				continue;
			}
			BlockPos top = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (sl.dimensionType().hasCeiling()) {                                       // caves and the Nether: search at the player's own level
				top = column;
				for (int dy = 8; dy >= -8; dy--) {
					BlockPos c = new BlockPos(x, (int) p.getY() + dy, z);
					if (SpawnPlacements.isSpawnPositionOk(type, sl, c)) {
						top = c;
						break;
					}
				}
			}
			if (SpawnPlacements.isSpawnPositionOk(type, sl, top) || Mobs.defOf(type).fly && sl.getBlockState(top).isAir()) {
				return Vec3.atBottomCenterOf(top).add(0, Mobs.defOf(type).fly ? 2.0 : 0.0, 0);
			}
		}
		return null;
	}

	private static void trackActive(MinecraftServer server) {
		for (Iterator<Active> it = ACTIVE.iterator(); it.hasNext();) {
			Active a = it.next();
			a.age++;
			a.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
			ServerPlayer p = server.getPlayerList().getPlayer(a.player);
			if (p == null || !p.isAlive() || a.age > GIVE_UP_TICKS) {
				for (UsmMob m : a.mobs) {
					m.discard();
				}
				if (p != null) {
					p.sendSystemMessage(Component.translatable("ultimatesurvival.invasion.lost").withStyle(ChatFormatting.GRAY));
				}
				it.remove();
			} else if (a.mobs.isEmpty()) {
				win(p, a);
				it.remove();
			}
		}
	}

	private static void win(ServerPlayer p, Active a) {
		ServerLevel sl = (ServerLevel) p.level();
		p.sendSystemMessage(Component.translatable("ultimatesurvival.invasion.won").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		sl.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		ExperienceOrb.award(sl, p.position(), a.def.xp());
		for (MobDef.Drop d : a.def.reward()) {
			if (sl.getRandom().nextDouble() <= d.chance()) {
				int n = d.min() + (d.max() > d.min() ? sl.getRandom().nextInt(d.max() - d.min() + 1) : 0);
				ItemStack st = Mobs.stack(d.item(), n);
				if (!st.isEmpty()) {
					p.getInventory().placeItemBackInInventory(st);   // inventory first, any leftover dropped at the player's feet; syncs the slots to the client
				}
			}
		}
	}
}
