package com.ultimatesurvival.mob;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.RitualAltarBlock;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.mob.attack.Styles;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import com.ultimatesurvival.util.Tasks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Summoning ritual: a sigil used on a Ritual Altar. The boss's ritual rule (dimension / time / sky / cost) is checked
 * first; if it holds, the sigil is spent and a ~3.5 second sequence of rising runes, thunder and tremors ends with the boss
 * climbing out of the altar. Breaking the altar mid-ritual cancels it and returns the sigil.
 */
public final class Ritual {
	public static final int LENGTH = 70;
	private static final Set<Long> BUSY = new HashSet<>();

	private Ritual() {
	}

	/** @return null when the ritual may start, otherwise the reason it cannot. */
	public static Component check(ServerLevel level, BlockPos altar, ServerPlayer player, MobDef boss) {
		JsonObject r = boss.ritual;
		if (level.getDifficulty() == Difficulty.PEACEFUL && !UsmConfig.get().bossesOnPeaceful) {
			return fail("peaceful");
		}
		if (BUSY.contains(altar.asLong())) {
			return fail("busy");
		}
		EntityType<? extends UsmMob> type = Mobs.TYPES.get(boss.id);
		if (!level.getEntities(type, e -> e.isAlive()).isEmpty()) {
			return Component.translatable("ultimatesurvival.ritual.fail.exists", Component.translatable("entity.ultimatesurvival." + boss.id)).withStyle(ChatFormatting.RED);
		}
		if (r == null) {
			return null;
		}
		if (!UsmConfig.get().ritualsAnywhere) {
			String dim = str(r, "dim", "");
			if (!dim.isEmpty() && !level.dimension().identifier().toString().equals(dim)) {
				return Component.translatable("ultimatesurvival.ritual.fail.dim", Component.translatable("ultimatesurvival.dim." + dim.substring(dim.indexOf(':') + 1))).withStyle(ChatFormatting.RED);
			}
			long dayTime = level.getOverworldClockTime() % 24000;
			switch (str(r, "time", "any")) {
				case "night" -> {
					if (dayTime < 13000 || dayTime > 23000) {
						return fail("night");
					}
				}
				case "day" -> {
					if (dayTime > 12000) {
						return fail("day");
					}
				}
				case "noon" -> {
					if (dayTime < 4500 || dayTime > 8500) {
						return fail("noon");
					}
				}
				default -> {
				}
			}
			if (r.has("sky") && r.get("sky").getAsBoolean() && !level.canSeeSky(altar.above())) {
				return fail("sky");
			}
		}
		if (!UsmConfig.get().freeRituals) {
			int xp = r.has("xp") ? r.get("xp").getAsInt() : 0;
			if (xp > 0 && !player.getAbilities().instabuild && player.experienceLevel < xp) {
				return Component.translatable("ultimatesurvival.ritual.fail.xp", xp).withStyle(ChatFormatting.RED);
			}
			double hearts = r.has("hearts") ? r.get("hearts").getAsDouble() : 0;
			if (hearts > 0 && !player.getAbilities().instabuild && player.getHealth() <= hearts * 2 + 1) {
				return Component.translatable("ultimatesurvival.ritual.fail.hearts", (int) hearts).withStyle(ChatFormatting.RED);
			}
			if (r.has("items") && !player.getAbilities().instabuild) {
				for (JsonElement e : r.getAsJsonArray("items")) {
					JsonObject it = e.getAsJsonObject();
					if (count(player, Mobs.stack(it.get("item").getAsString(), 1)) < it.get("count").getAsInt()) {
						return Component.translatable("ultimatesurvival.ritual.fail.items",
								it.get("count").getAsInt(), Mobs.stack(it.get("item").getAsString(), 1).getHoverName()).withStyle(ChatFormatting.RED);
					}
				}
			}
		}
		return null;
	}

	private static Component fail(String key) {
		return Component.translatable("ultimatesurvival.ritual.fail." + key).withStyle(ChatFormatting.RED);
	}

