package com.ultimatesurvival.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ultimatesurvival.UltimateSurvival;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The single tuning file: config/ultimatesurvival.json. Every field has a default, unknown
 * keys are dropped and missing keys are written back on load, so the file documents itself.
 * Values that shape registered items (damage/armor multipliers) apply on the next game start.
 */
public final class UsmConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static UsmConfig instance = new UsmConfig();
	/** 6 slots in the original panel + 8 rows of 9 below it (the last row holds one). */
	public static final int MAX_ACCESSORY_SLOTS = 70;
	private static final int VERSION = 2;

	// --- combat tuning ---------------------------------------------------------------------
	/** Multiplies the damage of every custom weapon (applied when items register). */
	public double weaponDamageMultiplier = 1.0;
	/** Multiplies armor points of custom armor. */
	public double armorMultiplier = 1.0;
	/** Multiplies the damage of every ability effect (slams, waves, lightning ...). */
	public double abilityDamageMultiplier = 1.0;

	// --- bosses & world --------------------------------------------------------------------
	public double bossHealthMultiplier = 1.0;
	public double bossDamageMultiplier = 1.0;
	/** Vanilla hostile mobs gain this fraction of health/damage per cleared tier above 1 (0 = off). */
	public double vanillaMobScalingPerTier = 0.20;
	/** How strongly custom invader mobs scale with the player's tier. */
	public double invaderScalingPerTier = 0.25;
	public boolean invasionsEnabled = true;
	/** Seconds between invasion rolls (per player). */
	public int invasionIntervalSeconds = 600;
	/** Bosses vanish (like the Wither) when the difficulty is Peaceful; true lets them fight anyway. */
	public boolean bossesOnPeaceful = false;
	/** A boss dragged farther than this from where it spawned teleports home and forgets its target. */
	public double bossLeashRange = 96;
	/** Ritual altars work in any dimension (default: each boss has its own dimension / time / sky rule). */
	public boolean ritualsAnywhere = false;
	/** Skip the cost of a ritual (hearts / experience / extra items) - sigil still consumed. */
	public boolean freeRituals = false;

	// --- raised limits ---------------------------------------------------------------------
	/** Max level for enchantments whose vanilla max is above 1. 0 = vanilla behaviour. */
	public int enchantmentMaxLevel = 10;
	public boolean removeAnvilTooExpensive = true;
	/** Raises the vanilla 1024 cap of the max-health attribute (needed for bosses above 1024 HP). 0 = leave vanilla. */
	public double maxHealthCap = 1_000_000;
	/** Stackable items (max stack above 1) hold up to this many (hard maximum 99). 0 or 1 = leave vanilla. */
	public int stackSizeCap = 99;

	// --- visuals ---------------------------------------------------------------------------
	/** Cuts particle counts roughly in half and skips screen shake/flash. */
	public boolean reducedEffects = false;
	public double screenShakeStrength = 1.0;
	/** How bright the full-screen flashes of big moments get (0 = none, 1 = as designed; capped at 80% opacity and one per second). Photosensitive? Set 0. */
	public double screenFlashStrength = 1.0;
	/** Hard cap on the particles of one effect call. */
	public int maxParticlesPerEffect = 192;
	/** Multiplies the particle count of every ability / attack effect (1 = the original amount). */
	public double effectIntensity = 1.6;
	/** Format of this file: older files get the power pass's new defaults once (see {@link #load}). */
	public int configVersion = 0;

	// --- quality of life -------------------------------------------------------------------
	public boolean giveCodexOnFirstJoin = true;
	public boolean bossMusic = true;
	/** Felling axes also remove the leaves of the felled tree. */
	public boolean fellLeaves = true;
	public int fellMaxLogs = 384;
	public int veinMineMaxBlocks = 256;

	// --- creative cheats (Codex > Creative page; also editable here) ---------------------------
	/** How many accessory slots the player has (6 = the normal amount). */
	public int accessorySlots = 6;
	/** Weapon and gadget abilities recharge in a single tick. */
	public boolean noAbilityCooldowns = false;

	private UsmConfig() {
	}

	public static UsmConfig get() {
		return instance;
	}

	/** Keeps hand-edited nonsense (a negative multiplier, a stack cap of 5000) inside what the game can actually represent. */
	public void sanitize() {
		weaponDamageMultiplier = clamp(weaponDamageMultiplier, 0.01, 1000);
		armorMultiplier = clamp(armorMultiplier, 0.01, 100);
		abilityDamageMultiplier = clamp(abilityDamageMultiplier, 0, 1000);
		bossHealthMultiplier = clamp(bossHealthMultiplier, 0.01, 1000);
		bossDamageMultiplier = clamp(bossDamageMultiplier, 0, 1000);
		vanillaMobScalingPerTier = clamp(vanillaMobScalingPerTier, 0, 10);
		invaderScalingPerTier = clamp(invaderScalingPerTier, 0, 10);
		invasionIntervalSeconds = (int) clamp(invasionIntervalSeconds, 30, 86_400);
		bossLeashRange = clamp(bossLeashRange, 24, 1024);
		enchantmentMaxLevel = (int) clamp(enchantmentMaxLevel, 0, 255);
		maxHealthCap = clamp(maxHealthCap, 0, 1e9);
		stackSizeCap = (int) clamp(stackSizeCap, 0, 99);
		screenShakeStrength = clamp(screenShakeStrength, 0, 5);
		screenFlashStrength = clamp(screenFlashStrength, 0, 1);
		maxParticlesPerEffect = (int) clamp(maxParticlesPerEffect, 0, 4096);
		effectIntensity = clamp(effectIntensity, 0.1, 4);
		fellMaxLogs = (int) clamp(fellMaxLogs, 1, 4096);
		veinMineMaxBlocks = (int) clamp(veinMineMaxBlocks, 1, 4096);
		accessorySlots = (int) clamp(accessorySlots, 6, MAX_ACCESSORY_SLOTS);
	}

	private static double clamp(double v, double lo, double hi) {
		return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
	}

	/** Writes the current values back to the config file (the Creative cheats page edits the live config). */
	public static void save() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(UltimateSurvival.MOD_ID + ".json");
		try {
			instance.sanitize();
			Files.writeString(file, GSON.toJson(instance));
		} catch (IOException e) {
			UltimateSurvival.LOGGER.error("Could not write {}", file, e);
		}
	}

	/** A copy of the default configuration (what "reset cheats" returns to). */
	public static UsmConfig defaults() {
		UsmConfig c = new UsmConfig();
		c.configVersion = VERSION;
		return c;
	}

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(UltimateSurvival.MOD_ID + ".json");
		try {
			if (Files.exists(file)) {
				UsmConfig read = GSON.fromJson(Files.readString(file), UsmConfig.class);
				if (read != null) {
					if (read.configVersion < VERSION) {                      // power pass: bigger caps for bigger effects, vein mining and felling
						read.maxParticlesPerEffect = Math.max(read.maxParticlesPerEffect, 192);
						read.veinMineMaxBlocks = Math.max(read.veinMineMaxBlocks, 256);
						read.fellMaxLogs = Math.max(read.fellMaxLogs, 384);
						read.configVersion = VERSION;
					}
					read.sanitize();
					instance = read;
				}
			}
			instance.configVersion = Math.max(instance.configVersion, VERSION);          // a fresh file is born current
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(instance));
		} catch (IOException | RuntimeException e) {
			UltimateSurvival.LOGGER.error("Could not read/write {} - using defaults", file, e);
		}
	}
}
