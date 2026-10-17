package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.BossTests.hits;
import static com.ultimatesurvival.test.BossTests.hp;
import static com.ultimatesurvival.test.BossTests.immortal;
import static com.ultimatesurvival.test.BossTests.mortal;
import static com.ultimatesurvival.test.BossTests.shot;
import static com.ultimatesurvival.test.BossTests.spawn;
import static com.ultimatesurvival.test.BossTests.waitRunEnd;
import static com.ultimatesurvival.test.BossTests.waitRunEndFrom;
import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.Ritual;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.progress.Gates;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The four Tier 6 Sovereigns - Boreus (frost), Morbane (plague), Astraeus (cosmos) and Eternus (every colour): their new attacks, phases, shields, rewards and rituals. */
final class Boss4Tests {
	private Boss4Tests() {
	}

	private static boolean part(String name) {
		String only = System.getenv("USM_PART");
		return only == null || only.isEmpty() || java.util.Arrays.asList(only.split(",")).contains(name);
	}

	/** Stands the player dx blocks east(+)/west(-) of the boss on the ground, looking at the boss. */
	private static void at(TestSingleplayerContext w, UsmBoss b, double dx, double groundY) {
		Vec3 bp = T.onServer(w, () -> b.position());
		T.stand(w, bp.x + dx, groundY, bp.z, bp.x, Math.max(groundY + 1.5, bp.y), bp.z);
	}

