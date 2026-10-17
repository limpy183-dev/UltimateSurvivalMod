package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.item.UsmItems;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Weapon abilities: dash, slam, volley, backstab, kill buffs, energy bow, cooldowns, and locked weapons doing nothing. */
final class AbilityTests {
	private AbilityTests() {
	}

	static Husk spawn(TestSingleplayerContext w, double x, double y, double z, float yaw) {
		return T.onServer(w, () -> {
			Husk z0 = new Husk(EntityTypes.HUSK, T.level(w));
			z0.setNoAi(true);
			z0.setPersistenceRequired();
			z0.snapTo(x, y, z, yaw, 0);
			T.level(w).addFreshEntity(z0);
			return z0;
		});
	}

	static void use(TestSingleplayerContext w) {
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			p.gameMode.useItem(p, T.level(w), p.getMainHandItem(), InteractionHand.MAIN_HAND);
		});
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("usm unlock ender_dragon");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 + 30;
		double y = me.getY();
		double z = me.getZ() + 0.5 + 30;
		ctx.waitTicks(2);

		// ---- locked weapons do nothing ----------------------------------------------------------------------------------
		server.runCommand("usm reset");
		T.stand(w, x, y, z, x + 10, y + 1.6, z);
		T.hold(w, "wyrmfang");
		ctx.waitTicks(3);
		use(w);
		Vec3 after = T.onServer(w, () -> T.player(w).position());
		check(after.distanceTo(new Vec3(x, y, z)) < 1.0, "a locked Wyrmfang cannot dash");
		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(3);

		// ---- dash strike -------------------------------------------------------------------------------------------------
		Husk z1 = spawn(w, x + 5, y, z, 90);
		T.stand(w, x, y, z, x + 10, y + 1.6, z);
		T.hold(w, "wyrmfang");
		ctx.waitTicks(3);
		float hp0 = T.onServer(w, () -> z1.getHealth());
		use(w);
		ctx.waitTicks(2);
		Vec3 p1 = T.onServer(w, () -> T.player(w).position());
		check(p1.x - x > 5.0, "Wyrmfang dashed ~7 blocks forward (moved " + (p1.x - x) + ")");
		float hp1 = T.onServer(w, () -> z1.getHealth());
		check(hp1 < hp0 - 5, "the zombie in the dash path took damage (" + hp0 + " -> " + hp1 + ")");
		check(T.onServer(w, () -> T.player(w).getCooldowns().isOnCooldown(T.player(w).getMainHandItem())), "dash puts the sword on cooldown");
		ctx.waitTicks(6);
		T.takeShot(ctx, "phase2-hud-cooldown");
		Vec3 p1b = T.onServer(w, () -> T.player(w).position());
		use(w);                                   // second use during cooldown must be a no-op
		ctx.waitTicks(2);
		double moved = T.onServer(w, () -> T.player(w).position()).distanceTo(p1b);
		check(moved < 0.5, "cooldown blocks a second dash (moved " + moved + ")");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- ground slam --------------------------------------------------------------------------------------------------
		T.stand(w, x, y, z, x + 10, y + 1.6, z);
		Husk z2 = spawn(w, x + 3, y, z + 1, 0);
		Husk z3 = spawn(w, x - 2, y, z - 2, 0);
		Husk far = spawn(w, x + 14, y, z, 0);
		T.hold(w, "drakebreaker");
		ctx.waitTicks(3);
		use(w);
		ctx.waitTicks(2);
		check(T.onServer(w, () -> z2.getHealth()) < 20 && T.onServer(w, () -> z3.getHealth()) < 20, "Drakebreaker's slam hurt both nearby zombies");
		check(T.onServer(w, () -> far.getHealth()) >= 20, "the slam did not reach a zombie 14 blocks away");
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- projectile volley (staff) + sneak blink ----------------------------------------------------------------
		T.stand(w, x, y, z, x + 10, y + 1.6, z);
		T.hold(w, "azure_wand");
		ctx.waitTicks(3);
		use(w);
		int bolts = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, new AABB(x - 5, y - 5, z - 5, x + 60, y + 10, z + 5)).size());
		int fan = (int) T.ab("azure_wand", "projectile_wave", "count");
		check(bolts == fan, "the wand fired a " + fan + "-bolt fan (" + bolts + " bolts)");
		ctx.waitTicks(30);
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");
		T.onServer(w, () -> T.player(w).getCooldowns().removeCooldown(T.player(w).getCooldowns().getCooldownGroup(T.player(w).getMainHandItem())));
		T.onServer(w, () -> T.player(w).setShiftKeyDown(true));
		Vec3 b0 = T.onServer(w, () -> T.player(w).position());
		use(w);
		T.onServer(w, () -> T.player(w).setShiftKeyDown(false));
		check(T.onServer(w, () -> T.player(w).position()).x - b0.x > 6.0, "sneak + right-click on the wand blinks forward");

		// ---- backstab: the dagger hits harder from behind ----------------------------------------------------
		T.stand(w, x, y, z, x + 10, y + 1.6, z);
		Husk facingAway = spawn(w, x + 1.5, y, z, -90);      // yaw -90 = looking +x (away from us at -x side)
		Husk facingMe = spawn(w, x + 1.5, y, z + 3, 90);     // yaw 90 = looking -x (toward us)
		T.hold(w, "verdite_dagger");
		ctx.waitTicks(3);
		float lostBack = hit(w, facingAway);
		float lostFront = hit(w, facingMe);
		check(lostBack > lostFront * 1.35f, "dagger backstab: " + lostBack + " vs frontal " + lostFront);
		server.runCommand("kill @e[type=minecraft:husk]");

		// ---- on-kill buff --------------------------------------------------------------------------------------------------
		T.hold(w, "verdite_sword");
		Husk victim = spawn(w, x + 1.5, y, z, 0);
		T.onServer(w, () -> T.player(w).removeAllEffects());
		ctx.waitTicks(3);
		T.onServer(w, () -> {
			ServerLevel l = T.level(w);
			victim.hurtServer(l, l.damageSources().playerAttack(T.player(w)), 1000f);
		});
		ctx.waitTicks(2);
		check(T.onServer(w, () -> T.player(w).hasEffect(MobEffects.SPEED)), "killing with the Verdite Blade grants Speed");

		// ---- energy bow: full draw fires a piercing bolt, short draw nothing ------------------------------------
		T.hold(w, "skyshot_bow");
		ctx.waitTicks(3);
		ItemStack bowStack = T.onServer(w, () -> T.player(w).getMainHandItem());
		T.onServer(w, () -> bowStack.getItem().releaseUsing(bowStack, T.level(w), T.player(w), 72000 - 1));
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, new AABB(x - 5, y - 5, z - 5, x + 60, y + 10, z + 5)).size()) == 0,
				"an instant release fires nothing");
		T.onServer(w, () -> bowStack.getItem().releaseUsing(bowStack, T.level(w), T.player(w), 72000 - 20));
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, new AABB(x - 5, y - 5, z - 5, x + 60, y + 10, z + 5)).size()) == 1,
				"a full draw fires one energy bolt without ammo");
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");

		// ---- arrow bow: consumes an arrow, boosted damage -------------------------------------------------------------
		server.runCommand("clear @a");
		T.hold(w, "verdite_bow");
		server.runCommand("give @a minecraft:arrow 4");
		ctx.waitTicks(3);
		ItemStack vb = T.onServer(w, () -> T.player(w).getMainHandItem());
		T.onServer(w, () -> vb.getItem().releaseUsing(vb, T.level(w), T.player(w), 72000 - 20));
		List<AbstractArrow> arrows = T.onServer(w, () -> T.level(w).getEntitiesOfClass(AbstractArrow.class, new AABB(x - 5, y - 5, z - 5, x + 60, y + 10, z + 5)));
		check(arrows.size() == 1, "the verdite bow fired a vanilla arrow");
		check(T.count(w, "minecraft:arrow") == 3, "and consumed one arrow from the inventory");
		server.runCommand("kill @e[type=minecraft:arrow]");
		ctx.waitTicks(2);
		T.takeShot(ctx, "phase2-abilities");
	}

	/** Hit once for 4 damage the way a player melee does; returns the health the zombie lost. */
	private static float hit(TestSingleplayerContext w, LivingEntity target) {
		float before = T.onServer(w, () -> target.getHealth());
		T.onServer(w, () -> {
			ServerLevel l = T.level(w);
			ServerPlayer p = T.player(w);
			target.invulnerableTime = 0;
			target.hurtServer(l, l.damageSources().playerAttack(p), 4f);
		});
		return before - T.onServer(w, () -> target.getHealth());
	}
}
