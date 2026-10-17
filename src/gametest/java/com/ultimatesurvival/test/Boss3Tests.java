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

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.Ritual;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.util.LightZones;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The three Tier 5 Sovereigns: Nihilus (void), Solareon (light) and the Omnarch (everything) - every new mechanic, their loot and rituals. */
final class Boss3Tests {
	private Boss3Tests() {
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

	private static long hazards(TestSingleplayerContext w, UsmBoss b, String type) {
		return T.onServer(w, () -> b.hazards().stream().filter(h -> h.getClass().getSimpleName().equals(type)).count());
	}

	/** A stone wall (9 wide, 12 tall) between player and boss, three blocks in front of the player (the boss is east of the player). */
	private static void wall(TestSingleplayerContext w, double y, boolean on, Vec3 player) {
		int wx = (int) Math.floor(player.x) + 3;
		w.getServer().runCommand(String.format("fill %d %d %d %d %d %d %s", wx, (int) y, (int) player.z - 5, wx, (int) y + 12, (int) player.z + 5,
				on ? "minecraft:stone" : "minecraft:air"));
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
		double z = me.getZ() + 0.5 + 70;
		T.stand(w, x - 14, y, z, x, y, z);
		ctx.waitTicks(40);

		for (String id : new String[] {"nihilus", "solareon", "omnarch", "void_spawn", "null_anchor", "halo_blade", "apex_pylon", "void_stalker", "rift_weaver",
				"radiant_sentinel", "prism_drone"}) {
			check(Mobs.DEFS.containsKey(id) && Mobs.EGGS.containsKey(id), id + " is registered with a spawn egg");
		}
		check(Mobs.DEFS.get("omnarch").phases.size() == 4 && Mobs.DEFS.get("nihilus").phases.size() == 3, "the Omnarch has four phases, Nihilus three");
		if (part("gallery")) {
			gallery(ctx, w, x, y, z);
		}
		if (part("nihilus")) {
			nihilus(ctx, w, x, y, z);
		}
		if (part("solareon")) {
			solareon(ctx, w, x, y, z);
		}
		if (part("omnarch")) {
			omnarch(ctx, w, x, y, z);
		}
		mortal(w);
	}

	// =================================================================================================================================
	private static void gallery(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		for (String id : new String[] {"nihilus", "solareon", "omnarch"}) {
			UsmBoss b = spawn(w, id, x, y, z);
			ctx.waitTicks(12);
			double height = T.onServer(w, () -> b.getBbHeight());
			Vec3 bp = T.onServer(w, () -> b.position());
			T.stand(w, bp.x - 22, y, bp.z, bp.x, bp.y + height * 0.5, bp.z);
			ctx.waitTicks(10);
			T.takeShot(ctx, "boss3-gallery-" + id);
			T.stand(w, bp.x - 9, y, bp.z - 9, bp.x, bp.y + height * 0.55, bp.z);
			ctx.waitTicks(5);
			T.takeShot(ctx, "boss3-gallery-" + id + "-close");
			T.onServer(w, () -> b.discard());
			ctx.waitTicks(3);
		}
	}

	// =================================================================================================================================
	private static void nihilus(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock chronos");
		UsmBoss b = spawn(w, "nihilus", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 3400f, "Nihilus has 3400 HP");
		check(T.onServer(w, () -> b.def().fly && b.isNoGravity()), "Nihilus floats");
		double bbh = T.onServer(w, () -> b.getBbHeight());
		check(Math.abs(bbh - 3.4 * 1.5) < 0.2, "Nihilus is half again as tall as its model's base size (" + String.format("%.2f", bbh) + " blocks)");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -16, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- black hole: opens where you stood, burns the core, drags what is near, then collapses -----------------------------------------------------------------------
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("blackhole"));
		ctx.waitTicks(3);
		for (int i = 0; i < 60 && hazards(w, b, "Singularity") == 0; i++) {
			ctx.waitTicks(1);
		}
		check(hazards(w, b, "Singularity") == 1, "a black hole opens");
		ctx.waitTicks(24);
		check(hits(w, b) > h0, "its core burns whoever stands in it (hits " + h0 + " -> " + hits(w, b) + ")");
		shot(ctx, w, "boss3-1-blackhole");
		Vec3 hole = T.onServer(w, () -> b.hazards().get(0).pos);
		T.stand(w, hole.x + 7, y, hole.z, hole.x, y, hole.z);
		ctx.waitTicks(2);
		double d0 = T.onServer(w, () -> Math.hypot(T.player(w).getX() - hole.x, T.player(w).getZ() - hole.z));
		ctx.waitTicks(25);
		double d1 = T.onServer(w, () -> Math.hypot(T.player(w).getX() - hole.x, T.player(w).getZ() - hole.z));
		check(d1 < d0 - 1.2, "the singularity drags the player toward its core (" + String.format("%.1f -> %.1f", d0, d1) + ")");
		T.stand(w, hole.x + 30, y, hole.z, hole.x, y, hole.z);
		ctx.waitTicks(3);
		Vec3 far = T.onServer(w, () -> T.player(w).position());
		ctx.waitTicks(20);
		check(T.onServer(w, () -> T.player(w).position().distanceTo(far)) < 0.2, "beyond its reach nothing pulls");
		for (int i = 0; i < 160 && hazards(w, b, "Singularity") > 0; i++) {
			ctx.waitTicks(1);
		}
		check(hazards(w, b, "Singularity") == 0, "the black hole collapses and is gone");
		check(T.onServer(w, () -> b.def().phases.get(0).attacks().stream().anyMatch(a -> a.type().equals("blackhole"))), "black holes belong to Nihilus's first phase");

		// ---- rifts: each opens a door, releases a void spawn, and stays open while it lives -----------------------------------------------------------------------------------------
		at(w, b, -14, y);
		ctx.waitTicks(3);
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.forceAttack("rifts"));
		waitRunEnd(ctx, w, b, 160);
		ctx.waitTicks(4);
		check(count(w, b, "void_spawn") == 3, "three rifts released three void spawn (" + count(w, b, "void_spawn") + ")");
		check(T.onServer(w, () -> b.hazards().size()) == 3, "and each rift burns on the floor");
		shot(ctx, w, "boss3-2-rifts");
		T.onServer(w, () -> {
			var spawns = T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("void_spawn"));
			spawns.get(0).kill(T.level(w));
		});
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.hazards().size()) == 2, "killing what came out closes its rift");
		server.runCommand("kill @e[type=ultimatesurvival:void_spawn]");
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.hazards().isEmpty()), "and the rest close with their spawn");

		// ---- eclipse: darkness rots anyone who is not standing in light ----------------------------------------------------------------------------------------------------------------
		at(w, b, -10, y);
		immortal(w);
		ctx.waitTicks(3);
		int g0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("gloom"));
		ctx.waitTicks(45);
		check(hazards(w, b, "GloomField") == 1, "an eclipse dome rises");
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.DARKNESS)), "the dome darkens the player's screen");
		ctx.waitTicks(30);
		check(hits(w, b) > g0, "standing in the dark rots the player (hits " + g0 + " -> " + hits(w, b) + ")");
		shot(ctx, w, "boss3-3-eclipse");
		check(T.onServer(w, () -> !b.def().phases.get(0).attacks().stream().anyMatch(a -> a.type().equals("gloom"))) && !T.onServer(w, () -> b.def().phases.get(1).enter().isEmpty()),
				"the eclipse begins as phase two opens");
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		server.runCommand(String.format("setblock %d %d %d minecraft:glowstone", (int) Math.floor(pp.x) + 1, (int) Math.floor(pp.y), (int) Math.floor(pp.z)));
		ctx.waitTicks(10);
		int g1 = hits(w, b);
		ctx.waitTicks(60);
		check(hits(w, b) == g1, "a light source beside the player keeps the rot away");
		server.runCommand(String.format("setblock %d %d %d minecraft:air", (int) Math.floor(pp.x) + 1, (int) Math.floor(pp.y), (int) Math.floor(pp.z)));
		ctx.waitTicks(10);
		int g2 = hits(w, b);
		ctx.waitTicks(30);
		check(hits(w, b) > g2, "without it the dark gets the player again");
		server.runCommand("clear @a");
		ServerPlayer pl = T.onServer(w, () -> T.player(w));
		check(Gates.isCleared("chronos") && UsmItems.get("lumen_flare") != null, "the Lumen Flare is available once Chronos has fallen");
		int g3 = hits(w, b);
		T.onServer(w, () -> new ItemStack(UsmItems.get("lumen_flare")).finishUsingItem(T.level(w), pl));
		ctx.waitTicks(10);
		check(T.onServer(w, () -> LightZones.lit(T.level(w), pl.position())), "a Lumen Flare lights the place where it was cracked");
		ctx.waitTicks(60);
		check(hits(w, b) == g3, "and the eclipse cannot touch anyone inside it");
		check(T.onServer(w, () -> pl.hasEffect(MobEffects.NIGHT_VISION)), "the flare also grants Night Vision");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> pl.removeEffect(MobEffects.DARKNESS));
		LightZones.clear();

		// ---- the cage: bars close on a player who stays, harmless to one who leaves ---------------------------------------------------------------------------------------------------
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(4);
		Vec3 centre = T.onServer(w, () -> T.player(w).position());
		int c0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("cage"));
		waitRunEndFrom(ctx, w, b, 70, 220, () -> shot(ctx, w, "boss3-4-cage"));
		check(hits(w, b) > c0, "the closing cage hurts a player who stays inside (hits " + c0 + " -> " + hits(w, b) + ")");
		at(w, b, -14, y);
		ctx.waitTicks(4);
		int c1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("cage"));
		ctx.waitTicks(4);
		T.stand(w, centre.x + 24, y, centre.z, centre.x + 30, y, centre.z);
		waitRunEnd(ctx, w, b, 260);
		check(hits(w, b) == c1, "a player who walks away from the cage is not touched (hits " + c1 + " -> " + hits(w, b) + ")");

		// ---- phases, pylons -----------------------------------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Eclipse) begins at 66%");
		ctx.waitTicks(90);
		check(hazards(w, b, "GloomField") == 1, "and the eclipse dome comes with it");
		T.onServer(w, () -> b.hazards().clear());
		server.runCommand("kill @e[type=ultimatesurvival:void_spawn]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Oblivion) begins at 33%");
		ctx.waitTicks(80);
		check(count(w, b, "null_anchor") == 3, "three Null Anchors rise (" + count(w, b, "null_anchor") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "Nihilus is shielded while they stand");
		float s0 = hp(w, b);
		check(!T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 100f)) && hp(w, b) == s0, "a shielded Nihilus takes no damage");
		shot(ctx, w, "boss3-5-anchors");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("null_anchor"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 1000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "breaking every anchor drops the shield and staggers it");

		// ---- death rewards + ritual -----------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:void_spawn]");
		check(!Gates.isCleared("nihilus"), "Nihilus's gate is closed");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(150);
		check(Gates.isCleared("nihilus") && Gates.currentTier() == 5, "killing Nihilus opens its gate: tier five");
		server.runCommand("clear @a");
		T.hold(w, "nihilus_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "void_core") >= 8 && T.count(w, "heart_of_nihilus") == 1, "the hoard holds Void Cores and Nihilus's heart (" + T.count(w, "void_core") + ")");
		server.runCommand("clear @a");

		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 14, y, z + 14);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		ctx.waitTicks(3);
		MobDef def = Mobs.DEFS.get("nihilus");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "Nihilus's altar answers only in the End");
		UsmConfig.get().ritualsAnywhere = true;
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "and refuses a player without four Eyes of Ender");
		T.onServer(w, () -> p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.ENDER_EYE, 4)));
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "four Eyes of Ender are enough");
		ItemStack sigil = new ItemStack(UsmItems.get("void_sigil"));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.count(w, "minecraft:ender_eye") == 0, "the ritual consumes the four Eyes of Ender");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(12), e -> e.def().id.equals("nihilus")).size()) == 1, "the Void Sigil summons Nihilus");
		ctx.waitTicks(12);
		T.takeShot(ctx, "boss3-6-nihilus-emerges");
		server.runCommand("kill @e[type=ultimatesurvival:nihilus]");
		UsmConfig.get().ritualsAnywhere = false;
		server.runCommand("usm reset");
	}

	// =================================================================================================================================
	private static void solareon(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock nihilus");
		UsmBoss b = spawn(w, "solareon", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 4800f, "Solareon has 4800 HP");
		check(T.onServer(w, () -> b.def().fly), "Solareon flies");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -16, y);
		immortal(w);
		ctx.waitTicks(5);

		// ---- the beam sweeps across the player; terrain stops it --------------------------------------------------------------------------------------------------------------------
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("beam"));
		waitRunEndFrom(ctx, w, b, 70, 300, () -> shot(ctx, w, "boss3-7-beam"));
		check(hits(w, b) > h0, "the sweeping beam crossed the player (hits " + h0 + " -> " + hits(w, b) + ")");
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		wall(w, y, true, pp);
		ctx.waitTicks(3);
		int h1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("beam"));
		waitRunEnd(ctx, w, b, 300);
		check(hits(w, b) == h1, "behind a wall the beam never reaches the player (hits " + h1 + " -> " + hits(w, b) + ")");

		// ---- supernova: only distance or cover saves you; it exhausts the caster -------------------------------------------------------------------------------------------------------
		int s0 = hits(w, b);
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.BLINDNESS));
		T.onServer(w, () -> b.forceAttack("supernova"));
		waitRunEndFrom(ctx, w, b, 95, 400, () -> {
			check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "the spent star leaves Solareon exhausted and vulnerable");
			check(hits(w, b) == s0 && !T.onServer(w, () -> T.player(w).hasEffect(MobEffects.BLINDNESS)), "a supernova cannot burn what it cannot see");
			shot(ctx, w, "boss3-8-supernova");
		});
		wall(w, y, false, pp);
		ctx.waitTicks(3);
		int s1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("supernova"));
		waitRunEndFrom(ctx, w, b, 95, 400, () -> check(hits(w, b) > s1 && T.onServer(w, () -> T.player(w).hasEffect(MobEffects.BLINDNESS)), "in the open it burns and blinds"));
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.BLINDNESS));

		// ---- flash: only those looking at it are caught --------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -16, y);
		ctx.waitTicks(4);
		T.onServer(w, () -> b.forceAttack("flash"));
		waitRunEnd(ctx, w, b, 160);
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.BLINDNESS)), "looking at the flash blinds the player");
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.BLINDNESS));
		Vec3 fp = T.onServer(w, () -> T.player(w).position());
		T.stand(w, fp.x, y, fp.z, fp.x - 30, y, fp.z);
		ctx.waitTicks(4);
		T.onServer(w, () -> b.forceAttack("flash"));
		waitRunEnd(ctx, w, b, 160);
		check(!T.onServer(w, () -> T.player(w).hasEffect(MobEffects.BLINDNESS)), "looking away spares them");

		// ---- sunder: a crack races along the floor toward where you stood, then burns on ------------------------------------------------------------------------------------------------
		at(w, b, -16, y);
		T.onServer(w, () -> b.hazards().clear());
		ctx.waitTicks(4);
		int u0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("sunder"));
		waitRunEndFrom(ctx, w, b, 52, 200, () -> shot(ctx, w, "boss3-9-sunder"));
		check(hits(w, b) > u0, "the crack runs through a player on its line (hits " + u0 + " -> " + hits(w, b) + ")");
		check(T.onServer(w, () -> b.hazards().size()) >= 5, "and leaves burning ground along its length (" + T.onServer(w, () -> b.hazards().size()) + " patches)");
		T.onServer(w, () -> b.hazards().clear());
		at(w, b, -16, y);
		ctx.waitTicks(4);
		Vec3 line = T.onServer(w, () -> T.player(w).position());
		int u1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("sunder"));
		ctx.waitTicks(4);
		T.stand(w, line.x, y, line.z + 12, line.x + 5, y, line.z + 12);
		waitRunEnd(ctx, w, b, 200);
		check(hits(w, b) == u1, "a player who steps off the line is not touched (hits " + u1 + " -> " + hits(w, b) + ")");
		T.onServer(w, () -> b.hazards().clear());

		// ---- blades, mirror -------------------------------------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		ctx.waitTicks(4);
		T.onServer(w, () -> b.forceAttack("summon"));
		waitRunEnd(ctx, w, b, 120);
		check(count(w, b, "halo_blade") >= 2, "Solareon summons Halo Blades (" + count(w, b, "halo_blade") + ")");
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		Vec3 bp2 = T.onServer(w, () -> b.position());
		Husk attacker = AbilityTests.spawn(w, bp2.x + 2.5, y, bp2.z, 90);
		T.onServer(w, () -> b.forceAttack("reflect"));
		ctx.waitTicks(40);
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_SHIELD)), "Solareon takes its mirror stance");
		float r0 = hp(w, b);
		check(!T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().mobAttack(attacker), 40f)) && hp(w, b) == r0, "damage against the mirror is refused");
		server.runCommand("kill @e[type=minecraft:husk]");
		waitRunEnd(ctx, w, b, 220);

		// ---- phases ------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two (Zenith) begins at 66%");
		ctx.waitTicks(90);
		check(count(w, b, "halo_blade") >= 3, "Zenith opens with three Halo Blades (" + count(w, b, "halo_blade") + ")");
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(90);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Judgement) begins at 33%");

		// ---- death rewards + ritual ---------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(150);
		check(Gates.isCleared("solareon"), "killing Solareon opens its gate");
		server.runCommand("clear @a");
		T.hold(w, "solareon_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "radiant_core") >= 8 && T.count(w, "heart_of_solareon") == 1, "the hoard holds Radiant Cores and Solareon's heart (" + T.count(w, "radiant_core") + ")");
		server.runCommand("clear @a");

		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 14, y, z + 22);
		BlockPos covered = BlockPos.containing(x + 22, y, z + 22);
		T.onServer(w, () -> {
			T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState());
			T.level(w).setBlockAndUpdate(covered, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState());
			T.level(w).setBlockAndUpdate(covered.above(3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		});
		MobDef def = Mobs.DEFS.get("solareon");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		server.runCommand("xp set @a 40 levels");
		server.runCommand("time set midnight");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "Solareon's altar answers only around midday");
		server.runCommand("time set noon");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), covered, p, def)) != null, "...and only under open sky");
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "at noon under the sky it answers");
		server.runCommand("xp set @a 5 levels");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "it refuses a player without twelve levels");
		server.runCommand("xp set @a 40 levels");
		ctx.waitTicks(3);
		ItemStack sigil = new ItemStack(UsmItems.get("radiant_sigil"));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.onServer(w, () -> p.experienceLevel) == 28, "the ritual cost twelve levels (" + T.onServer(w, () -> p.experienceLevel) + " left)");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(12), e -> e.def().id.equals("solareon")).size()) == 1,
				"the Radiant Sigil summons Solareon");
		ctx.waitTicks(12);
		T.takeShot(ctx, "boss3-10-solareon-emerges");
		server.runCommand("kill @e[type=ultimatesurvival:solareon]");
		server.runCommand("usm reset");
	}

	// =================================================================================================================================
	private static void omnarch(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock solareon");
		UsmBoss b = spawn(w, "omnarch", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 7500f && !T.onServer(w, () -> b.def().fly), "the Omnarch: 7500 HP and a ground fighter");
		check(Math.abs(T.onServer(w, () -> b.getBbHeight()) - 3.8 * 1.6) < 0.2, "and it towers (" + String.format("%.1f", T.onServer(w, () -> b.getBbHeight())) + " blocks)");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -14, y);
		immortal(w);
		ctx.waitTicks(5);
		T.takeShot(ctx, "boss3-11-omnarch-idle");

		// ---- phase one borrows the first three Sovereigns -------------------------------------------------------------------------------------------------------------------------------
		java.util.Set<String> styles = new java.util.HashSet<>();
		for (var a : Mobs.DEFS.get("omnarch").phases.get(0).attacks()) {
			styles.add(a.p().s("style", ""));
		}
		check(styles.containsAll(java.util.Set.of("ember", "storm", "blood")), "phase one borrows fire, storm and blood (" + styles + ")");
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("lightning"));
		waitRunEndFrom(ctx, w, b, 50, 200, () -> shot(ctx, w, "boss3-12-omnarch-lightning"));
		check(hits(w, b) > h0, "its borrowed storm strikes the player");

		// ---- phase two: time, void, light -------------------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.69f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two begins at 70%");
		ctx.waitTicks(90);
		at(w, b, -14, y);
		ctx.waitTicks(4);
		int b0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("beam"));
		waitRunEndFrom(ctx, w, b, 80, 300, () -> shot(ctx, w, "boss3-13-omnarch-beam"));
		check(hits(w, b) > b0, "a low beam sweeps across a player standing on the ground (hits " + b0 + " -> " + hits(w, b) + ")");
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		T.onServer(w, () -> T.player(w).setNoGravity(true));
		T.stand(w, pp.x, y + 2.6, pp.z, pp.x + 10, y + 2.6, pp.z);
		ctx.waitTicks(3);
		int b1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("beam"));
		waitRunEnd(ctx, w, b, 300);
		check(hits(w, b) == b1, "...but a player above it is not hit: low beams are jumped (hits " + b1 + " -> " + hits(w, b) + ")");
		T.onServer(w, () -> T.player(w).setNoGravity(false));
		at(w, b, -14, y);
		ctx.waitTicks(10);

		// ---- phase three: cages with three gaps, two black holes at once --------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.39f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three (Sundered) begins at 40%");
		ctx.waitTicks(90);
		check(count(w, b, "halo_blade") >= 3, "and opens with Halo Blades (" + count(w, b, "halo_blade") + ")");
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		T.onServer(w, () -> b.hazards().clear());
		at(w, b, -14, y);
		ctx.waitTicks(4);
		T.onServer(w, () -> b.forceAttack("blackhole"));
		ctx.waitTicks(3);
		for (int i = 0; i < 80 && hazards(w, b, "Singularity") < 2; i++) {
			ctx.waitTicks(1);
		}
		check(hazards(w, b, "Singularity") == 2, "two black holes open at once (" + hazards(w, b, "Singularity") + ")");
		shot(ctx, w, "boss3-14-two-holes");
		T.onServer(w, () -> b.hazards().clear());
		waitRunEnd(ctx, w, b, 200);
		int cg = Mobs.DEFS.get("omnarch").phases.get(2).attacks().stream().filter(a -> a.type().equals("cage")).mapToInt(a -> a.p().i("gaps", 2)).max().orElse(0);
		check(cg == 3, "the cage has three gaps in the Sundered phase");

		// ---- the last phase: pylons shield it and the ground burns ------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.14f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 3, "phase four (Apex) begins at 15%");
		ctx.waitTicks(100);
		check(count(w, b, "apex_pylon") == 4, "four Apex Pylons rise (" + count(w, b, "apex_pylon") + ")");
		check(T.onServer(w, () -> b.hasNodes() && b.hasFlag(UsmMob.F_SHIELD)), "the Omnarch is shielded while they stand");
		check(T.onServer(w, () -> b.hazards().size()) >= 8, "and the arena itself burns in patches (" + T.onServer(w, () -> b.hazards().size()) + ")");
		shot(ctx, w, "boss3-15-apex");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(90, 40, 90), e -> e.def().id.equals("apex_pylon"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 2000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "breaking all four pylons staggers it");

		// ---- the cage in the open: the final implosion catches someone who stays ------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.hazards().clear());
		at(w, b, -14, y);
		ctx.waitTicks(4);
		int c0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("cage"));
		waitRunEnd(ctx, w, b, 260);
		check(hits(w, b) > c0, "the cage closes on a player who stays (hits " + c0 + " -> " + hits(w, b) + ")");

		// ---- death rewards + ritual ---------------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:halo_blade]");
		server.runCommand("kill @e[type=ultimatesurvival:void_spawn]");
		T.onServer(w, () -> b.hazards().clear());
		check(!Gates.isCleared("omnarch"), "the Omnarch's gate is closed");
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(190);
		check(Gates.isCleared("omnarch") && Gates.currentTier() == 5, "killing the Omnarch opens the last gate");
		server.runCommand("clear @a");
		T.hold(w, "omnarch_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "omni_core") >= 10 && T.count(w, "heart_of_the_omnarch") == 1, "the hoard holds Omni Cores and the Omnarch's heart (" + T.count(w, "omni_core") + ")");
		server.runCommand("clear @a");

		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 26, y, z + 14);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		ctx.waitTicks(3);
		MobDef def = Mobs.DEFS.get("omnarch");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		server.runCommand("xp set @a 10 levels");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "the Omnarch's ritual refuses a player without thirty levels");
		server.runCommand("xp set @a 45 levels");
		T.onServer(w, () -> p.setHealth(8));
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "...and one too weak to pay eight hearts");
		T.onServer(w, () -> p.setHealth(20));
		ctx.waitTicks(2);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "a healthy player with the levels may begin it");
		ItemStack sigil = new ItemStack(UsmItems.get("omni_sigil"));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.onServer(w, () -> p.experienceLevel) == 15 && T.onServer(w, () -> p.getHealth()) <= 4.5f,
				"the ritual drained thirty levels and eight hearts (" + T.onServer(w, () -> p.experienceLevel) + " levels, " + T.onServer(w, () -> p.getHealth()) + " HP left)");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(14), e -> e.def().id.equals("omnarch")).size()) == 1, "the Omni Sigil summons the Omnarch");
		ctx.waitTicks(20);
		T.takeShot(ctx, "boss3-16-omnarch-emerges");
		server.runCommand("kill @e[type=ultimatesurvival:omnarch]");
		server.runCommand("usm reset");
	}
}
