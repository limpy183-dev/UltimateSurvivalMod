package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.UsmState;
import com.ultimatesurvival.util.LightZones;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Tier 5 gear: Aetherite and Apex tools, the void / light / apex weapons and their abilities, the three armor sets, accessories, drinks and recipes. */
final class T5Tests {
	private T5Tests() {
	}

	private static Husk husk(TestSingleplayerContext w, double x, double y, double z) {
		Husk h = AbilityTests.spawn(w, x, y, z, 90);
		tank(w, h);
		return h;
	}

	/** A pig with AI on (NoAI mobs ignore every push) and a huge health pool: it can be hauled around and damage can be measured. */
	private static Pig pig(TestSingleplayerContext w, double x, double y, double z) {
		Pig p = T.onServer(w, () -> {
			Pig pg = new Pig(EntityTypes.PIG, T.level(w));
			pg.setPersistenceRequired();
			pg.snapTo(x, y, z, 90, 0);
			T.level(w).addFreshEntity(pg);
			return pg;
		});
		tank(w, p);
		return p;
	}

	private static void tank(TestSingleplayerContext w, LivingEntity e) {
		T.onServer(w, () -> {
			e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
			e.setHealth(100000);
		});
	}

	private static float hp(TestSingleplayerContext w, LivingEntity e) {
		return T.onServer(w, () -> e.getHealth());
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

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		int ox = me.getX() + 40;
		int oy = me.getY();
		int oz = me.getZ() + 40;
		T.stand(w, ox - 3, oy, oz + 3, ox, oy + 1, oz + 3);
		ctx.waitTicks(40);

		// ---- registration, worldgen, recipes ------------------------------------------------------------------------------------------------------------------------------
		for (String id : new String[] {"aetherite_pickaxe", "aetherite_axe", "aetherite_shovel", "aetherite_hoe", "voidrender", "event_horizon", "null_dagger", "singularity_staff", "rift_bow",
				"solaris_edge", "dawnbreaker", "seraph_spear", "prism_staff", "daybreak_bow", "apex_blade", "worldsplitter", "apex_bow", "weavers_staff", "apex_pickaxe",
				"voidwalker_helmet", "seraph_chestplate", "apex_leggings", "apex_boots", "void_shard", "null_ring", "heart_of_nihilus", "sun_pendant", "prism_ring",
				"heart_of_solareon", "heart_of_the_omnarch", "weavers_thread", "apex_signet", "void_tonic", "null_elixir", "radiant_elixir", "dawn_bread", "apex_elixir", "omni_feast",
				"lumen_flare", "void_core", "radiant_core", "omni_core", "aetherite_ingot", "raw_aetherite", "void_sigil", "radiant_sigil", "omni_sigil", "aetherite_block", "aetherite_ore"}) {
			check(UsmItems.BY_ID.containsKey(id) || com.ultimatesurvival.block.UsmBlocks.BY_ID.containsKey(id), id + " is registered");
		}
		check(UsmItems.BY_ID.size() >= 180, "the mod has 180+ items (" + UsmItems.BY_ID.size() + ")");
		boolean endOre = T.onServer(w, () -> {
			var biomes = T.level(w).registryAccess().lookupOrThrow(Registries.BIOME);
			var key = ResourceKey.create(Registries.PLACED_FEATURE, UltimateSurvival.id("ore_aetherite"));
			boolean inEnd = biomes.listElements().anyMatch(b -> b.key().identifier().getPath().startsWith("end_") && b.value().getGenerationSettings().features().stream()
					.anyMatch(step -> step.stream().anyMatch(f -> f.is(key))));
			boolean inPlains = biomes.get(ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.withDefaultNamespace("plains"))).map(b -> b.value().getGenerationSettings()
					.features().stream().anyMatch(step -> step.stream().anyMatch(f -> f.is(key)))).orElse(true);
			return inEnd && !inPlains;
		});
		check(endOre, "aetherite ore generates in the End and nowhere else");

