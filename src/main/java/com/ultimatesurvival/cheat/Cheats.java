package com.ultimatesurvival.cheat;

import com.ultimatesurvival.codex.Discovery;
import com.ultimatesurvival.command.UsmCommands;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.mob.Invasions;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.Ritual;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.UsmState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * The cheats on the Creative page of the Codex. Toggles and numbers live in the config (they are saved to
 * config/ultimatesurvival.json and apply to every world, like any other config value); actions run once.
 * The client only sends {@code (id, value)}: a toggle is 0/1, a number is an index into its allowed values,
 * an action is the index of its choice. The server checks that the sender is in creative mode.
 */
public final class Cheats {
	public enum Kind {
		TOGGLE, NUMBER, ACTION
	}

	public static final int GROUP_PROGRESS = 0;
	public static final int GROUP_BOSSES = 1;
	public static final int GROUP_GEAR = 2;

	/** What an action does; returns the line shown above the hotbar. */
	@FunctionalInterface
	interface Run {
		String run(ServerPlayer p, int choice);
	}

	/** {@code values}: the allowed numbers of a NUMBER; {@code choices}: the options of an ACTION (empty = just a button). */
	public record Cheat(String id, Kind kind, int group, double[] values, DoubleSupplier get, DoubleConsumer set, List<String> choices, Run run) {
		/** Index of the allowed value closest to the current one. */
		public int valueIndex() {
			double now = get.getAsDouble();
			int best = 0;
			for (int i = 1; i < values.length; i++) {
				if (Math.abs(values[i] - now) < Math.abs(values[best] - now)) {
					best = i;
				}
			}
			return best;
		}
	}

	public static final List<Cheat> ALL = new ArrayList<>();
	private static final double[] ON_OFF = {0, 1};

