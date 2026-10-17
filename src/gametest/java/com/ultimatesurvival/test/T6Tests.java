package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.google.gson.JsonObject;
import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ctx;
import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.UsmState;
import com.ultimatesurvival.util.Frozen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;

/** Tier 6 gear: Eternium tools, the frost / plague / cosmic / eternal weapons and every new ability, the four armor sets (flight and Second Wind among them), accessories, drinks and recipes. */
final class T6Tests {
	private T6Tests() {
	}

	private static Husk husk(TestSingleplayerContext w, double x, double y, double z) {
		Husk h = AbilityTests.spawn(w, x, y, z, 90);
		tank(w, h);
		return h;
	}

	private static void tank(TestSingleplayerContext w, LivingEntity e) {
		T.onServer(w, () -> {
			e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);
			e.setHealth(1000000);
		});
	}

	/** Poisoned - or, for the undead who cannot be, withering. */
	private static boolean rotting(LivingEntity e) {
		return e.hasEffect(MobEffects.POISON) || e.hasEffect(MobEffects.WITHER);
	}

	private static float lost(TestSingleplayerContext w, LivingEntity e) {
		return 1000000f - T.onServer(w, () -> e.getHealth());
	}

	private static void ready(TestSingleplayerContext w) {
		T.onServer(w, () -> T.player(w).getCooldowns().removeCooldown(T.player(w).getCooldowns().getCooldownGroup(T.player(w).getMainHandItem())));
	}

	private static void sneakUse(TestSingleplayerContext w) {
		T.onServer(w, () -> T.player(w).setShiftKeyDown(true));
		AbilityTests.use(w);
		T.onServer(w, () -> T.player(w).setShiftKeyDown(false));
	}

	private static double attack(TestSingleplayerContext w) {
		return T.onServer(w, () -> T.player(w).getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** Runs an ability's hit hook directly with the given parameters (so a chance can be pinned to 1). */
	private static void hitWith(TestSingleplayerContext w, String id, String json, LivingEntity target) {
		T.onServer(w, () -> {
			JsonObject o = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
			ServerPlayer p = T.player(w);
			Abilities.get(id).onHit.on(new Ctx(T.level(w), p, p.getMainHandItem(), new AbilityRef(id, o)), target);
		});
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		int ox = me.getX() - 50;
		int oy = me.getY();
		int oz = me.getZ() - 50;
		T.stand(w, ox - 3, oy, oz + 3, ox, oy + 1, oz + 3);
		ctx.waitTicks(40);
		ServerPlayer p = T.onServer(w, () -> T.player(w));

		// ---- the power pass: bigger numbers, shorter waits ------------------------------------------------------------------------------------------------------------------------
		check(Math.abs(T.stat("verdite_sword", "damage") - 10.0) < 1e-6 && Math.abs(T.stat("apex_blade", "damage") - 170.0) < 1e-6, "weapons hit harder across the board (verdite 10, apex 170)");
		check(T.stat("rimebrand", "damage") == 240.0 && T.stat("threadcutter", "damage") == 600.0, "the Tier 6 weapons start where the old endgame ended (240 and 600)");
		check(T.stat("verdite_sword", "speed") > 2.0 && T.ab("apex_blade", "cleave", "cd") < 100, "swings and abilities are faster (" + T.stat("verdite_sword", "speed") + " hits/s, cleave " + T.ab("apex_blade", "cleave", "cd") + " ticks)");
		check(com.ultimatesurvival.util.Fx.n(10) >= 16, "effects are bigger: ten particles became " + com.ultimatesurvival.util.Fx.n(10));

		// ---- registration -----------------------------------------------------------------------------------------------------------------------------------------------------------
		for (String id : new String[] {"eternium_shard", "eternium_ingot", "titan_heart", "eternium_pickaxe", "eternium_axe", "eternium_shovel", "eternium_hoe",
				"frost_core", "rime_sigil", "boreus_bag", "rimebrand", "glacier_maul", "hoarfrost_fang", "blizzard_staff", "winters_bow", "rimeguard_helmet", "rimeguard_boots",
				"heart_of_boreus", "glacier_charm", "frostfire_elixir", "rime_cake", "blight_core", "blight_sigil", "morbane_bag", "plaguebringer", "rotbone_greatsword", "venom_fang",
				"spore_staff", "miasma_bow", "blightbound_chestplate", "heart_of_morbane", "spore_sac", "blight_tonic", "fungal_stew", "star_core", "astral_sigil", "astraeus_bag",
				"comet_blade", "orrery_maul", "starlance", "stellar_staff", "galaxy_bow", "starborn_leggings", "heart_of_astraeus", "starwing_pendant", "stardust_elixir", "nebula_bread",
				"weave_core", "thread_sigil", "eternus_bag", "threadcutter", "loomhammer", "eternal_daggers", "chronicle_staff", "infinity_bow", "eternal_helmet", "heart_of_eternus",
				"ouroboros_ring", "thread_of_fate", "eternal_elixir", "ambrosia"}) {
			check(UsmItems.BY_ID.containsKey(id), id + " is registered");
		}
		for (String id : new String[] {"wyrm_rapier", "drake_glaive", "wyrm_scale_charm", "ash_scythe", "volcano_hammer", "ember_spear", "swiftness_anklet", "storm_rapier", "tempest_spear",
				"crimson_greatsword", "hunger_staff", "titan_belt", "chrono_dagger", "hourglass_hammer", "soul_lantern", "void_scythe", "rift_spear", "dawn_dagger", "radiant_greatsword",
				"apex_dagger", "apex_hammer", "apex_axe", "apex_shovel", "apex_hoe", "aegis_charm"}) {
			check(UsmItems.BY_ID.containsKey(id), "the arsenal expansion adds " + id);
		}
		check(UsmItems.BY_ID.size() >= 280, "the mod has 280+ items (" + UsmItems.BY_ID.size() + ")");

		// ---- gated crafting: Eternium waits for Boreus, the Rime Sigil for the Omnarch ----------------------------------------------------------------------------------------
		String[] press = {"eternium_shard", "eternium_shard", "", "eternium_shard", "eternium_shard", "", "", "", ""};
		check(T.craft(w, press).isEmpty(), "Eternium cannot be pressed before Boreus has fallen");
		String[] rime = {"minecraft:blue_ice", "aetherite_ingot", "minecraft:blue_ice", "aetherite_ingot", "omni_core", "aetherite_ingot", "minecraft:blue_ice", "aetherite_ingot", "minecraft:blue_ice"};
		check(T.craft(w, rime).isEmpty(), "the Rime Sigil cannot be crafted before the Omnarch has fallen");
		server.runCommand("usm unlock omnarch");
		ctx.waitTicks(3);
		check(T.craft(w, rime).is(UsmItems.get("rime_sigil")), "...and can afterwards (blue ice, aetherite and an omni core)");
		server.runCommand("usm unlock boreus");
		ctx.waitTicks(3);
		check(T.craft(w, press).is(UsmItems.get("eternium_ingot")), "four shards press into an Eternium Ingot once Boreus is dead");
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		// ---- locked gear is inert until its boss falls -------------------------------------------------------------------------------------------------------------------------
		T.hold(w, "rimebrand");
		ctx.waitTicks(3);
		check(Math.abs(attack(w) - 1.0) < 1e-6, "a locked Rimebrand hits like a fist");
		server.runCommand("usm unlock boreus");
		ctx.waitTicks(3);
		check(Math.abs(attack(w) - T.stat("rimebrand", "damage")) < 1e-6, "an unlocked Rimebrand hits for its table damage (" + attack(w) + ")");

		// ---- the Eternium pickaxe: a 13x13 face, eight deep, and a vein ---------------------------------------------------------------------------------------------------
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 8, oy + 14, oz + 14));
		T.stand(w, ox - 2.5, oy + 7, oz + 7.5, ox + 0.5, oy + 7.5, oz + 7.5);
		T.hold(w, "eternium_pickaxe");
		ctx.waitTicks(3);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 7, oz + 7)));
		int cleared = 0;
		for (int dx = 0; dx < 9; dx++) {
			for (int dy = 1; dy <= 13; dy++) {
				for (int dz = 1; dz <= 13; dz++) {
					cleared += T.air(w, new BlockPos(ox + dx, oy + dy, oz + dz)) ? 1 : 0;
				}
			}
		}
		check(cleared == 13 * 13 * 8, "the Eternium pickaxe cleared a 13x13 face eight deep (" + cleared + " of 1352)");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", ox, oy, oz, ox + 8, oy + 14, oz + 14));
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		// ---- Boreus's gear: absolute zero freezes, shatter breaks, frostbite locks, the blizzard grinds ----------------------------------------------------------------------
		int bx = ox;
		int bz = oz + 30;
		server.runCommand("usm unlock boreus");
		ctx.waitTicks(3);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk near = husk(w, bx + 4.5, oy, bz + 0.5);
		Husk mid = husk(w, bx - 9.5, oy, bz + 3.5);
		Husk far = husk(w, bx + 0.5, oy, bz + 40);
		T.hold(w, "rimebrand");
		ctx.waitTicks(3);
		ready(w);
		sneakUse(w);                                                           // second ability: the dash would move the player - the first (absolute zero) is the plain use
		ctx.waitTicks(1);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(2);
		check(lost(w, near) >= 300 && lost(w, mid) >= 200, "Absolute Zero hurts everything in its radius (" + lost(w, near) + " / " + lost(w, mid) + ")");
		check(T.onServer(w, () -> Frozen.is(near) && Frozen.is(mid) && near.hasEffect(MobEffects.SLOWNESS)), "and freezes it solid");
		check(lost(w, far) == 0 && !T.onServer(w, () -> Frozen.is(far)), "but not a husk forty blocks away");
		T.takeShot(ctx, "t6-absolute-zero");
		// a blow against ice is a blow against glass
		float n0 = lost(w, near);
		T.onServer(w, () -> {
			near.invulnerableTime = 0;
			near.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(p), 4f);
		});
		float shattered = lost(w, near) - n0;
		float f0 = lost(w, far);
		T.onServer(w, () -> {
			far.invulnerableTime = 0;
			far.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(p), 4f);
		});
		float plain = lost(w, far) - f0;
		check(shattered > plain * 2.5f, "Shatter lands several times harder on frozen ice (" + shattered + " vs " + plain + ")");
		check(!T.onServer(w, () -> Frozen.is(near)), "and breaks it");
		server.runCommand("kill @e[type=minecraft:husk]");

		Husk bit = husk(w, bx + 2.5, oy, bz + 0.5);
		hitWith(w, "frostbite", "{\"chance\":1.0,\"ticks\":60}", bit);
		check(T.onServer(w, () -> Frozen.is(bit) && bit.hasEffect(MobEffects.SLOWNESS)), "Frostbite locks its target in ice");
		server.runCommand("kill @e[type=minecraft:husk]");

		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 14, oy, bz + 0.5);
		Husk s1 = husk(w, bx + 14.5, oy, bz + 0.5);
		Husk s2 = husk(w, bx + 17.5, oy, bz + 3.5);
		T.hold(w, "blizzard_staff");
		ctx.waitTicks(3);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(45);
		check(lost(w, s1) >= 400 && lost(w, s2) >= 400 && T.onServer(w, () -> s1.hasEffect(MobEffects.SLOWNESS)), "the Blizzard grinds and slows everything inside it (" + lost(w, s1) + " / " + lost(w, s2) + ")");
		T.takeShot(ctx, "t6-blizzard");
		ctx.waitTicks(110);
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- Morbane's gear: the plague spreads from corpse to corpse --------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock morbane");
		ctx.waitTicks(3);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk carrier = AbilityTests.spawn(w, bx + 6.5, oy, bz + 0.5, 90);
		Husk neighbour = husk(w, bx + 9.5, oy, bz + 2.5);
		Husk elsewhere = husk(w, bx + 6.5, oy, bz + 30);
		T.hold(w, "plaguebringer");
		ctx.waitTicks(3);
		hitWith(w, "contagion", "{\"dmg\":180,\"radius\":11,\"ticks\":200}", carrier);
		check(T.onServer(w, () -> rotting(carrier)), "Contagion poisons its target");
		T.onServer(w, () -> carrier.kill(T.level(w)));
		ctx.waitTicks(3);
		check(lost(w, neighbour) >= 150 && T.onServer(w, () -> rotting(neighbour)), "and when it dies the plague bursts out and finds its neighbour (" + lost(w, neighbour) + ")");
		check(lost(w, elsewhere) == 0, "but not something thirty blocks away");
		server.runCommand("kill @e[type=minecraft:husk]");

		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk p1 = husk(w, bx + 4.5, oy, bz + 0.5);
		Husk p2 = husk(w, bx - 6.5, oy, bz + 0.5);
		T.hold(w, "plaguebringer");
		ctx.waitTicks(3);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(lost(w, p1) >= 400 && lost(w, p2) >= 400 && T.onServer(w, () -> rotting(p1)), "Plague Burst hurts and infects everything around the wielder (" + lost(w, p1) + ")");
		T.takeShot(ctx, "t6-plague-burst");
		ctx.waitTicks(40);
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 14, oy, bz + 0.5);
		Husk c1 = husk(w, bx + 14.5, oy, bz + 0.5);
		ready(w);
		sneakUse(w);
		ctx.waitTicks(40);
		check(lost(w, c1) >= 200 && T.onServer(w, () -> rotting(c1)), "the Toxic Cloud poisons and eats whatever stands in it (" + lost(w, c1) + ")");
		ctx.waitTicks(180);
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- Astraeus's gear: stars fall, a beam from orbit ---------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock astraeus");
		ctx.waitTicks(3);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 20, oy, bz + 0.5);
		Husk o1 = husk(w, bx + 20.5, oy, bz + 0.5);
		Husk o2 = husk(w, bx + 20.5, oy, bz + 30);
		T.hold(w, "stellar_staff");
		ctx.waitTicks(3);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(10);
		T.takeShot(ctx, "t6-orbital-strike-charging");
		ctx.waitTicks(20);
		check(lost(w, o1) >= 1800 && lost(w, o2) == 0, "the Orbital Strike lands on the husk it was pointed at, and only there (" + lost(w, o1) + ")");
		T.takeShot(ctx, "t6-orbital-strike");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 20, oy, bz + 0.5);
		Husk[] rain = new Husk[5];
		for (int i = 0; i < rain.length; i++) {
			rain[i] = husk(w, bx + 20.5 + (i - 2) * 3.0, oy, bz + 0.5 + (i % 2) * 3);
		}
		T.hold(w, "comet_blade");
		ctx.waitTicks(3);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(40);
		T.takeShot(ctx, "t6-meteor-shower");
		ctx.waitTicks(80);
		float most = 0;
		for (Husk h : rain) {
			most = Math.max(most, lost(w, h));
		}
		check(most >= 400, "the Meteor Shower hits what stands under it (the worst-hit husk lost " + most + ")");
		server.runCommand("kill @e[type=minecraft:husk]");
		Husk stricken = husk(w, bx + 4.5, oy, bz + 0.5);
		hitWith(w, "starfall", "{\"chance\":1.0,\"dmg\":300,\"radius\":4}", stricken);
		ctx.waitTicks(6);
		check(lost(w, stricken) >= 250, "Starfall drops a star on its target (" + lost(w, stricken) + ")");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- the four armor sets ---------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock eternus");
		ctx.waitTicks(3);
		String[][] sets = {{"rimeguard", "frost_aura", "rime_shell", "regen_ooc"}, {"blightbound", "blight_aura", "damage_reduction", "regen_ooc"}, {"starborn", "flight", "star_aura", "quicken"},
				{"eternal", "second_wind", "omni_aura", "quicken", "damage_reduction"}};
		for (String[] set : sets) {
			T.wear(w, set[0]);
			ctx.waitTicks(12);
			boolean all = true;
			for (int i = 1; i < set.length; i++) {
				final String id = set[i];
				all &= T.onServer(w, () -> Wearer.has(p, id));
			}
			check(all, "four " + set[0] + " pieces grant every bonus of the set");
			check(Math.abs(T.onServer(w, () -> p.getAttributeValue(Attributes.ARMOR)) - T.setArmor(set[0])) < 1e-6, "and the set's armor points (" + T.setArmor(set[0]) + ")");
			ctx.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			ctx.waitTicks(25);
			T.takeShot(ctx, "t6-armor-" + set[0]);
			ctx.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			T.stripArmor(w);
			ctx.waitTicks(4);
		}

		// ---- flight: worn, it lets a survival player fly; taken off, it does not ------------------------------------------------------------------------------------------------
		T.wear(w, "starborn");
		ctx.waitTicks(12);
		check(T.onServer(w, () -> p.getAbilities().mayfly), "the Starborn set lets a survival player fly");
		T.onServer(w, () -> {
			p.teleportTo(p.getX(), p.getY() + 6, p.getZ());                             // landing cancels flight, so leave the ground first
			p.getAbilities().flying = true;
			p.onUpdateAbilities();
		});
		ctx.waitTicks(30);
		check(T.onServer(w, () -> p.getAbilities().flying), "and keep flying");
		T.takeShot(ctx, "t6-flight");
		T.stripArmor(w);
		ctx.waitTicks(12);
		check(!T.onServer(w, () -> p.getAbilities().mayfly && p.getAbilities().flying), "taking the set off takes the wings away");
		var list = T.onServer(w, () -> UsmState.progress().accessories(p.getUUID()));
		T.onServer(w, () -> list.set(0, new ItemStack(UsmItems.get("starwing_pendant"))));
		ctx.waitTicks(15);
		check(T.onServer(w, () -> p.getAbilities().mayfly), "the Starwing Pendant does the same from the accessory slot");
		T.onServer(w, () -> list.set(0, ItemStack.EMPTY));
		ctx.waitTicks(15);
		check(!T.onServer(w, () -> p.getAbilities().mayfly), "and nothing is left when it comes off");

		// ---- the orrery: stars circle the wearer and cut what touches them --------------------------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 6, oy, bz + 0.5);
		T.wear(w, "starborn");
		Husk circled = husk(w, bx + 3.5, oy, bz + 0.5);
		ctx.waitTicks(60);
		check(lost(w, circled) >= 100, "the Starborn stars cut a husk standing in their orbit (" + lost(w, circled) + ")");
		T.stripArmor(w);
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- the frost aura of Rimeguard: slows, hurts, sometimes freezes ------------------------------------------------------------------------------------------------------
		T.wear(w, "rimeguard");
		Husk chilled = husk(w, bx + 3.5, oy, bz + 0.5);
		ctx.waitTicks(80);
		check(lost(w, chilled) >= 100 && T.onServer(w, () -> chilled.hasEffect(MobEffects.SLOWNESS)), "Rimeguard's aura chills and wears down what stands near (" + lost(w, chilled) + ")");
		T.stripArmor(w);
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- Second Wind: a killing blow is refused, once ---------------------------------------------------------------------------------------------------------------------------
		T.wear(w, "eternal");
		Husk frozenByWind = husk(w, bx + 4.5, oy, bz + 0.5);
		ctx.waitTicks(12);
		T.onServer(w, () -> p.setHealth(2));
		T.onServer(w, () -> p.hurtServer(T.level(w), T.level(w).damageSources().generic(), 100000f));
		ctx.waitTicks(3);
		check(T.onServer(w, () -> p.isAlive()) && T.onServer(w, () -> p.getHealth()) >= T.onServer(w, () -> p.getMaxHealth()) * 0.9f, "a killing blow is refused and the wearer stands back up whole ("
				+ T.onServer(w, () -> p.getHealth()) + " HP)");
		check(T.onServer(w, () -> Frozen.is(frozenByWind)), "and everything nearby stops");
		check(!T.onServer(w, () -> com.ultimatesurvival.ability.EternalAbilities.windReady(p)), "the Second Wind then needs time to recharge");
		T.takeShot(ctx, "t6-second-wind");
		T.stripArmor(w);
		T.onServer(w, () -> p.removeAllEffects());
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- the Threadcutter: omega slash and the dash ---------------------------------------------------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk e1 = husk(w, bx + 6.5, oy, bz + 0.5);
		Husk e2 = husk(w, bx - 8.5, oy, bz + 4.5);
		T.hold(w, "threadcutter");
		ctx.waitTicks(3);
		ready(w);
		AbilityTests.use(w);
		ctx.waitTicks(30);
		check(lost(w, e1) >= 2000 && lost(w, e2) >= 2000, "Omega Slash strikes everything around the wielder several times (" + lost(w, e1) + " / " + lost(w, e2) + ")");
		T.takeShot(ctx, "t6-omega-slash");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- accessories, hearts, drinks -------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> list.set(0, new ItemStack(UsmItems.get("heart_of_eternus"))));
		T.onServer(w, () -> list.set(1, new ItemStack(UsmItems.get("heart_of_astraeus"))));
		ctx.waitTicks(15);
		check(Math.abs(T.onServer(w, () -> p.getMaxHealth()) - (20.0 + T.attr("heart_of_eternus", "max_health") + T.attr("heart_of_astraeus", "max_health"))) < 1e-6,
				"the Hearts of Eternus and Astraeus add their hearts (" + T.onServer(w, () -> p.getMaxHealth()) + ")");
		T.onServer(w, () -> list.replaceAll(s -> ItemStack.EMPTY));
		ctx.waitTicks(15);
		double hp0 = T.onServer(w, () -> p.getMaxHealth());
		T.onServer(w, () -> new ItemStack(UsmItems.get("titan_heart")).finishUsingItem(T.level(w), p));
		ctx.waitTicks(3);
		check(Math.abs(T.onServer(w, () -> p.getMaxHealth()) - (hp0 + 2.0)) < 1e-6, "a Titan Heart adds one heart");
		T.onServer(w, () -> UsmState.progress().addCounter("hearts_" + p.getUUID(), 30));
		T.onServer(w, () -> com.ultimatesurvival.ability.ConsumableAbilitiesAccess.applyHearts(p));
		double capped = T.onServer(w, () -> p.getMaxHealth());
		T.onServer(w, () -> new ItemStack(UsmItems.get("titan_heart")).finishUsingItem(T.level(w), p));
		ctx.waitTicks(3);
		check(Math.abs(T.onServer(w, () -> p.getMaxHealth()) - capped) < 1e-6, "and refuses past twenty");
		T.onServer(w, () -> p.removeAllEffects());
		T.onServer(w, () -> new ItemStack(UsmItems.get("eternal_elixir")).finishUsingItem(T.level(w), p));
		check(T.onServer(w, () -> p.getEffect(MobEffects.STRENGTH).getAmplifier()) == (int) T.ab("eternal_elixir", "give_effect", "amp")
				&& T.onServer(w, () -> p.getEffect(MobEffects.RESISTANCE).getAmplifier()) == (int) T.ab("eternal_elixir", "give_effect", "amp2"),
				"the Eternal Elixir grants the Strength and Resistance its table names");
		T.onServer(w, () -> p.removeAllEffects());

		// ---- the bows --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		for (String bow : new String[] {"winters_bow", "miasma_bow", "galaxy_bow", "infinity_bow"}) {
			T.hold(w, bow);
			ctx.waitTicks(3);
			ItemStack stack = T.onServer(w, () -> p.getMainHandItem());
			T.onServer(w, () -> stack.getItem().releaseUsing(stack, T.level(w), p, 72000 - 20));
			int shots = T.onServer(w, () -> T.level(w).getEntitiesOfClass(com.ultimatesurvival.entity.Bolt.class, p.getBoundingBox().inflate(40, 20, 40)).size());
			check(shots >= (int) T.ab(bow, "bow_shot", "count"), bow + " fires its volley without ammunition (" + shots + ")");
			server.runCommand("kill @e[type=ultimatesurvival:bolt]");
		}
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(5);
	}
}