		// gated crafting: the void sigil needs Chronos to have fallen
		String[] voidSigil = {"minecraft:crying_obsidian", "minecraft:ender_eye", "minecraft:crying_obsidian", "minecraft:ender_eye", "chrono_core", "minecraft:ender_eye",
				"minecraft:crying_obsidian", "minecraft:ender_eye", "minecraft:crying_obsidian"};
		check(T.craft(w, voidSigil).isEmpty(), "the Void Sigil cannot be crafted before Chronos has fallen");
		server.runCommand("usm unlock chronos");
		ctx.waitTicks(3);
		check(T.craft(w, voidSigil).is(UsmItems.get("void_sigil")), "...and can afterwards (chrono core + eyes of ender + crying obsidian)");
		ItemStack flares = T.craft(w, new String[] {"minecraft:glowstone_dust", "minecraft:blaze_powder", "minecraft:amethyst_shard", "minecraft:gunpowder", "", "", "", "", ""});
		check(flares.is(UsmItems.get("lumen_flare")) && flares.getCount() == 3, "a Lumen Flare recipe yields three flares");
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		// ---- locked: the gear is inert until its boss falls ------------------------------------------------------------------------------------------------------------------
		T.hold(w, "voidrender");
		ctx.waitTicks(3);
		check(Math.abs(attack(w) - 1.0) < 1e-6, "a locked Voidrender hits like a fist");
		server.runCommand("usm unlock nihilus");
		ctx.waitTicks(3);
		check(Math.abs(attack(w) - T.stat("voidrender", "damage")) < 1e-6, "an unlocked Voidrender hits for its table damage (" + attack(w) + ")");
		T.hold(w, "apex_blade");
		ctx.waitTicks(3);
		check(Math.abs(attack(w) - 1.0) < 1e-6, "the Apex Blade stays locked until the Omnarch falls");
		T.hold(w, "voidrender");