	private static String str(JsonObject o, String k, String d) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
	}

	private static int count(ServerPlayer p, ItemStack like) {
		int n = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (ItemStack.isSameItem(s, like)) {
				n += s.getCount();
			}
		}
		return n;
	}

	private static void take(ServerPlayer p, ItemStack like, int amount) {
		for (int i = 0; i < p.getInventory().getContainerSize() && amount > 0; i++) {
			ItemStack s = p.getInventory().getItem(i);
			if (ItemStack.isSameItem(s, like)) {
				int t = Math.min(amount, s.getCount());
				s.shrink(t);
				amount -= t;
			}
		}
	}

	/** Pays the cost, spends the sigil and runs the sequence. Caller has already verified {@link #check}. */
	public static void begin(ServerLevel level, BlockPos altar, ServerPlayer player, ItemStack sigil, MobDef boss) {
		JsonObject r = boss.ritual;
		boolean creative = player.getAbilities().instabuild;
		ItemStack refund = creative ? ItemStack.EMPTY : sigil.copyWithCount(1);
		if (!creative) {
			sigil.shrink(1);
			if (r != null && !UsmConfig.get().freeRituals) {
				int xp = r.has("xp") ? r.get("xp").getAsInt() : 0;
				if (xp > 0) {
					player.giveExperienceLevels(-xp);
				}
				double hearts = r.has("hearts") ? r.get("hearts").getAsDouble() : 0;
				if (hearts > 0) {
					player.hurtServer(level, level.damageSources().magic(), (float) (hearts * 2));
				}
				if (r.has("items")) {
					for (JsonElement e : r.getAsJsonArray("items")) {
						JsonObject it = e.getAsJsonObject();
						take(player, Mobs.stack(it.get("item").getAsString(), 1), it.get("count").getAsInt());
					}
				}
			}
		}
		long key = altar.asLong();
		BUSY.add(key);
		String style = boss.style;
		Vec3 c = Vec3.atBottomCenterOf(altar);
		Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.6f, 0.6f);
		for (int t = 0; t <= LENGTH; t++) {
			final int tick = t;
			Tasks.later(t, () -> step(level, altar, c, style, tick, boss, refund, player.getUUID(), key));
		}
	}

	private static void step(ServerLevel level, BlockPos altar, Vec3 c, String style, int t, MobDef boss, ItemStack refund, java.util.UUID who, long key) {
		if (!BUSY.contains(key)) {
			return;
		}
		if (!(level.getBlockState(altar).getBlock() instanceof RitualAltarBlock)) {                // altar broken: abort and give the sigil back
			BUSY.remove(key);
			Fx.burst(level, c.add(0, 1, 0), ParticleTypes.SMOKE, 30, 0.6, 0.05);
			Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.4f, 0.8f);
			if (!refund.isEmpty()) {
				net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(level, c.x, c.y + 0.5, c.z, refund);
				level.addFreshEntity(ie);
			}
			return;
		}
		double f = t / (double) LENGTH;
		if (t % 2 == 0) {
			Styles.rim(level, c, 6.5 - 5.0 * f, style, 26);
			Styles.beam(level, c.add(0, 0.8, 0), 1.0 + 9.0 * f, style, 8 + (int) (12 * f));
			Fx.burst(level, c.add(0, 1.4 + 3 * f, 0), ParticleTypes.ENCHANT, 12, 1.4, 0.4);
		}
		if (t == LENGTH * 3 / 10 || t == LENGTH * 6 / 10) {
			Fx.sound(level, c, SoundEvents.WARDEN_AMBIENT, SoundSource.HOSTILE, 1.6f, 0.5f);
			Shake.at(level, c, 40, 0.3f + (float) f * 0.3f, 14);
		}
		if (t == LENGTH * 85 / 100) {
			Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 0.7f);
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(c.x, c.y, c.z);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
			ScreenFlash.at(level, c, 40, 0xE8F0FF, 0.5f, 12);
		}
		if (t == LENGTH) {
			BUSY.remove(key);
			spawn(level, altar, boss);
		}
	}

	/** Spawns the boss on top of the altar and starts its emergence animation. */
	public static UsmBoss spawn(ServerLevel level, BlockPos altar, MobDef boss) {
		EntityType<? extends UsmMob> type = Mobs.TYPES.get(boss.id);
		UsmMob m = type.create(level, EntitySpawnReason.TRIGGERED);
		if (!(m instanceof UsmBoss b)) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 0.85, 0);
		b.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0f);
		level.addFreshEntity(b);
		b.beginSpawn();
		Shake.at(level, at, 60, 1.0f, 24);
		UltimateSurvival.LOGGER.info("Ritual complete: {} awakens at {}", boss.id, altar);
		return b;
	}

	public static boolean busy(BlockPos altar) {
		return BUSY.contains(altar.asLong());
	}

	public static void clear() {
		BUSY.clear();
	}

	/** Rows of the ritual requirement as plain text for tooltips / the Codex. */
	public static JsonArray itemsOf(MobDef boss) {
		return boss.ritual != null && boss.ritual.has("items") ? boss.ritual.getAsJsonArray("items") : new JsonArray();
	}
}
