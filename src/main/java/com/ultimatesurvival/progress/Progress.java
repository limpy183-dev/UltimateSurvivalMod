package com.ultimatesurvival.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ultimatesurvival.UltimateSurvival;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Everything the mod remembers about a world: which gates (bosses) are cleared, which Codex
 * entries were discovered, who already received the Codex, and a few counters. Saved with the
 * world (data/ultimatesurvival/progress.dat). Sets are concurrent because the client's HUD and
 * tooltips read this while the integrated server mutates it (singleplayer only by design).
 */
public class Progress extends SavedData {
	public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().optionalFieldOf("defeated", List.of()).forGetter(p -> new ArrayList<>(p.defeated)),
			Codec.STRING.listOf().optionalFieldOf("discovered", List.of()).forGetter(p -> new ArrayList<>(p.discovered)),
			UUIDUtil.CODEC.listOf().optionalFieldOf("codex_given", List.of()).forGetter(p -> new ArrayList<>(p.codexGiven)),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(p -> Map.copyOf(p.counters)),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.OPTIONAL_CODEC.listOf()).optionalFieldOf("accessories", Map.of()).forGetter(p -> Map.copyOf(p.accessories))
	).apply(i, Progress::new));

	public static final SavedDataType<Progress> TYPE = new SavedDataType<>(
			UltimateSurvival.id("progress"), Progress::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Set<String> defeated = ConcurrentHashMap.newKeySet();
	private final Set<String> discovered = ConcurrentHashMap.newKeySet();
	private final Set<UUID> codexGiven = ConcurrentHashMap.newKeySet();
	private final Map<String, Integer> counters = new ConcurrentHashMap<>();
	private final Map<UUID, List<ItemStack>> accessories = new ConcurrentHashMap<>();
	public static final int ACCESSORY_SLOTS = 6;

	public Progress() {
	}

	private Progress(List<String> defeated, List<String> discovered, List<UUID> codexGiven, Map<String, Integer> counters,
			Map<UUID, List<ItemStack>> accessories) {
		this.defeated.addAll(defeated);
		this.discovered.addAll(discovered);
		this.codexGiven.addAll(codexGiven);
		this.counters.putAll(counters);
		accessories.forEach((k, v) -> this.accessories.put(k, new ArrayList<>(v)));
	}

	// ---- accessory slots (kept on death; saved with the world) ----------------------------------
	/** How many accessory slots are open right now (the config / Creative cheat; never below {@link #ACCESSORY_SLOTS}). */
	public static int slots() {
		return Math.max(ACCESSORY_SLOTS, Math.min(com.ultimatesurvival.config.UsmConfig.MAX_ACCESSORY_SLOTS, com.ultimatesurvival.config.UsmConfig.get().accessorySlots));
	}

	/** The live list of this player's accessory stacks (at least {@link #slots()} long; only the first {@link #slots()} are active). */
	public List<ItemStack> accessories(UUID player) {
		List<ItemStack> list = accessories.computeIfAbsent(player, k -> new ArrayList<>(Collections.nCopies(ACCESSORY_SLOTS, ItemStack.EMPTY)));
		while (list.size() < slots()) {                                      // the slot cheat grew; shrinking keeps the items (they just stop counting)
			list.add(ItemStack.EMPTY);
		}
		return list;
	}

	// ---- gates --------------------------------------------------------------------------------
	public boolean isDefeated(String gateId) {
		return defeated.contains(gateId);
	}

	public boolean markDefeated(String gateId) {
		boolean changed = defeated.add(gateId);
		if (changed) {
			setDirty();
		}
		return changed;
	}

	public boolean unmarkDefeated(String gateId) {
		boolean changed = defeated.remove(gateId);
		if (changed) {
			setDirty();
		}
		return changed;
	}

	public void clearGates() {
		if (!defeated.isEmpty()) {
			defeated.clear();
			setDirty();
		}
	}

	// ---- codex discovery -----------------------------------------------------------------------
	public boolean isDiscovered(String key) {
		return discovered.contains(key);
	}

	/** @return true if this key was newly discovered */
	public boolean discover(String key) {
		boolean changed = discovered.add(key);
		if (changed) {
			setDirty();
		}
		return changed;
	}

	public Set<String> discoveredKeys() {
		return discovered;
	}

	public void clearDiscoveries() {
		if (!discovered.isEmpty()) {
			discovered.clear();
			setDirty();
		}
	}

	// ---- codex handout --------------------------------------------------------------------------
	public boolean hasReceivedCodex(UUID player) {
		return codexGiven.contains(player);
	}

	public void markCodexGiven(UUID player) {
		if (codexGiven.add(player)) {
			setDirty();
		}
	}

	public void clearCodexGiven() {
		if (!codexGiven.isEmpty()) {
			codexGiven.clear();
			setDirty();
		}
	}

	// ---- counters (boss kill counts, permanent upgrades ...) ---------------------------------
	public int counter(String key) {
		return counters.getOrDefault(key, 0);
	}

	public int addCounter(String key, int delta) {
		int v = counters.merge(key, delta, Integer::sum);
		setDirty();
		return v;
	}

	public void clearCounters() {
		if (!counters.isEmpty()) {
			counters.clear();
			setDirty();
		}
	}
}