		// ---- Aetherite pickaxe: 7x7 face, five deep; sneaking mines one block ---------------------------------------------------------------------------------------------
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 6, oy + 8, oz + 8));
		T.stand(w, ox - 2.5, oy + 4, oz + 4.5, ox + 0.5, oy + 4.5, oz + 4.5);
		T.hold(w, "aetherite_pickaxe");
		ctx.waitTicks(3);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 4, oz + 4)));
		int cleared = 0;
		for (int dx = 0; dx < 7; dx++) {
			for (int dy = 1; dy <= 7; dy++) {
				for (int dz = 1; dz <= 7; dz++) {
					cleared += T.air(w, new BlockPos(ox + dx, oy + dy, oz + dz)) ? 1 : 0;
				}
			}
		}
		check(cleared == 7 * 7 * 5, "the Aetherite pickaxe cleared a 7x7 face five deep (" + cleared + " of 245)");
		check(!T.air(w, new BlockPos(ox, oy, oz + 4)) && !T.air(w, new BlockPos(ox + 5, oy + 4, oz + 4)) && !T.air(w, new BlockPos(ox, oy + 4, oz + 8)), "and nothing outside it");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", ox, oy, oz, ox + 6, oy + 8, oz + 8));

		// ---- Apex pickaxe: 9x9 face, six deep ---------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock omnarch");
		ctx.waitTicks(3);
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 7, oy + 10, oz + 10));
		T.stand(w, ox - 2.5, oy + 5, oz + 5.5, ox + 0.5, oy + 5.5, oz + 5.5);
		T.hold(w, "apex_pickaxe");
		ctx.waitTicks(3);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 5, oz + 5)));
		int big = 0;
		for (int dx = 0; dx < 7; dx++) {
			for (int dy = 1; dy <= 9; dy++) {
				for (int dz = 1; dz <= 9; dz++) {
					big += T.air(w, new BlockPos(ox + dx, oy + dy, oz + dz)) ? 1 : 0;
				}
			}
		}
		check(big == 9 * 9 * 6, "the Apex pickaxe cleared a 9x9 face six deep (" + big + " of 486)");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", ox, oy, oz, ox + 7, oy + 10, oz + 10));
		server.runCommand("usm reset");                              // back to stage one
		ctx.waitTicks(3);

		// ---- Voidrender: rend tears extra health; gravity well hauls enemies in ---------------------------------------------------------------------------------------------------------
		int bx = ox;
		int bz = oz + 24;
		server.runCommand("usm unlock nihilus");
		ctx.waitTicks(3);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		Husk plain = husk(w, bx + 2.0, oy, bz + 0.5);
		T.hold(w, "voidrender");
		ctx.waitTicks(25);
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		float a0 = hp(w, plain);
		T.onServer(w, () -> p.attack(plain));
		float lost = a0 - hp(w, plain);
		check(lost >= 100, "each Voidrender hit rends 80 extra health (lost " + lost + ", max " + T.onServer(w, () -> plain.getMaxHealth()) + ", strength "
				+ T.onServer(w, () -> p.getAttackStrengthScale(0.5f)) + ", held " + T.onServer(w, () -> p.getMainHandItem().getItem()) + ")");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Pig pulled = pig(w, bx + 10.5, oy, bz + 0.5);
		Pig behind = pig(w, bx - 8.5, oy, bz + 0.5);
		Pig farAway = pig(w, bx + 0.5, oy, bz + 30);
		ready(w);
		float pf = hp(w, farAway);
		double dFar0 = T.onServer(w, () -> pulled.position().distanceTo(new Vec3(bx + 0.5, oy, bz + 0.5)));
		sneakUse(w);
		ctx.waitTicks(8);
		double dFar1 = T.onServer(w, () -> pulled.position().distanceTo(new Vec3(bx + 0.5, oy, bz + 0.5)));
		check(dFar1 < dFar0 - 4, "the gravity well hauls enemies toward the wielder (" + String.format("%.1f -> %.1f", dFar0, dFar1) + ")");
		check(T.onServer(w, () -> behind.position().distanceTo(new Vec3(bx + 0.5, oy, bz + 0.5))) < 6.5, "...from every direction");
		check(hp(w, pulled) <= 100000 - 39 && T.onServer(w, () -> pulled.hasEffect(MobEffects.SLOWNESS)), "and hurts and slows them");
		check(hp(w, farAway) == pf, "but not one 30 blocks away");
		server.runCommand("kill @e[type=minecraft:pig]");

		// ---- Event Horizon: a singularity drags and burns ----------------------------------------------------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy, bz + 0.5);
		Pig core = pig(w, bx + 10.5, oy, bz + 0.5);
		Pig rim = pig(w, bx + 10.5, oy, bz + 7.5);
		T.hold(w, "event_horizon");
		ctx.waitTicks(3);
		float c0 = hp(w, core);
		double r0 = T.onServer(w, () -> rim.position().distanceTo(core.position()));
		AbilityTests.use(w);
		ctx.waitTicks(40);
		check(c0 - hp(w, core) >= 100, "a singularity burns whoever sits at its core (lost " + (c0 - hp(w, core)) + ")");
		double r1 = T.onServer(w, () -> rim.position().distanceTo(core.position()));
		check(r1 < r0 - 1.0, "and drags the others toward it (" + String.format("%.1f -> %.1f", r0, r1) + ")");
		T.takeShot(ctx, "t5-singularity");
		ctx.waitTicks(60);
		server.runCommand("kill @e[type=minecraft:pig]");

		// ---- Null Dagger: a long blink, a heavy backstab, devour on kill ------------------------------------------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.6, bz + 0.5);
		T.hold(w, "null_dagger");
		ctx.waitTicks(3);
		Vec3 s0 = T.onServer(w, () -> p.position());
		AbilityTests.use(w);
		check(T.onServer(w, () -> p.position()).x - s0.x > 12, "the Null Dagger blinks far forward");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.6, bz + 0.5);
		T.onServer(w, () -> p.setHealth(10));
		Husk victim = AbilityTests.spawn(w, bx + 1.5, oy, bz + 0.5, 0);
		T.onServer(w, () -> victim.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(p), 100000f));
		ctx.waitTicks(3);
		check(T.onServer(w, () -> p.getHealth()) >= 16f && T.onServer(w, () -> p.hasEffect(MobEffects.ABSORPTION)), "Devour restores health and grants absorption on a kill (" + T.onServer(w, () -> p.getHealth()) + " HP)");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- the Voidwalker set: entropy slows and wears enemies down; three jumps, evasion ---------------------------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		T.wear(w, "voidwalker");
		ctx.waitTicks(12);
		check(T.onServer(w, () -> Wearer.has(p, "entropy_aura") && Wearer.has(p, "dodge") && Wearer.has(p, "double_jump")), "four Voidwalker pieces grant all three bonuses");
		check(Math.abs(T.onServer(w, () -> p.getAttributeValue(Attributes.ARMOR)) - T.setArmor("voidwalker")) < 1e-6, "the Voidwalker set's armor points (got " + T.onServer(w, () -> p.getAttributeValue(Attributes.ARMOR)) + ")");
		Husk near = husk(w, bx + 3.5, oy, bz + 0.5);
		ctx.waitTicks(45);
		check(100000 - hp(w, near) >= 30 && T.onServer(w, () -> near.hasEffect(MobEffects.SLOWNESS)), "the entropy aura wears down and slows what stands near (" + (100000 - hp(w, near)) + " damage)");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stripArmor(w);

		// ---- accessories and drinks (stage one) ------------------------------------------------------------------------------------------------------------------------------------------------------------
		var list = T.onServer(w, () -> UsmState.progress().accessories(p.getUUID()));
		T.onServer(w, () -> list.set(0, new ItemStack(UsmItems.get("heart_of_nihilus"))));
		ctx.waitTicks(12);
		check(Math.abs(T.onServer(w, () -> p.getMaxHealth()) - (20.0 + T.attr("heart_of_nihilus", "max_health"))) < 1e-6, "the Heart of Nihilus adds its hearts (" + T.onServer(w, () -> p.getMaxHealth()) + ")");
		T.onServer(w, () -> list.set(0, ItemStack.EMPTY));
		ctx.waitTicks(12);
		T.onServer(w, () -> new ItemStack(UsmItems.get("void_tonic")).finishUsingItem(T.level(w), p));
		check(T.onServer(w, () -> p.getEffect(MobEffects.STRENGTH).getAmplifier()) == (int) T.ab("void_tonic", "give_effect", "amp")
				&& T.onServer(w, () -> p.getEffect(MobEffects.RESISTANCE).getAmplifier()) == (int) T.ab("void_tonic", "give_effect", "amp2"),
				"the Void Tonic grants the Strength and Resistance its table names");
		T.onServer(w, () -> p.setHealth(4));
		T.onServer(w, () -> new ItemStack(UsmItems.get("null_elixir")).finishUsingItem(T.level(w), p));
		check(T.onServer(w, () -> p.getHealth()) >= 19f && !T.onServer(w, () -> p.hasEffect(MobEffects.STRENGTH)) && T.onServer(w, () -> p.hasEffect(MobEffects.ABSORPTION)),
				"the Null Elixir cleanses, heals and adds absorption");
		T.onServer(w, () -> p.removeAllEffects());

		// ---- stage two: Solareon's light ------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock solareon");
		ctx.waitTicks(3);
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 20, oy, bz + 0.5);
		Husk h6 = husk(w, bx + 6.5, oy, bz + 0.5);
		Husk h12 = husk(w, bx + 12.5, oy, bz + 0.5);
		Husk h18 = husk(w, bx + 18.5, oy, bz + 0.5);
		Husk aside = husk(w, bx + 8.5, oy, bz + 7.5);
		T.hold(w, "solaris_edge");
		ctx.waitTicks(3);
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(100000 - hp(w, h6) >= 100 && 100000 - hp(w, h12) >= 100 && 100000 - hp(w, h18) >= 100, "the Sunbeam pierces everything on its line (" + (100000 - hp(w, h6)) + "/" + (100000 - hp(w, h12)) + "/" + (100000 - hp(w, h18)) + ")");
		check(hp(w, aside) == 100000f, "and nothing beside it");
		T.takeShot(ctx, "t5-sunbeam");
		for (Husk h : new Husk[] {h6, h12, h18}) {
			T.onServer(w, () -> h.setHealth(100000));
		}
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", bx + 9, oy, bz - 3, bx + 9, oy + 4, bz + 3));
		ready(w);
		ctx.waitTicks(2);
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(100000 - hp(w, h6) >= 100 && hp(w, h12) == 100000f && hp(w, h18) == 100000f, "terrain stops the beam: only the husk in front of the wall is hurt");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", bx + 9, oy, bz - 3, bx + 9, oy + 4, bz + 3));

		// smite: undead take more than animals from the very same hit
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		Husk undead = husk(w, bx + 2.0, oy, bz + 0.5);
		Pig animal = T.onServer(w, () -> {
			Pig pig = new Pig(EntityTypes.PIG, T.level(w));
			pig.setNoAi(true);
			pig.setPersistenceRequired();
			pig.snapTo(bx + 2.0, oy, bz + 4.5, 90, 0);
			T.level(w).addFreshEntity(pig);
			return pig;
		});
		tank(w, animal);
		T.hold(w, "solaris_edge");
		ctx.waitTicks(25);
		float u0 = hp(w, undead);
		T.onServer(w, () -> p.attack(undead));
		float undeadLoss = u0 - hp(w, undead);
		T.stand(w, bx + 0.5, oy, bz + 4.5, bx + 3, oy + 1.2, bz + 4.5);
		ctx.waitTicks(25);
		float pg0 = hp(w, animal);
		T.onServer(w, () -> p.attack(animal));
		float animalLoss = pg0 - hp(w, animal);
		check(undeadLoss > animalLoss * 1.5f, "smite and sear hit the undead much harder than an animal (" + undeadLoss + " vs " + animalLoss + ")");
		check(T.onServer(w, () -> undead.hasEffect(MobEffects.GLOWING)), "searing light leaves the target glowing");
		server.runCommand("kill @e[type=minecraft:husk]");
		server.runCommand("kill @e[type=minecraft:pig]");

		// solar flare: blinds everything around
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk flared = husk(w, bx + 6.5, oy, bz + 0.5);
		Husk unflared = husk(w, bx + 6.5, oy, bz + 20.5);
		T.hold(w, "solaris_edge");
		ready(w);
		ctx.waitTicks(3);
		sneakUse(w);
		ctx.waitTicks(3);
		check(T.onServer(w, () -> flared.hasEffect(MobEffects.BLINDNESS)) && 100000 - hp(w, flared) >= 100, "Solar Flare blinds and burns what is near");
		check(!T.onServer(w, () -> unflared.hasEffect(MobEffects.BLINDNESS)), "but not what is far");
		server.runCommand("kill @e[type=minecraft:husk]");

		// the Seraph set: the halo burns, lights and reveals
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		T.wear(w, "seraph");
		ctx.waitTicks(12);
		check(T.onServer(w, () -> Wearer.has(p, "halo_aura") && Wearer.has(p, "damage_reduction") && Wearer.has(p, "regen_ooc")), "four Seraph pieces grant all three bonuses");
		Husk lit = husk(w, bx + 3.5, oy, bz + 0.5);
		ctx.waitTicks(45);
		check(100000 - hp(w, lit) >= 20 && T.onServer(w, () -> lit.hasEffect(MobEffects.GLOWING)), "the halo burns and reveals enemies near the wearer");
		check(T.onServer(w, () -> LightZones.lit(T.level(w), p.position())), "and the wearer counts as standing in light");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stripArmor(w);

		// the Dawnbreaker's supernova
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.2, bz + 0.5);
		Husk blast = husk(w, bx + 8.5, oy, bz + 0.5);
		Husk safe = husk(w, bx + 0.5, oy, bz + 24.5);
		T.hold(w, "dawnbreaker");
		ctx.waitTicks(3);
		AbilityTests.use(w);
		ctx.waitTicks(4);
		check(T.onServer(w, () -> p.hasEffect(MobEffects.RESISTANCE)), "the caster is shielded while the supernova gathers");
		ctx.waitTicks(30);
		check(100000 - hp(w, blast) >= 120, "the supernova detonates for heavy damage (" + (100000 - hp(w, blast)) + ")");
		check(hp(w, safe) == 100000f, "and spares what stands outside its radius");
		T.takeShot(ctx, "t5-supernova");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.onServer(w, () -> p.removeAllEffects());

		// ---- stage three: the Apex ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock omnarch");
		ctx.waitTicks(3);
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy, bz + 0.5);
		Husk front = husk(w, bx + 6.5, oy, bz + 0.5);
		Husk back = husk(w, bx - 5.5, oy, bz + 0.5);
		Husk side = husk(w, bx + 0.5, oy, bz + 7.5);
		T.hold(w, "apex_blade");
		ready(w);
		ctx.waitTicks(3);
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(100000 - hp(w, front) >= 200, "World Cleave slashes everything in front for heavy damage (" + (100000 - hp(w, front)) + ")");
		check(hp(w, back) == 100000f && hp(w, side) == 100000f, "but not behind the wielder or at right angles to the arc");
		T.takeShot(ctx, "t5-cleave");
		server.runCommand("kill @e[type=minecraft:husk]");

		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy, bz + 0.5);
		T.onServer(w, () -> p.setHealth(5));
		Husk around = husk(w, bx + 12.5, oy, bz + 0.5);
		T.hold(w, "weavers_staff");
		ready(w);
		ctx.waitTicks(3);
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(100000 - hp(w, around) >= 200, "Apex Nova flattens what is within its radius (" + (100000 - hp(w, around)) + ")");
		check(T.onServer(w, () -> p.getHealth()) >= 19f && T.onServer(w, () -> p.hasEffect(MobEffects.ABSORPTION)), "and heals its caster, who gains absorption");
		server.runCommand("kill @e[type=minecraft:husk]");

		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		T.wear(w, "apex");
		ctx.waitTicks(12);
		check(T.onServer(w, () -> Wearer.has(p, "omni_aura") && Wearer.has(p, "quicken") && Wearer.has(p, "damage_reduction")), "four Apex pieces grant all three bonuses");
		check(Math.abs(T.onServer(w, () -> p.getAttributeValue(Attributes.ARMOR)) - T.setArmor("apex")) < 1e-6, "the Apex set's armor points (got " + T.onServer(w, () -> p.getAttributeValue(Attributes.ARMOR)) + ")");
		check(Math.abs(com.ultimatesurvival.armor.Wearer.cdMult(p) - T.bonus("apex", "quicken", "cd_mult")) < 1e-6, "abilities recharge as the Apex set says (x" + com.ultimatesurvival.armor.Wearer.cdMult(p) + ")");
		T.onServer(w, () -> p.setHealth(8));
		Husk fed = husk(w, bx + 3.5, oy, bz + 0.5);
		ctx.waitTicks(45);
		check(100000 - hp(w, fed) >= 60, "the omni aura wears enemies down (" + (100000 - hp(w, fed)) + ")");
		check(T.onServer(w, () -> p.getHealth()) >= 11f, "while the wearer recovers (" + T.onServer(w, () -> p.getHealth()) + " HP)");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stripArmor(w);
		T.onServer(w, () -> list.set(0, new ItemStack(UsmItems.get("heart_of_the_omnarch"))));
		T.onServer(w, () -> list.set(1, new ItemStack(UsmItems.get("apex_signet"))));
		ctx.waitTicks(12);
		check(Math.abs(T.onServer(w, () -> p.getMaxHealth()) - (20.0 + T.attr("heart_of_the_omnarch", "max_health") + T.attr("apex_signet", "max_health"))) < 1e-6, "the Heart of the Omnarch and the Apex Signet add their hearts (" + T.onServer(w, () -> p.getMaxHealth()) + ")");
		T.onServer(w, () -> list.replaceAll(s -> ItemStack.EMPTY));
		ctx.waitTicks(12);

		// ---- the bows ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		for (String bow : new String[] {"rift_bow", "daybreak_bow", "apex_bow"}) {
			T.hold(w, bow);
			ctx.waitTicks(3);
			ItemStack stack = T.onServer(w, () -> p.getMainHandItem());
			T.onServer(w, () -> stack.getItem().releaseUsing(stack, T.level(w), p, 72000 - 20));
			int shots = T.onServer(w, () -> T.level(w).getEntitiesOfClass(com.ultimatesurvival.entity.Bolt.class, p.getBoundingBox().inflate(40, 20, 40)).size());
			check(shots >= 1, bow + " fires energy bolts without ammunition (" + shots + ")");
			server.runCommand("kill @e[type=ultimatesurvival:bolt]");
		}
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(5);
	}
}