	static {
		List<String> tiers = UsmCommands.targets();
		// ---- progression -------------------------------------------------------------------------
		action("unlock", GROUP_PROGRESS, tiers, (p, i) -> {
			Gates.unlockUpTo(p.level().getServer(), UsmCommands.resolve(tiers.get(i)));
			return "Unlocked: " + label("unlock", i);
		});
		action("relock", GROUP_PROGRESS, tiers, (p, i) -> {
			Gates.lockFrom(p.level().getServer(), UsmCommands.lockOrder(tiers.get(i)));
			return "Re-locked from: " + label("relock", i);
		});
		action("discover", GROUP_PROGRESS, List.of(), (p, i) -> {
			int n = 0;
			for (String k : Discovery.allKeys()) {
				n += UsmState.progress().discover(k) ? 1 : 0;
			}
			return "Codex: " + n + " entries revealed";
		});
		action("forget", GROUP_PROGRESS, List.of(), (p, i) -> {
			UsmState.progress().clearDiscoveries();
			return "Codex discoveries forgotten";
		});
		// ---- bosses & world ----------------------------------------------------------------------
		List<String> bosses = new ArrayList<>();
		for (Gate g : Content.GATES) {
			if (g.boss() != null && Mobs.TYPES.containsKey(g.id())) {
				bosses.add(g.id());
			}
		}
		action("summon", GROUP_BOSSES, bosses, (p, i) -> {
			MobDef def = Mobs.DEFS.get(bosses.get(i));
			if (def == null || !(p.level() instanceof ServerLevel sl)) {
				return "Cannot summon that here";
			}
			BlockPos ahead = p.blockPosition().relative(p.getDirection(), 8).below();
			return Ritual.spawn(sl, ahead, def) != null ? def.name + " awakens" : "Cannot summon that here";
		});
		action("summon_all", GROUP_BOSSES, List.of(), (p, i) -> {
			if (!(p.level() instanceof ServerLevel sl)) {
				return "Cannot summon that here";
			}
			int n = 0;
			for (int k = 0; k < bosses.size(); k++) {
				double a = Math.PI * 2 * k / bosses.size();
				BlockPos at = p.blockPosition().offset((int) Math.round(Math.cos(a) * 14), -1, (int) Math.round(Math.sin(a) * 14));
				n += Ritual.spawn(sl, at, Mobs.DEFS.get(bosses.get(k))) != null ? 1 : 0;
			}
			return n + " Sovereigns awaken around you";
		});
		action("dismiss", GROUP_BOSSES, List.of(), (p, i) -> {
			int n = 0;
			for (ServerLevel sl : p.level().getServer().getAllLevels()) {
				for (Entity e : sl.getAllEntities()) {
					if (e instanceof UsmBoss b) {
						b.discard();
						n++;
					}
				}
			}
			return n + " Sovereign" + (n == 1 ? "" : "s") + " dismissed";
		});
		action("invade", GROUP_BOSSES, List.of(), (p, i) -> Invasions.startNow(p) + " invaders sent");
		toggle("free_rituals", GROUP_BOSSES, () -> UsmConfig.get().freeRituals, v -> UsmConfig.get().freeRituals = v);
		toggle("rituals_anywhere", GROUP_BOSSES, () -> UsmConfig.get().ritualsAnywhere, v -> UsmConfig.get().ritualsAnywhere = v);
		toggle("peaceful_bosses", GROUP_BOSSES, () -> UsmConfig.get().bossesOnPeaceful, v -> UsmConfig.get().bossesOnPeaceful = v);
		toggle("invasions", GROUP_BOSSES, () -> UsmConfig.get().invasionsEnabled, v -> UsmConfig.get().invasionsEnabled = v);
		number("boss_hp", GROUP_BOSSES, new double[] {0.1, 0.25, 0.5, 1, 2, 5, 10}, () -> UsmConfig.get().bossHealthMultiplier, v -> UsmConfig.get().bossHealthMultiplier = v);
		number("boss_dmg", GROUP_BOSSES, new double[] {0, 0.1, 0.25, 0.5, 1, 2, 5}, () -> UsmConfig.get().bossDamageMultiplier, v -> UsmConfig.get().bossDamageMultiplier = v);
		number("boss_leash", GROUP_BOSSES, new double[] {24, 48, 96, 192, 512, 1024}, () -> UsmConfig.get().bossLeashRange, v -> UsmConfig.get().bossLeashRange = v);
		number("invasion_gap", GROUP_BOSSES, new double[] {30, 60, 120, 300, 600, 1200, 3600}, () -> UsmConfig.get().invasionIntervalSeconds, v -> UsmConfig.get().invasionIntervalSeconds = (int) v);
		number("mob_scaling", GROUP_BOSSES, new double[] {0, 0.1, 0.2, 0.5, 1}, () -> UsmConfig.get().vanillaMobScalingPerTier, v -> UsmConfig.get().vanillaMobScalingPerTier = v);
		// ---- gear & tools ------------------------------------------------------------------------
		double[] slots = new double[UsmConfig.MAX_ACCESSORY_SLOTS - 5];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = 6 + i;
		}
		number("acc_slots", GROUP_GEAR, slots, () -> UsmConfig.get().accessorySlots, v -> UsmConfig.get().accessorySlots = (int) v);
		toggle("no_cooldowns", GROUP_GEAR, () -> UsmConfig.get().noAbilityCooldowns, v -> UsmConfig.get().noAbilityCooldowns = v);
		number("ability_dmg", GROUP_GEAR, new double[] {0, 0.5, 1, 2, 5, 10, 100}, () -> UsmConfig.get().abilityDamageMultiplier, v -> UsmConfig.get().abilityDamageMultiplier = v);
		number("weapon_dmg", GROUP_GEAR, new double[] {0.5, 1, 2, 5, 10, 100}, () -> UsmConfig.get().weaponDamageMultiplier, v -> UsmConfig.get().weaponDamageMultiplier = v);
		number("armor_mult", GROUP_GEAR, new double[] {0.5, 1, 2, 5, 10}, () -> UsmConfig.get().armorMultiplier, v -> UsmConfig.get().armorMultiplier = v);
		number("ench_cap", GROUP_GEAR, new double[] {0, 5, 10, 20, 50, 100, 255}, () -> UsmConfig.get().enchantmentMaxLevel, v -> UsmConfig.get().enchantmentMaxLevel = (int) v);
		toggle("anvil", GROUP_GEAR, () -> UsmConfig.get().removeAnvilTooExpensive, v -> UsmConfig.get().removeAnvilTooExpensive = v);
		number("stack_cap", GROUP_GEAR, new double[] {1, 16, 32, 64, 99}, () -> UsmConfig.get().stackSizeCap, v -> UsmConfig.get().stackSizeCap = (int) v);
		number("fell_logs", GROUP_GEAR, new double[] {16, 64, 192, 512, 1024, 4096}, () -> UsmConfig.get().fellMaxLogs, v -> UsmConfig.get().fellMaxLogs = (int) v);
		number("vein_blocks", GROUP_GEAR, new double[] {8, 16, 48, 128, 512, 4096}, () -> UsmConfig.get().veinMineMaxBlocks, v -> UsmConfig.get().veinMineMaxBlocks = (int) v);
		toggle("fell_leaves", GROUP_GEAR, () -> UsmConfig.get().fellLeaves, v -> UsmConfig.get().fellLeaves = v);
		action("reset", GROUP_GEAR, List.of(), (p, i) -> {
			resetValues();
			return "Cheat values back to default";
		});
	}

	private Cheats() {
	}

	private static void toggle(String id, int group, BooleanSupplier get, Consumer<Boolean> set) {
		ALL.add(new Cheat(id, Kind.TOGGLE, group, ON_OFF, () -> get.getAsBoolean() ? 1 : 0, v -> set.accept(v > 0.5), List.of(), null));
	}

	private static void number(String id, int group, double[] values, DoubleSupplier get, DoubleConsumer set) {
		ALL.add(new Cheat(id, Kind.NUMBER, group, values, get, set, List.of(), null));
	}

	private static void action(String id, int group, List<String> choices, Run run) {
		ALL.add(new Cheat(id, Kind.ACTION, group, new double[0], () -> 0, v -> {
		}, choices, run));
	}

	public static Cheat find(String id) {
		for (Cheat c : ALL) {
			if (c.id().equals(id)) {
				return c;
			}
		}
		return null;
	}

	/** Display text of a choice: tier targets read "Tier 3" / "Everything", the rest are boss names. */
	public static String label(String cheat, int choice) {
		Cheat c = find(cheat);
		if (c == null || choice < 0 || choice >= c.choices().size()) {
			return "";
		}
		String key = c.choices().get(choice);
		if (key.matches("[1-5]")) {
			return "Tier " + key;
		}
		if (key.equals("all")) {
			return "Everything";
		}
		Gate g = Content.gate(key);
		return g == null ? key : g.name();
	}

	/** The current value as short text for the list ("ON", "x2", "12"); actions show nothing. */
	public static String valueText(Cheat c) {
		return switch (c.kind()) {
			case TOGGLE -> c.get().getAsDouble() > 0.5 ? "ON" : "OFF";
			case NUMBER -> {
				double v = c.values()[c.valueIndex()];
				boolean mult = c.id().endsWith("_hp") || c.id().endsWith("_dmg") || c.id().equals("armor_mult") || c.id().equals("mob_scaling");
				String t = v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
				yield mult ? "x" + t : t;
			}
			case ACTION -> "";
		};
	}

	/** Applies one request from a client. Ignored unless the sender is in creative mode. */
	public static void apply(ServerPlayer p, String id, int value) {
		if (!p.isCreative()) {
			return;
		}
		Cheat c = find(id);
		if (c == null) {
			return;
		}
		switch (c.kind()) {
			case TOGGLE -> c.set().accept(value != 0 ? 1 : 0);
			case NUMBER -> c.set().accept(c.values()[Math.max(0, Math.min(c.values().length - 1, value))]);
			case ACTION -> {
				int choice = c.choices().isEmpty() ? 0 : Math.max(0, Math.min(c.choices().size() - 1, value));
				p.sendSystemMessage(Component.literal(c.run().run(p, choice)), true);
				return;
			}
		}
		UsmConfig.save();
	}

	/** Puts every toggle and number back to its default (progression and Codex are left alone). */
	public static void resetValues() {
		UsmConfig d = UsmConfig.defaults();
		UsmConfig now = UsmConfig.get();
		now.freeRituals = d.freeRituals;
		now.ritualsAnywhere = d.ritualsAnywhere;
		now.bossesOnPeaceful = d.bossesOnPeaceful;
		now.invasionsEnabled = d.invasionsEnabled;
		now.bossHealthMultiplier = d.bossHealthMultiplier;
		now.bossDamageMultiplier = d.bossDamageMultiplier;
		now.bossLeashRange = d.bossLeashRange;
		now.invasionIntervalSeconds = d.invasionIntervalSeconds;
		now.vanillaMobScalingPerTier = d.vanillaMobScalingPerTier;
		now.accessorySlots = d.accessorySlots;
		now.noAbilityCooldowns = d.noAbilityCooldowns;
		now.abilityDamageMultiplier = d.abilityDamageMultiplier;
		now.weaponDamageMultiplier = d.weaponDamageMultiplier;
		now.armorMultiplier = d.armorMultiplier;
		now.enchantmentMaxLevel = d.enchantmentMaxLevel;
		now.removeAnvilTooExpensive = d.removeAnvilTooExpensive;
		now.stackSizeCap = d.stackSizeCap;
		now.fellMaxLogs = d.fellMaxLogs;
		now.veinMineMaxBlocks = d.veinMineMaxBlocks;
		now.fellLeaves = d.fellLeaves;
		UsmConfig.save();
	}
}
