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
import com.ultimatesurvival.entity.Bolt;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The three Tier 4 Sovereigns: Voltaris (storm), Hemorath (blood) and Chronos (time) - their new mechanics, loot and rituals. */
final class Boss2Tests {
	private Boss2Tests() {
	}

	/** Stands the player dx blocks east(+)/west(-) of the boss on the ground (fliers hover well above it), looking at the boss. */
	private static void at(TestSingleplayerContext w, UsmBoss b, double dx, double groundY) {
		Vec3 bp = T.onServer(w, () -> b.position());
		T.stand(w, bp.x + dx, groundY, bp.z, bp.x, Math.max(groundY, bp.y), bp.z);
	}

	private static int count(TestSingleplayerContext w, UsmBoss b, String id) {
		return T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(80, 40, 80), e -> e.def().id.equals(id) && e.isAlive()).size());
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 + 60;
		double y = me.getY();
		double z = me.getZ() + 0.5 - 60;
		T.stand(w, x - 14, y, z, x, y, z);
		ctx.waitTicks(40);

		for (String id : new String[] {"voltaris", "hemorath", "chronos", "storm_wisp", "storm_totem", "blood_bat"}) {
			check(Mobs.DEFS.containsKey(id) && Mobs.EGGS.containsKey(id), id + " is registered with a spawn egg");
		}
		voltaris(ctx, w, x, y, z);
		hemorath(ctx, w, x, y, z);
		chronos(ctx, w, x, y, z);
		mortal(w);
	}

	// =================================================================================================================================
	private static void voltaris(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock ignarok");
		UsmBoss b = spawn(w, "voltaris", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 1100f, "Voltaris has 1100 HP");
		check(T.onServer(w, () -> b.def().fly) && T.onServer(w, () -> b.isNoGravity()), "Voltaris flies");

		// ---- ranged spacing and hovering: it backs off from a player who stands too close and floats above the ground ------------------------
		T.onServer(w, () -> {
			b.noMelee = false;
			b.setTarget(T.player(w));
		});
		at(w, b, -4, y);
		immortal(w);
		ctx.waitTicks(110);
		Vec3 bp = T.onServer(w, () -> b.position());
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		double flat = Math.hypot(bp.x - pp.x, bp.z - pp.z);
		check(flat >= 8.0, "Voltaris keeps its distance from a close player (" + String.format("%.1f", flat) + " blocks)");
		check(bp.y - pp.y >= 1.5, "and hovers above the ground (" + String.format("%.1f", bp.y - pp.y) + " blocks up)");
		T.takeShot(ctx, "boss2-0-voltaris-hover");
		T.onServer(w, () -> b.noMelee = true);

		// ---- lightning: three flashes, then a real strike --------------------------------------------------------------------------------------
		at(w, b, -8, y);
		ctx.waitTicks(5);
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("lightning"));
		waitRunEndFrom(ctx, w, b, 40, 200, () -> shot(ctx, w, "boss2-1-lightning"));
		check(hits(w, b) > h0, "lightning struck the player's position (hits " + h0 + " -> " + hits(w, b) + ")");

		// ---- gust: shoves the player away from the boss ------------------------------------------------------------------------------------------------
		at(w, b, -6, y);
		ctx.waitTicks(5);
		Vec3 before = T.onServer(w, () -> T.player(w).position());
		Vec3 bpos = T.onServer(w, () -> b.position());
		T.onServer(w, () -> b.forceAttack("gust"));
		waitRunEnd(ctx, w, b, 80);
		Vec3 after = T.onServer(w, () -> T.player(w).position());
		double d0 = Math.hypot(before.x - bpos.x, before.z - bpos.z);
		double d1 = Math.hypot(after.x - bpos.x, after.z - bpos.z);
		check(d1 > d0 + 1.5, "the gust pushed the player away (" + String.format("%.1f -> %.1f", d0, d1) + ")");

		// ---- orbs: homing spheres reach a player who does not move --------------------------------------------------------------------------------
		at(w, b, -10, y);
		ctx.waitTicks(5);
		int o0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("orbs"));
		ctx.waitTicks(60);
		int orbs = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, b.getBoundingBox().inflate(60, 30, 60)).size());
		check(orbs >= 1, "orbs are drifting through the arena (" + orbs + ")");
		shot(ctx, w, "boss2-2-orbs");
		waitRunEnd(ctx, w, b, 300);
		ctx.waitTicks(80);
		check(hits(w, b) > o0, "homing orbs found the player (hits " + o0 + " -> " + hits(w, b) + ")");
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");

		// ---- perch: lands, is stunned and vulnerable, then lifts off again ----------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		float n0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 10f));
		float normal = n0 - hp(w, b);
		ctx.waitTicks(25);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		T.onServer(w, () -> b.setPos(b.getX(), b.getY() + 4, b.getZ()));
		T.onServer(w, () -> b.forceAttack("perch"));
		waitRunEndFrom(ctx, w, b, 60, 200, () -> {
			double gy = T.onServer(w, () -> T.level(w).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int) b.getX(), (int) b.getZ()));
			check(T.onServer(w, () -> b.getY()) < gy + 1.5, "Voltaris dives to the ground when it perches (y=" + T.onServer(w, () -> b.getY()) + ", ground=" + gy
					+ ", onGround=" + T.onServer(w, () -> b.onGround()) + ", run=" + T.onServer(w, () -> b.runType() + "@" + b.runTick()) + ")");
			check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "and is vulnerable while perched");
			float v0 = hp(w, b);
			T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 10f));
			check(v0 - hp(w, b) > normal * 1.4f, "a perched Voltaris takes extra damage (" + normal + " -> " + (v0 - hp(w, b)) + ")");
			shot(ctx, w, "boss2-3-perched");
		});
		ctx.waitTicks(20);
		check(!T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)) && !T.onServer(w, () -> b.perching), "the perch ends and it takes to the air again");

		// ---- phase three: shield pylons make it immune until they are all broken -----------------------------------------------------------------------------
		at(w, b, -10, y);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.60f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two begins at 66%");
		ctx.waitTicks(80);
		server.runCommand("kill @e[type=ultimatesurvival:storm_wisp]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.30f));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> b.phaseIndex()) == 2, "phase three begins at 33%");
		ctx.waitTicks(80);
		int totems = count(w, b, "storm_totem");
		check(totems == 3, "three storm pylons rise (" + totems + ")");
		check(T.onServer(w, () -> b.hasNodes()) && T.onServer(w, () -> b.hasFlag(UsmMob.F_SHIELD)), "Voltaris is shielded while they stand");
		float s0 = hp(w, b);
		boolean hit = T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 100f));
		check(!hit && hp(w, b) == s0, "a shielded Voltaris takes no damage");
		shot(ctx, w, "boss2-4-pylons");
		T.onServer(w, () -> {
			for (UsmMob t : T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(80, 40, 80), e -> e.def().id.equals("storm_totem"))) {
				t.hurtServer(T.level(w), T.level(w).damageSources().genericKill(), 1000f);
			}
		});
		ctx.waitTicks(25);
		check(!T.onServer(w, () -> b.hasNodes()) && !T.onServer(w, () -> b.hasFlag(UsmMob.F_SHIELD)), "destroying every pylon drops the shield");
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "and staggers the boss");
		float p0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 40f));
		check(hp(w, b) < p0, "now it can be hurt");

		// ---- death rewards ------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:storm_wisp]");
		check(!Gates.isCleared("voltaris"), "the Storm Regent's gate is closed");
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(130);
		check(Gates.isCleared("voltaris"), "killing Voltaris opens its gate");
		server.runCommand("clear @a");
		T.hold(w, "voltaris_bag");
		com.ultimatesurvival.test.AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "storm_core") >= 8 && T.count(w, "heart_of_voltaris") == 1, "the hoard holds Storm Cores and Voltaris's heart (" + T.count(w, "storm_core") + ")");
		server.runCommand("clear @a");

		// ---- ritual: open sky required --------------------------------------------------------------------------------------------------------------------------
		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 12, y, z + 12);
		BlockPos covered = BlockPos.containing(x + 12, y, z - 12);
		T.onServer(w, () -> {
			T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState());
			T.level(w).setBlockAndUpdate(covered, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState());
			T.level(w).setBlockAndUpdate(covered.above(3), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		});
		ctx.waitTicks(5);
		MobDef def = Mobs.DEFS.get("voltaris");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		check(T.onServer(w, () -> Ritual.check(T.level(w), covered, p, def)) != null, "the altar under a roof refuses Voltaris's ritual");
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "the altar under open sky accepts it");
		T.onServer(w, () -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UsmItems.get("tempest_sigil"))));
		T.stand(w, altar.getX() + 0.5 - 4, y, altar.getZ() + 0.5, altar.getX() + 0.5, y, altar.getZ() + 0.5);
		T.onServer(w, () -> T.level(w).getBlockState(altar).useItemOn(p.getMainHandItem(), T.level(w), p, InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(altar).add(0, 0.4, 0), net.minecraft.core.Direction.UP, altar, false)));
		ctx.waitTicks(Ritual.LENGTH + 4);
		var found = T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(10), e -> e.def().id.equals("voltaris")));
		check(found.size() == 1, "the Tempest Sigil summons Voltaris (" + found.size() + ")");
		ctx.waitTicks(15);
		T.takeShot(ctx, "boss2-5-voltaris-emerges");
		T.onServer(w, () -> found.get(0).discard());
		server.runCommand("kill @e[type=ultimatesurvival:storm_wisp]");
		server.runCommand("kill @e[type=ultimatesurvival:storm_totem]");
	}

	// =================================================================================================================================
	private static void hemorath(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock voltaris");
		UsmBoss b = spawn(w, "hemorath", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 1700f && !T.onServer(w, () -> b.def().fly), "Hemorath: 1700 HP, a ground fighter");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -8, y);
		immortal(w);
		ctx.waitTicks(5);
		T.takeShot(ctx, "boss2-6-hemorath-idle");

		// ---- leech: drains the player and heals her; a wall in the way breaks the beam ----------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.8f));          // stay above the 66% phase threshold
		float l0 = hp(w, b);
		int h0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("leech"));
		waitRunEndFrom(ctx, w, b, 40, 160, () -> shot(ctx, w, "boss2-7-leech"));
		check(hits(w, b) >= h0 + 4, "the leech beam keeps hurting (hits " + h0 + " -> " + hits(w, b) + ")");
		check(hp(w, b) > l0 + 8, "and heals Hemorath (" + l0 + " -> " + hp(w, b) + ")");
		Vec3 bp = T.onServer(w, () -> b.position());
		Vec3 pp = T.onServer(w, () -> T.player(w).position());
		BlockPos mid = BlockPos.containing((bp.x + pp.x) / 2, y, (bp.z + pp.z) / 2);
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", mid.getX(), mid.getY(), mid.getZ() - 3, mid.getX(), mid.getY() + 6, mid.getZ() + 3));
		ctx.waitTicks(3);
		float l1 = hp(w, b);
		int h1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("leech"));
		waitRunEnd(ctx, w, b, 160);
		check(hits(w, b) == h1 && hp(w, b) <= l1 + 0.5f, "a wall between them breaks the beam (hits " + h1 + " -> " + hits(w, b) + ")");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", mid.getX(), mid.getY(), mid.getZ() - 3, mid.getX(), mid.getY() + 6, mid.getZ() + 3));

		// ---- leap: she lands where you were ------------------------------------------------------------------------------------------------------------------
		at(w, b, -14, y);
		ctx.waitTicks(5);
		Vec3 target = T.onServer(w, () -> T.player(w).position());
		int lh = hits(w, b);
		Vec3 leapFrom = T.onServer(w, () -> b.position());
		T.onServer(w, () -> b.forceAttack("leap"));
		waitRunEndFrom(ctx, w, b, 12, 120, () -> shot(ctx, w, "boss2-8-leap"));
		Vec3 land = T.onServer(w, () -> b.position());
		check(Math.hypot(land.x - target.x, land.z - target.z) < 4.5, "the leap lands on the target's position (" + String.format("%.1f", Math.hypot(land.x - target.x, land.z - target.z)) + " off; from " + leapFrom
				+ " target " + target + " landed " + land + " tgt=" + T.onServer(w, () -> b.getTarget() == null ? "null" : "set") + ")");
		check(hits(w, b) > lh, "and the landing slam reaches the player");

		// ---- reflect: attackers are hurt, she is not -------------------------------------------------------------------------------------------------------------------
		at(w, b, -6, y);
		Vec3 bp2 = T.onServer(w, () -> b.position());
		Husk attacker = AbilityTests.spawn(w, bp2.x + 2.5, y, bp2.z, 90);
		T.onServer(w, () -> b.forceAttack("reflect"));
		ctx.waitTicks(24);
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_SHIELD)), "Hemorath raises her crimson shield");
		float r0 = hp(w, b);
		float a0 = T.onServer(w, () -> attacker.getHealth());
		boolean got = T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().mobAttack(attacker), 40f));
		check(!got && hp(w, b) == r0, "damage against the shield is refused");
		check(T.onServer(w, () -> attacker.getHealth()) < a0, "and half of it is thrown back at the attacker (" + a0 + " -> " + T.onServer(w, () -> attacker.getHealth()) + ")");
		shot(ctx, w, "boss2-9-reflect");
		server.runCommand("kill @e[type=minecraft:husk]");
		waitRunEnd(ctx, w, b, 200);
		check(!T.onServer(w, () -> b.hasFlag(UsmMob.F_SHIELD)), "the shield drops when the stance ends");

		// ---- pools slow, bats multiply -------------------------------------------------------------------------------------------------------------------------------------
		at(w, b, -8, y);
		immortal(w);
		ctx.waitTicks(3);
		T.onServer(w, () -> b.forceAttack("pools"));
		waitRunEnd(ctx, w, b, 120);
		ctx.waitTicks(25);
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.SLOWNESS)), "standing in a blood pool slows you");
		shot(ctx, w, "boss2-10-blood-pool");
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.forceAttack("summon"));
		waitRunEnd(ctx, w, b, 120);
		check(count(w, b, "blood_bat") >= 3, "Hemorath calls her bats (" + count(w, b, "blood_bat") + ")");
		shot(ctx, w, "boss2-11-bats");
		server.runCommand("kill @e[type=ultimatesurvival:blood_bat]");
		T.onServer(w, () -> b.minionCount());

		// ---- death + ritual costs --------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(130);
		check(Gates.isCleared("hemorath"), "killing Hemorath opens her gate");
		server.runCommand("clear @a");
		T.hold(w, "hemorath_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "blood_core") >= 8 && T.count(w, "matriarchs_kiss") == 1, "her hoard holds Blood Cores and the Matriarch's Kiss (" + T.count(w, "blood_core") + ")");
		server.runCommand("clear @a");

		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 12, y, z + 18);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		ctx.waitTicks(3);
		MobDef def = Mobs.DEFS.get("hemorath");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		server.runCommand("time set day");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "Hemorath's altar answers only at night");
		server.runCommand("time set midnight");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "...and does at midnight");
		T.onServer(w, () -> p.setHealth(8));
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "it refuses a player too weak to pay four hearts");
		T.onServer(w, () -> p.setHealth(20));
		ItemStack sigil = new ItemStack(UsmItems.get("blood_sigil"));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.onServer(w, () -> p.getHealth()) <= 12.5f, "the ritual costs four hearts (" + T.onServer(w, () -> p.getHealth()) + " HP left)");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(10), e -> e.def().id.equals("hemorath")).size()) == 1,
				"the Blood Sigil summons Hemorath");
		server.runCommand("kill @e[type=ultimatesurvival:hemorath]");
		server.runCommand("time set day");
	}

	// =================================================================================================================================
	private static void chronos(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm unlock hemorath");
		UsmBoss b = spawn(w, "chronos", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 2400f, "Chronos has 2400 HP");
		T.onServer(w, () -> b.setTarget(T.player(w)));
		at(w, b, -9, y);
		immortal(w);
		ctx.waitTicks(5);
		T.takeShot(ctx, "boss2-12-chronos-idle");

		// ---- he is hard to hurt, except while the hourglass is open ---------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		float n0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 20f));
		float normal = n0 - hp(w, b);
		ctx.waitTicks(25);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		T.onServer(w, () -> b.forceAttack("exposed"));
		waitRunEndFrom(ctx, w, b, 40, 160, () -> {
			check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "the hourglass opens");
			float v0 = hp(w, b);
			T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 20f));
			float open = v0 - hp(w, b);
			check(open > normal * 3.0f, "an open hourglass multiplies the damage (" + normal + " -> " + open + ")");
			shot(ctx, w, "boss2-13-exposed");
		});

		// ---- rewind: everyone is put back where they stood three seconds ago ----------------------------------------------------------------------------------------------
		Vec3 a = T.onServer(w, () -> b.position()).add(-14, 0, 0);
		T.stand(w, a.x, y, a.z, a.x + 5, y, a.z);
		ctx.waitTicks(75);                                                  // fill the position history
		Vec3 far = a.add(-10, 0, 8);
		T.stand(w, far.x, y, far.z, far.x + 5, y, far.z);
		T.onServer(w, () -> b.forceAttack("rewind"));
		waitRunEndFrom(ctx, w, b, 30, 100, () -> shot(ctx, w, "boss2-14-rewind"));
		Vec3 now = T.onServer(w, () -> T.player(w).position());
		check(Math.hypot(now.x - a.x, now.z - a.z) < 3.0, "the rewind returned the player to where they stood (" + String.format("%.1f", Math.hypot(now.x - a.x, now.z - a.z)) + " off)");

		// ---- time stop: a ring of frozen bolts, released all at once -------------------------------------------------------------------------------------------------------
		at(w, b, -10, y);
		immortal(w);
		ctx.waitTicks(5);
		int s0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("stop"));
		ctx.waitTicks(40);
		var frozen = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, b.getBoundingBox().inflate(60, 30, 60)));
		check(frozen.size() >= 10, "time stops with a ring of bolts hanging in the air (" + frozen.size() + ")");
		check(frozen.stream().allMatch(bolt -> T.onServer(w, () -> bolt.getDeltaMovement().lengthSqr()) < 1e-6), "and none of them is moving");
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.SLOWNESS)), "the player is slowed while it hangs");
		shot(ctx, w, "boss2-15-time-stop");
		waitRunEnd(ctx, w, b, 200);
		ctx.waitTicks(40);
		int released = hits(w, b) - s0;
		check(released >= 6 && released <= 13, "released, the bolts converge on the player but leave a gap (" + released + " of 14 reached)");
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");

		// ---- blink: appears behind the target and strikes -------------------------------------------------------------------------------------------------------------------------
		at(w, b, -12, y);
		ctx.waitTicks(5);
		int bh = hits(w, b);
		Vec3 tp = T.onServer(w, () -> T.player(w).position());
		T.onServer(w, () -> b.forceAttack("blink"));
		waitRunEnd(ctx, w, b, 120);
		Vec3 bpos = T.onServer(w, () -> b.position());
		check(Math.hypot(bpos.x - tp.x, bpos.z - tp.z) < 5.0, "Chronos blinks next to the player (" + String.format("%.1f", Math.hypot(bpos.x - tp.x, bpos.z - tp.z)) + " blocks away)");
		check(hits(w, b) > bh, "and strikes the spot");

		// ---- death + the xp ritual ---------------------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(130);
		check(Gates.isCleared("chronos"), "killing Chronos opens its gate");
		server.runCommand("clear @a");
		T.hold(w, "chronos_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "chrono_core") >= 8 && T.count(w, "chronos_heart") == 1, "the hoard holds Chrono Cores and Chronos's heart (" + T.count(w, "chrono_core") + ")");
		server.runCommand("clear @a");

		UsmConfig.get().ritualsAnywhere = false;
		BlockPos altar = BlockPos.containing(x + 20, y, z + 12);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		ctx.waitTicks(3);
		MobDef def = Mobs.DEFS.get("chronos");
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		mortal(w);
		server.runCommand("xp set @a 0 levels");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "Chronos's ritual refuses a player without five levels");
		server.runCommand("xp set @a 12 levels");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "and accepts one who has them");
		ItemStack sigil = new ItemStack(UsmItems.get("chrono_sigil"));
		T.onServer(w, () -> Ritual.begin(T.level(w), altar, p, sigil, def));
		check(T.onServer(w, () -> p.experienceLevel) == 7, "the ritual drained five levels (" + T.onServer(w, () -> p.experienceLevel) + " left)");
		ctx.waitTicks(Ritual.LENGTH + 4);
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(10), e -> e.def().id.equals("chronos")).size()) == 1,
				"the Chrono Sigil summons Chronos");
		server.runCommand("kill @e[type=ultimatesurvival:chronos]");
		server.runCommand("usm reset");
	}
}
