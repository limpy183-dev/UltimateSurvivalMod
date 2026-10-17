package com.ultimatesurvival.ability;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.ItemDef;
import java.util.HashMap;
import java.util.Map;

/** Registry of ability implementations, keyed by the ids used in the content tables. */
public final class Abilities {
	private static final Map<String, Ability> MAP = new HashMap<>();

	private Abilities() {
	}

	public static Ability get(String id) {
		return MAP.get(id);
	}

	public static Ability get(AbilityRef ref) {
		return MAP.get(ref.id());
	}

	private static Ability make(String id, int cd) {
		Ability a = new Ability(id);
		a.defaultCooldown = cd;
		if (MAP.put(id, a) != null) {
			throw new IllegalStateException("duplicate ability " + id);
		}
		return a;
	}

	/** Right-click ability with a default cooldown (ticks). */
	public static Ability active(String id, int cd, Ability.Use use) {
		Ability a = make(id, cd);
		a.use = use;
		return a;
	}

	public static void onHit(String id, Ability.Hit hit) {
		make(id, 0).onHit = hit;
	}

	public static void onKill(String id, Ability.Hit kill) {
		make(id, 0).onKill = kill;
	}

	public static void modify(String id, Ability.Modify mod) {
		make(id, 0).modify = mod;
	}

	/** Wearer bonus (armor set / accessory): configure the returned ability's hooks. */
	public static Ability passive(String id) {
		return make(id, 0);
	}

	/** Ability whose logic lives elsewhere (set bonuses, accessory flags, tool traits); registered so content can reference it. */
	public static void marker(String id) {
		make(id, 0);
	}

	public static void bootstrap() {
		WeaponAbilities.register();
		FireAbilities.register();
		StormAbilities.register();
		BloodAbilities.register();
		TimeAbilities.register();
		VoidAbilities.register();
		LightAbilities.register();
		ApexAbilities.register();
		FrostAbilities.register();
		PlagueAbilities.register();
		CosmicAbilities.register();
		EternalAbilities.register();
		com.ultimatesurvival.armor.WearerAbilities.register();
		ConsumableAbilities.register();
		validate();
	}

	/** Fail loudly at startup if a content row references an ability that has no implementation. */
	private static void validate() {
		int bad = 0;
		for (ItemDef def : Content.ITEMS) {
			for (AbilityRef a : def.abilities) {
				if (!MAP.containsKey(a.id())) {
					UltimateSurvival.LOGGER.error("item {} references unknown ability '{}'", def.id, a.id());
					bad++;
				}
			}
		}
		if (bad > 0) {
			throw new IllegalStateException(bad + " unknown ability reference(s) in content.json");
		}
		UltimateSurvival.LOGGER.info("{} abilities registered", MAP.size());
	}
}
