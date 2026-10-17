package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.UsmState;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Tier 3 gear: Cinderite tools (depth, smelting), fire abilities, the Cinderite set bonuses and the Phoenix charm. */
final class FireTests {
	private FireTests() {
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		ctx.waitTicks(2);
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		int ox = me.getX() + 28;
		int oy = me.getY();
		int oz = me.getZ() - 30;
		T.stand(w, ox - 3, oy, oz + 3, ox, oy + 1, oz + 3);
		ctx.waitTicks(40);

		// ---- locked: Cinderite gear does nothing until Ignarok falls --------------------------------------------------------------
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 3, oy + 6, oz + 6));
		T.stand(w, ox - 2.5, oy + 3, oz + 3.5, ox + 0.5, oy + 3.5, oz + 3.5);
		T.hold(w, "cinderite_pickaxe");
		ctx.waitTicks(3);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 3, oz + 3)));
		int one = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dz = -1; dz <= 1; dz++) {
				one += T.air(w, new BlockPos(ox, oy + 3 + dy, oz + 3 + dz)) ? 1 : 0;
			}
		}
		check(one == 1, "a locked Cinderite pickaxe breaks a single block (cleared " + one + ")");

		// ---- unlocked: 3x3x2 ------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock ignarok");
		ctx.waitTicks(3);
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 3, oy + 6, oz + 6));
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 3, oz + 3)));
		int front = 0;
		int second = 0;
		int third = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dz = -1; dz <= 1; dz++) {
				front += T.air(w, new BlockPos(ox, oy + 3 + dy, oz + 3 + dz)) ? 1 : 0;
				second += T.air(w, new BlockPos(ox + 1, oy + 3 + dy, oz + 3 + dz)) ? 1 : 0;
				third += T.air(w, new BlockPos(ox + 2, oy + 3 + dy, oz + 3 + dz)) ? 1 : 0;
			}
		}
		check(front == 9 && second == 9 && third == 0, "the Cinderite pickaxe digs a 3x3 face two deep (" + front + "/" + second + "/" + third + ")");

		// ---- auto-smelt: ore comes out as ingots, logs as charcoal ----------------------------------------------------------------------
		server.runCommand("clear @a");
		T.hold(w, "cinderite_pickaxe");
		server.runCommand(String.format("setblock %d %d %d minecraft:iron_ore", ox + 2, oy + 3, oz + 3));
		T.onServer(w, () -> T.player(w).setShiftKeyDown(true));
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox + 2, oy + 3, oz + 3)));
		T.onServer(w, () -> T.player(w).setShiftKeyDown(false));
		ctx.waitTicks(3);
		check(T.count(w, "minecraft:iron_ingot") >= 1 && T.count(w, "minecraft:raw_iron") == 0, "the Cinderite pickaxe smelts iron ore into an ingot ("
				+ T.count(w, "minecraft:iron_ingot") + " ingots)");
		T.hold(w, "cinderite_axe");
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", ox, oy, oz, ox + 3, oy + 6, oz + 6));
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:oak_log", ox + 1, oy, oz + 3, ox + 1, oy + 3, oz + 3));
		T.stand(w, ox - 2.5, oy + 1, oz + 3.5, ox + 1.5, oy + 1.5, oz + 3.5);
		ctx.waitTicks(3);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox + 1, oy, oz + 3)));
		ctx.waitTicks(3);
		check(T.count(w, "minecraft:charcoal") >= 3, "the Cinderite axe fells the trunk and smelts the logs to charcoal (" + T.count(w, "minecraft:charcoal") + ")");

		// ---- Fire Nova (Cinder Brand): damages + ignites everything close ------------------------------------------------------------------
		server.runCommand("clear @a");
		int bx = ox;
		int bz = oz + 20;
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy + 1.6, bz + 0.5);
		Husk near = AbilityTests.spawn(w, bx + 3.5, oy, bz + 0.5, 0);
		Husk far = AbilityTests.spawn(w, bx + 15.5, oy, bz + 0.5, 0);
		T.hold(w, "cinder_brand");
		ctx.waitTicks(3);
		AbilityTests.use(w);
		ctx.waitTicks(2);
		check(T.onServer(w, () -> near.getHealth()) < 10 && T.onServer(w, () -> near.isOnFire()), "Fire Nova burned the nearby husk ("
				+ T.onServer(w, () -> near.getHealth()) + " HP, on fire " + T.onServer(w, () -> near.isOnFire()) + ")");
		check(T.onServer(w, () -> far.getHealth()) >= 20 && !T.onServer(w, () -> far.isOnFire()), "Fire Nova did not reach a husk 15 blocks away");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- Ignite on hit, and burning enemies take extra damage ---------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		Husk a = AbilityTests.spawn(w, bx + 2.0, oy, bz + 0.5, 90);
		Husk burning = AbilityTests.spawn(w, bx + 2.0, oy, bz + 4.5, 90);
		T.onServer(w, () -> burning.igniteForSeconds(30));
		T.onServer(w, () -> {
			for (Husk h : new Husk[] {a, burning}) {                                   // the brand hits hard now: measure the damage, not the overkill
				h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(5000);
				h.setHealth(5000);
			}
		});
		T.hold(w, "cinder_brand");
		T.onServer(w, () -> T.player(w).getCooldowns().removeCooldown(T.player(w).getCooldowns().getCooldownGroup(T.player(w).getMainHandItem())));
		ctx.waitTicks(25);
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		float a0 = T.onServer(w, () -> a.getHealth());
		T.onServer(w, () -> p.attack(a));
		float lossNormal = a0 - T.onServer(w, () -> a.getHealth());
		check(T.onServer(w, () -> a.isOnFire()), "a Cinder Brand hit sets the target on fire");
		T.stand(w, bx + 0.5, oy, bz + 4.5, bx + 3, oy + 1.2, bz + 4.5);
		ctx.waitTicks(25);
		float b0 = T.onServer(w, () -> burning.getHealth());
		T.onServer(w, () -> p.attack(burning));
		float lossBurning = b0 - T.onServer(w, () -> burning.getHealth());
		check(lossBurning > lossNormal + 2.5f, "burning enemies take extra damage (" + lossNormal + " vs " + lossBurning + ")");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- Pyre Staff: an exploding, igniting fireball ----------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 10, oy, bz + 0.5);                // "facing" aims from the feet: keep the pitch level
		Husk target = AbilityTests.spawn(w, bx + 8.5, oy, bz + 0.5, 90);
		Husk beside = AbilityTests.spawn(w, bx + 8.5, oy, bz + 2.5, 90);
		T.onServer(w, () -> {
			for (Husk h : new Husk[] {target, beside}) {
				h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(300);   // survive the 28-damage blast
				h.setHealth(300);
			}
		});
		T.hold(w, "pyre_staff");
		ctx.waitTicks(3);
		AbilityTests.use(w);
		int bolts = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, new AABB(bx - 5, oy - 5, bz - 10, bx + 40, oy + 10, bz + 10)).size());
		check(bolts == 1, "the Pyre Staff throws a single fireball (" + bolts + ")");
		ctx.waitTicks(10);
		check(T.onServer(w, () -> target.getHealth()) < 300 && T.onServer(w, () -> target.isOnFire()), "the fireball hit and ignited its target ("
				+ T.onServer(w, () -> target.getHealth()) + " HP, fire " + T.onServer(w, () -> target.isOnFire()) + ")");
		check(T.onServer(w, () -> beside.getHealth()) < 300, "and its blast hurt the husk standing next to it");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- the Cinderite set: fire immunity, burning thorns, flame aura ------------------------------------------------------------------------------------
		T.stand(w, bx + 0.5, oy, bz + 0.5, bx + 3, oy + 1.2, bz + 0.5);
		T.wear(w, "cinderite");
		ctx.waitTicks(15);
		check(T.onServer(w, () -> Wearer.has(T.player(w), "fire_res")) && T.onServer(w, () -> Wearer.has(T.player(w), "flame_aura")),
				"four Cinderite pieces grant the set bonuses");
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.FIRE_RESISTANCE)), "the set grants fire resistance");
		Husk attacker = AbilityTests.spawn(w, bx + 2.5, oy, bz + 0.5, 90);
		T.onServer(w, () -> attacker.doHurtTarget(T.level(w), T.player(w)));
		check(T.onServer(w, () -> attacker.isOnFire()), "Cinderite thorns set an attacker on fire");
		server.runCommand("kill @e[type=minecraft:husk]");
		Husk close = AbilityTests.spawn(w, bx + 2.5, oy, bz + 0.5, 90);
		ctx.waitTicks(45);
		check(T.onServer(w, () -> close.getHealth()) < 20, "the flame aura burns husks that stand close (" + T.onServer(w, () -> close.getHealth()) + " HP)");
		server.runCommand("kill @e[type=minecraft:husk]");
		T.stripArmor(w);

		// ---- Phoenix Ash: a lethal blow is refused once, then it must recharge -----------------------------------------------------------------------------------
		var list = T.onServer(w, () -> UsmState.progress().accessories(T.player(w).getUUID()));
		T.onServer(w, () -> list.set(0, new ItemStack(UsmItems.get("phoenix_ash"))));
		ctx.waitTicks(15);
		check(T.onServer(w, () -> Wearer.has(T.player(w), "phoenix")), "Phoenix Ash is worn");
		T.onServer(w, () -> {
			T.player(w).setHealth(20);
			T.player(w).hurtServer(T.level(w), T.level(w).damageSources().generic(), 500f);
		});
		ctx.waitTicks(3);
		check(T.onServer(w, () -> T.player(w).isAlive()) && T.onServer(w, () -> T.player(w).getHealth()) >= 9, "a killing blow is turned into a rebirth ("
				+ T.onServer(w, () -> T.player(w).getHealth()) + " HP)");
		check(!T.onServer(w, () -> com.ultimatesurvival.ability.FireAbilities.phoenixCharged(T.player(w))), "the charm is spent and recharging");
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.RESISTANCE));
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.FIRE_RESISTANCE));
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(10);
	}
}