	private static int count(TestSingleplayerContext w, UsmBoss b, String id) {
		return T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals(id) && e.isAlive()).size());
	}

	private static long hazards(TestSingleplayerContext w, UsmBoss b) {
		return T.onServer(w, () -> (long) b.hazards().size());
	}

	private static double flatDist(TestSingleplayerContext w, UsmBoss b) {
		return T.onServer(w, () -> Math.hypot(T.player(w).getX() - b.getX(), T.player(w).getZ() - b.getZ()));
	}

	/** Spawns a mob of the mod, frozen in place (no AI, no attacks of its own). */
	private static UsmMob still(TestSingleplayerContext w, String id, double x, double y, double z) {
		return T.onServer(w, () -> {
			@SuppressWarnings("unchecked")
			EntityType<UsmMob> type = (EntityType<UsmMob>) Mobs.TYPES.get(id);
			UsmMob m = type.create(T.level(w), EntitySpawnReason.COMMAND);
			m.snapTo(x, y, z, 90f, 0f);
			m.setNoAi(true);
			m.passive = true;
			T.level(w).addFreshEntity(m);
			return m;
		});
	}

	/** Kills the sovereign the way a player would and waits out its death sequence. */
	private static void kill(ClientGameTestContext ctx, TestSingleplayerContext w, UsmBoss b) {
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(170);
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 - 70;
		double y = me.getY();
		double z = me.getZ() + 0.5 - 70;
		T.stand(w, x - 14, y, z, x, y, z);
		ctx.waitTicks(40);

		for (String id : new String[] {"boreus", "morbane", "astraeus", "eternus", "rime_wraith", "rime_anchor", "glacial_brute", "spore_pod", "blight_heart", "blight_crawler", "mycelid",
				"star_sprite", "astral_node", "comet_hound", "loom_spindle"}) {
			check(Mobs.DEFS.containsKey(id) && Mobs.EGGS.containsKey(id), id + " is registered with a spawn egg");
		}
		check(Mobs.DEFS.get("boreus").phases.size() == 4 && Mobs.DEFS.get("morbane").phases.size() == 4 && Mobs.DEFS.get("astraeus").phases.size() == 4
				&& Mobs.DEFS.get("eternus").phases.size() == 5, "Boreus, Morbane and Astraeus have four phases, Eternus five");
		check(com.ultimatesurvival.mob.BossRush.ORDER.size() == 11, "the Boss Rush now calls eleven Sovereigns");
		if (part("gallery")) {
			gallery(ctx, w, x, y, z);
		}
		if (part("boreus")) {
			boreus(ctx, w, x, y, z);
		}
		if (part("morbane")) {
			morbane(ctx, w, x, y, z);
		}
		if (part("astraeus")) {
			astraeus(ctx, w, x, y, z);
		}
		if (part("eternus")) {
			eternus(ctx, w, x, y, z);
		}
		mortal(w);
	}

	// =================================================================================================================================
	private static void gallery(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		for (String id : new String[] {"boreus", "morbane", "astraeus", "eternus"}) {
			UsmBoss b = spawn(w, id, x, y, z);
			ctx.waitTicks(12);
			double height = T.onServer(w, () -> b.getBbHeight());
			Vec3 bp = T.onServer(w, () -> b.position());
			T.stand(w, bp.x - 24, y, bp.z, bp.x, bp.y + height * 0.5, bp.z);
			ctx.waitTicks(10);
			T.takeShot(ctx, "boss4-gallery-" + id);
			T.stand(w, bp.x - 10, y, bp.z - 8, bp.x, bp.y + height * 0.55, bp.z);
			ctx.waitTicks(5);
			T.takeShot(ctx, "boss4-gallery-" + id + "-close");
			T.onServer(w, () -> b.discard());
			ctx.waitTicks(3);
		}
		String[] minions = {"rime_wraith", "rime_anchor", "glacial_brute", "spore_pod", "blight_heart", "blight_crawler", "mycelid", "star_sprite", "astral_node", "comet_hound", "loom_spindle"};
		for (int i = 0; i < minions.length; i++) {
			still(w, minions[i], x - 12 + (i - minions.length / 2) * 4.2, y, z - 6);
		}
		T.stand(w, x - 12, y, z + 11, x - 12, y + 1.5, z - 6);
		ctx.waitTicks(25);
		T.takeShot(ctx, "boss4-gallery-creatures");
		w.getServer().runCommand("kill @e[type=!minecraft:player]");
		ctx.waitTicks(3);
	}

	// =================================================================================================================================
	private static void boreus(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock omnarch");
		UsmBoss b = spawn(w, "boreus", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 11000f, "Boreus has 11000 HP");
		check(!T.onServer(w, () -> b.def().fly), "Boreus walks");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- rime spikes: stand in a gap and nothing touches you; stand on a line and it freezes you -----------------------------------------------------------
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("icicles"));
		waitRunEndFrom(ctx, w, b, 50, 300, () -> shot(ctx, w, "boss4-1-icicles"));
		check(hits(w, b) == h0, "standing in a gap between the lines is safe (hits " + h0 + " -> " + hits(w, b) + ")");
		at(w, b, -14, y);
		ctx.waitTicks(4);
		int h1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("icicles"));
		ctx.waitTicks(3);
		Vec3 bp = T.onServer(w, () -> b.position());
		double base = Math.PI + Math.PI / 6;                                  // the first line is half a step off the bearing to the target
		T.stand(w, bp.x + 14 * Math.cos(base), y, bp.z + 14 * Math.sin(base), bp.x, y, bp.z);
		waitRunEnd(ctx, w, b, 300);
		check(hits(w, b) > h1, "standing on a line is not: the spikes find the player (hits " + h1 + " -> " + hits(w, b) + ")");
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.SLOWNESS)), "and the spikes slow whoever they hit");

		// ---- whiteout: stillness is punished, motion is not --------------------------------------------------------------------------------------------------------------
		at(w, b, -10, y);
		immortal(w);
		ctx.waitTicks(4);
		int w0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("whiteout"));
		ctx.waitTicks(100);
		check(hits(w, b) > w0, "standing still in the whiteout gets the player bitten (hits " + w0 + " -> " + hits(w, b) + ")");
		check(T.onServer(w, () -> T.player(w).getTicksFrozen()) > 0, "and frosts the screen");
		shot(ctx, w, "boss4-2-whiteout");
		waitRunEnd(ctx, w, b, 400);
		T.onServer(w, () -> T.player(w).setTicksFrozen(0));
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.SLOWNESS));
		at(w, b, -10, y);
		ctx.waitTicks(4);
		int w1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("whiteout"));
		Vec3 bq = T.onServer(w, () -> b.position());
		for (int i = 0; i < 100; i++) {
			ctx.waitTicks(3);
			T.stand(w, bq.x - 8 - (i * 5 % 11), y, bq.z, bq.x, y, bq.z);
		}
		waitRunEnd(ctx, w, b, 400);
		check(hits(w, b) == w1, "a player who keeps moving is never touched by the whiteout (hits " + w1 + " -> " + hits(w, b) + ")");

		// ---- deep freeze: roots whoever it catches, then spears fall on them ----------------------------------------------------------------------------------------------
		T.onServer(w, () -> T.player(w).setTicksFrozen(0));
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.SLOWNESS));
		at(w, b, -6, y);
		immortal(w);
		ctx.waitTicks(5);
		int g0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("glaciate"));
		ctx.waitTicks(40);
		check(hits(w, b) > g0 && T.onServer(w, () -> T.player(w).hasEffect(MobEffects.SLOWNESS)), "the deep freeze catches and roots the player (hits " + g0 + " -> " + hits(w, b) + ")");
		int g1 = hits(w, b);
		shot(ctx, w, "boss4-3-glaciate");
		waitRunEnd(ctx, w, b, 300);
		check(hits(w, b) >= g1 + 3, "and three spears of ice fall on the rooted player (hits " + g1 + " -> " + hits(w, b) + ")");

		// ---- phases, anchors ----------------------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.SLOWNESS));
		at(w, b, -12, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Blizzard) begins at 72%");
		ctx.waitTicks(95);
		T.onServer(w, () -> b.hazards().clear());
		server.runCommand("kill @e[type=ultimatesurvival:rime_wraith]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Permafrost) begins at 45%");
		ctx.waitTicks(90);
		check(count(w, b, "rime_anchor") == 3, "three Rime Anchors rise (" + count(w, b, "rime_anchor") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "Boreus is shielded while they stand");
		float s0 = hp(w, b);
		check(!T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 100f)) && hp(w, b) == s0, "a shielded Boreus takes no damage");
		shot(ctx, w, "boss4-4-anchors");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("rime_anchor"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 100000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "breaking every anchor drops the shield and staggers it");
		T.onServer(w, () -> b.hazards().clear());
		server.runCommand("kill @e[type=ultimatesurvival:rime_wraith]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.10f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 3, "the last phase (Absolute Zero) begins at 18%");
		ctx.waitTicks(125);
		check(hazards(w, b) >= 8, "frozen pools spread across the floor as it begins (" + hazards(w, b) + ")");

		// ---- death rewards + ritual -----------------------------------------------------------------------------------------------------------------------------------------------------
		check(!Gates.isCleared("boreus"), "Boreus's gate is closed");
		kill(ctx, w, b);
		check(Gates.isCleared("boreus") && Gates.currentTier() == 6, "killing Boreus opens its gate: tier six");
		server.runCommand("clear @a");
		T.hold(w, "boreus_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "frost_core") >= 10 && T.count(w, "heart_of_boreus") == 1 && T.count(w, "eternium_ingot") >= 16,
				"the hoard holds Frost Cores, Eternium and Boreus's heart (" + T.count(w, "frost_core") + ")");
		server.runCommand("clear @a");
		ritual(ctx, w, x, y, z, "boreus", "rime_sigil", new ItemStack(Items.BLUE_ICE, 16), 30, 0, "boss4-5-boreus-emerges");
		server.runCommand("usm reset");
	}

	/** Checks the ritual's cost one item at a time, pays it, summons the boss and takes a picture of its emergence. */
	private static void ritual(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z, String id, String sigilId, ItemStack cost, int levels, int hearts, String shotName) {
		var server = w.getServer();
		boolean was = UsmConfig.get().ritualsAnywhere;
		UsmConfig.get().ritualsAnywhere = true;
		BlockPos altar = BlockPos.containing(x + 14, y, z + 14);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		ctx.waitTicks(3);
		MobDef def = Mobs.DEFS.get(id);
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, id + "'s altar refuses a player without its offering");
		T.onServer(w, () -> p.getInventory().add(cost.copy()));
		T.onServer(w, () -> p.giveExperienceLevels(levels));
		if (hearts > 0) {                                                                       // the cost in hearts needs more health than a fresh player has
			T.onServer(w, () -> p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100));
			T.onServer(w, () -> p.setHealth(100));
		}
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "the offering and the experience are enough");
		ItemStack sigil = new ItemStack(UsmItems.get(sigilId));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.count(w, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cost.getItem()).toString()) == 0, "the ritual consumes the offering");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(14), e -> e.def().id.equals(id)).size()) == 1, "the " + sigilId + " summons " + id);
		ctx.waitTicks(14);
		T.takeShot(ctx, shotName);
		server.runCommand("kill @e[type=ultimatesurvival:" + id + "]");
		T.onServer(w, () -> p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20));
		UsmConfig.get().ritualsAnywhere = was;
	}

	// =================================================================================================================================
	private static void morbane(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock boreus");
		UsmBoss b = spawn(w, "morbane", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 15500f, "Morbane has 15500 HP");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- spores: pods arc over and leave clouds ----------------------------------------------------------------------------------------------------------------------------------------
		int s0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("spores"));
		ctx.waitTicks(30 + 20 + 28);
		shot(ctx, w, "boss4-6-spores");
		waitRunEnd(ctx, w, b, 300);
		check(hazards(w, b) == 5, "five poison clouds linger where the pods landed (" + hazards(w, b) + ")");
		check(hits(w, b) > s0, "the first pod lands where the player stood (hits " + s0 + " -> " + hits(w, b) + ")");
		T.onServer(w, () -> b.hazards().clear());

		// ---- infestation: the player is marked; water washes it off, otherwise it bursts and calls pods ---------------------------------------------------------------
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);
		int i0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("infest"));
		ctx.waitTicks(30);
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.POISON)), "the infestation poisons the player");
		shot(ctx, w, "boss4-7-infest");
		waitRunEnd(ctx, w, b, 400);
		ctx.waitTicks(4);
		check(hits(w, b) > i0, "the uncured infection bursts and hurts (hits " + i0 + " -> " + hits(w, b) + ")");
		check(count(w, b, "spore_pod") >= 2, "and calls spore pods (" + count(w, b, "spore_pod") + ")");
		server.runCommand("kill @e[type=ultimatesurvival:spore_pod]");
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.POISON));

		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);
		int i1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("infest"));
		ctx.waitTicks(40);
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:water", (int) Math.floor(pp.x) - 1, (int) Math.floor(pp.y), (int) Math.floor(pp.z) - 1,
				(int) Math.floor(pp.x) + 1, (int) Math.floor(pp.y) + 1, (int) Math.floor(pp.z) + 1));
		ctx.waitTicks(6);
		check(!T.onServer(w, () -> T.player(w).hasEffect(MobEffects.POISON)), "stepping into water washes the infection off");
		waitRunEnd(ctx, w, b, 400);
		ctx.waitTicks(4);
		check(hits(w, b) == i1 && count(w, b, "spore_pod") == 0, "and then nothing bursts (hits " + i1 + " -> " + hits(w, b) + ")");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", (int) Math.floor(pp.x) - 1, (int) Math.floor(pp.y), (int) Math.floor(pp.z) - 1,
				(int) Math.floor(pp.x) + 1, (int) Math.floor(pp.y) + 1, (int) Math.floor(pp.z) + 1));

		// ---- phases, hearts -------------------------------------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Rot) begins at 72%");
		ctx.waitTicks(95);
		server.runCommand("kill @e[type=ultimatesurvival:spore_pod]");
		server.runCommand("kill @e[type=ultimatesurvival:blight_crawler]");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Pandemic) begins at 45%");
		ctx.waitTicks(90);
		check(count(w, b, "blight_heart") == 3, "three Blight Hearts rise (" + count(w, b, "blight_heart") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "Morbane is shielded while they beat");
		shot(ctx, w, "boss4-8-hearts");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("blight_heart"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 100000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "stopping every heart drops the shield and staggers it");
		server.runCommand("kill @e[type=ultimatesurvival:spore_pod]");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.10f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 3, "the last phase (Extinction) begins at 18%");

		check(!Gates.isCleared("morbane"), "Morbane's gate is closed");
		kill(ctx, w, b);
		check(Gates.isCleared("morbane"), "killing Morbane opens its gate");
		server.runCommand("clear @a");
		T.hold(w, "morbane_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "blight_core") >= 10 && T.count(w, "heart_of_morbane") == 1, "the hoard holds Blight Cores and Morbane's heart (" + T.count(w, "blight_core") + ")");
		server.runCommand("clear @a");
		ritual(ctx, w, x, y, z, "morbane", "blight_sigil", new ItemStack(Items.FERMENTED_SPIDER_EYE, 8), 0, 6, "boss4-9-morbane-emerges");
		server.runCommand("usm reset");
	}

	// =================================================================================================================================
	private static void astraeus(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock morbane");
		UsmBoss b = spawn(w, "astraeus", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 21000f, "Astraeus has 21000 HP");
		check(T.onServer(w, () -> b.def().fly && b.isNoGravity()), "Astraeus floats");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -10, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- planets: worlds sweep outward; the pocket close to the boss is safe, the ring is not ------------------------------------------------------------------------------
		int p0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("planets"));
		ctx.waitTicks(70);
		shot(ctx, w, "boss4-10-planets");
		waitRunEnd(ctx, w, b, 400);
		check(hits(w, b) > p0, "a planet sweeps across a player standing in its path (hits " + p0 + " -> " + hits(w, b) + ")");
		at(w, b, -2.6, y);
		immortal(w);
		ctx.waitTicks(5);
		int p1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("planets"));
		waitRunEnd(ctx, w, b, 400);
		check(hits(w, b) == p1, "close to the boss, inside the ring, nothing touches the player (hits " + p1 + " -> " + hits(w, b) + ")");

		// ---- constellation: lines of light between stars; a cell is safe, a line is not ------------------------------------------------------------------------------------------
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);
		int c0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("constellation"));
		ctx.waitTicks(3);
		Vec3 bp = T.onServer(w, () -> b.position());
		Vec3 centre = new Vec3(bp.x, y, bp.z);
		T.stand(w, centre.x - 2.0, y, centre.z, bp.x, y, bp.z);                                              // the middle of the web
		waitRunEndFrom(ctx, w, b, 60, 400, () -> shot(ctx, w, "boss4-11-constellation"));
		check(hits(w, b) == c0, "the middle of the web is safe (hits " + c0 + " -> " + hits(w, b) + ")");
		at(w, b, -14, y);
		ctx.waitTicks(5);
		int c1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("constellation"));
		ctx.waitTicks(3);
		int n = 7;                                                                                           // phase two's web: seven stars joined three apart
		double phase = Math.PI + Math.PI / n;
		double skip = 3;
		double mid = phase + skip * Math.PI / n;                                                             // the middle of the first line: between star 0 and star 3
		double rad = 19 * Math.cos(skip * Math.PI / n);
		Vec3 b2 = T.onServer(w, () -> b.position());
		T.stand(w, b2.x + rad * Math.cos(mid), y, b2.z + rad * Math.sin(mid), b2.x, y, b2.z);
		waitRunEnd(ctx, w, b, 400);
		check(hits(w, b) > c1, "a player standing on a line of light is cut by it (hits " + c1 + " -> " + hits(w, b) + ")");

		// ---- phases, nodes --------------------------------------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Night) begins at 72%");
		ctx.waitTicks(95);
		T.onServer(w, () -> b.hazards().clear());
		server.runCommand("kill @e[type=ultimatesurvival:star_sprite]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Nova) begins at 45%");
		ctx.waitTicks(90);
		check(count(w, b, "astral_node") == 4, "four Astral Nodes rise (" + count(w, b, "astral_node") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "Astraeus is shielded while they shine");
		shot(ctx, w, "boss4-12-nodes");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("astral_node"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 100000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "putting every node out drops the shield and staggers it");
		server.runCommand("kill @e[type=ultimatesurvival:star_sprite]");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.10f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 3, "the last phase (Heat Death) begins at 18%");

		check(!Gates.isCleared("astraeus"), "Astraeus's gate is closed");
		kill(ctx, w, b);
		check(Gates.isCleared("astraeus"), "killing Astraeus opens its gate");
		server.runCommand("clear @a");
		T.hold(w, "astraeus_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "star_core") >= 10 && T.count(w, "heart_of_astraeus") == 1, "the hoard holds Star Cores and Astraeus's heart (" + T.count(w, "star_core") + ")");
		server.runCommand("clear @a");
		ritual(ctx, w, x, y, z, "astraeus", "astral_sigil", new ItemStack(Items.END_CRYSTAL, 2), 40, 0, "boss4-13-astraeus-emerges");
		server.runCommand("usm reset");
	}

	// =================================================================================================================================
	private static void eternus(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock astraeus");
		UsmBoss b = spawn(w, "eternus", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 34000f, "Eternus has 34000 HP");
		check(T.onServer(w, () -> b.def().fly && b.isNoGravity()), "Eternus floats");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -22, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- threads: they reach everyone, haul them in and burst - unless something stands in the way -------------------------------------------------------------------------
		double d0 = flatDist(w, b);
		int t0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("tether"));
		ctx.waitTicks(30);
		shot(ctx, w, "boss4-14-tether");
		waitRunEnd(ctx, w, b, 300);
		double d1 = flatDist(w, b);
		check(d1 < d0 - 8, "the threads haul the player toward Eternus (" + String.format("%.1f -> %.1f", d0, d1) + ")");
		check(hits(w, b) > t0, "and the burst at the end hurts (hits " + t0 + " -> " + hits(w, b) + "; player " + T.onServer(w, () -> T.player(w).position()) + ", boss " + T.onServer(w, () -> b.position()) + ")");
		at(w, b, -22, y);
		immortal(w);
		ctx.waitTicks(5);
		Vec3 pl = T.onServer(w, () -> T.player(w).position());
		Vec3 bp = T.onServer(w, () -> b.position());
		int wx = (int) Math.floor(pl.x) + 3;
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", wx, (int) y, (int) pl.z - 6, wx, (int) y + 14, (int) pl.z + 6));
		ctx.waitTicks(3);
		double d2 = flatDist(w, b);
		int t1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("tether"));
		waitRunEnd(ctx, w, b, 300);
		check(Math.abs(flatDist(w, b) - d2) < 1.5 && hits(w, b) == t1, "behind a wall no thread can take hold (" + String.format("%.1f -> %.1f", d2, flatDist(w, b)) + ")");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", wx, (int) y, (int) pl.z - 6, wx, (int) y + 14, (int) pl.z + 6));

		// ---- five phases, each wearing another thread ---------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -14, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.70f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Blood and Time) begins at 78%");
		ctx.waitTicks(95);
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.50f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Void and Light) begins at 55%");
		ctx.waitTicks(95);
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.25f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 3, "phase four (Winter, Rot and the Stars) begins at 32%");
		check(T.onServer(w, () -> b.def().phases.get(3).attacks().stream().map(a -> a.type()).collect(java.util.stream.Collectors.toSet()))
				.containsAll(java.util.Set.of("icicles", "glaciate", "spores", "planets", "constellation")), "it fights with winter, rot and the stars at once");
		ctx.waitTicks(95);
		T.onServer(w, () -> b.hazards().clear());
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.08f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 4, "the last phase (The Unwoven) begins at 12%");
		ctx.waitTicks(100);
		check(count(w, b, "loom_spindle") == 4, "four Loom Spindles rise (" + count(w, b, "loom_spindle") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "Eternus is shielded while they hold the thread");
		shot(ctx, w, "boss4-15-spindles");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("loom_spindle"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 100000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "cutting every spindle drops the shield and staggers it");

		check(!Gates.isCleared("eternus"), "Eternus's gate is closed");
		kill(ctx, w, b);
		check(Gates.isCleared("eternus") && Gates.currentTier() == 6, "killing Eternus opens the last gate");
		server.runCommand("clear @a");
		T.hold(w, "eternus_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "weave_core") >= 12 && T.count(w, "heart_of_eternus") == 1, "the hoard holds Weave Cores and Eternus's heart (" + T.count(w, "weave_core") + ")");
		server.runCommand("clear @a");
		ritual(ctx, w, x, y, z, "eternus", "thread_sigil", new ItemStack(Items.NETHER_STAR, 2), 50, 10, "boss4-16-eternus-emerges");
		server.runCommand("usm reset");
	}
}
