package com.ultimatesurvival.test;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Boss framework: registration, rules (immunities, phases, vulnerability), every Ignarok attack, death rewards and the ritual. */
final class BossTests {
	private BossTests() {
	}

	private static final float FULL = 1024f;

	// ---- helpers ---------------------------------------------------------------------------------------------------------------
	static UsmBoss spawn(TestSingleplayerContext w, String id, double x, double y, double z) {
		return T.onServer(w, () -> {
			@SuppressWarnings("unchecked")
			EntityType<UsmBoss> type = (EntityType<UsmBoss>) Mobs.TYPES.get(id);
			UsmBoss b = type.create(T.level(w), EntitySpawnReason.COMMAND);
			b.snapTo(x, y, z, 90f, 0f);
			T.level(w).addFreshEntity(b);
			b.setAttackCooldown(100000);
			b.passive = true;
			b.noMelee = true;
			return b;
		});
	}

	static float hp(TestSingleplayerContext w, UsmMob m) {
		return T.onServer(w, () -> m.getHealth());
	}

	static float myHp(TestSingleplayerContext w) {
		return T.onServer(w, () -> T.player(w).getHealth());
	}

	/** Puts the player dx blocks east(+)/west(-) of the boss's current position (dy up), looking at the boss. */
	static void face(TestSingleplayerContext w, UsmBoss b, double dx, double dy) {
		Vec3 bp = T.onServer(w, () -> b.position());
		T.stand(w, bp.x + dx, bp.y + dy, bp.z, bp.x, bp.y + 2.2, bp.z);
	}

	static void face(TestSingleplayerContext w, UsmBoss b, double dx) {
		face(w, b, dx, 0);
	}

	/** Make the player effectively unkillable so damage can be measured, restore it with {@link #mortal}. */
	static void immortal(TestSingleplayerContext w) {
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			// Resistance V = no damage taken, yet (unlike setInvulnerable) the player stays a valid target for the boss
			p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100000, 4, false, false));
			p.setHealth(20);
			p.clearFire();
		});
	}

	static int hits(TestSingleplayerContext w, UsmMob m) {
		return T.onServer(w, () -> m.hits);
	}

	static void mortal(TestSingleplayerContext w) {
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			p.setNoGravity(false);
			p.removeEffect(MobEffects.RESISTANCE);
			p.setHealth(20);
			p.clearFire();
		});
	}

	/** Screenshot with a normal health bar (the immortal test player would otherwise draw hundreds of hearts). */
	static void shot(ClientGameTestContext ctx, TestSingleplayerContext w, String name) {
		ctx.waitTicks(1);
		T.takeShot(ctx, name);
	}

	/** Waits until the attack has run for {@code atTick} ticks, runs {@code during}, then waits for the attack to end. */
	static void waitRunEndFrom(ClientGameTestContext ctx, TestSingleplayerContext w, UsmMob b, int atTick, int max, Runnable during) {
		ctx.waitTicks(3);
		for (int i = 0; i < 80 && T.onServer(w, () -> b.attacking()) && T.onServer(w, () -> b.runTick()) < atTick; i++) {
			ctx.waitTicks(1);
		}
		during.run();
		for (int i = 0; i < max && T.onServer(w, () -> b.attacking()); i += 2) {
			ctx.waitTicks(2);
		}
	}

	static void waitRunEnd(ClientGameTestContext ctx, TestSingleplayerContext w, UsmMob b, int max) {
		waitRunEndFrom(ctx, w, b, 0, max, () -> { });
	}

	// ---- the suite -----------------------------------------------------------------------------------------------------------------
	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) throws Exception {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("kill @e[type=minecraft:husk]");
		server.runCommand("clear @a");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 + 60;
		double y = me.getY();
		double z = me.getZ() + 0.5 + 60;
		T.stand(w, x - 14, y, z, x, y + 2, z);
		ctx.waitTicks(40);

		// ---- registration -----------------------------------------------------------------------------------------------------
		check(Mobs.DEFS.containsKey("ignarok") && Mobs.DEFS.containsKey("ember_imp"), "manifest mobs registered: " + Mobs.DEFS.keySet());
		MobDef def = Mobs.DEFS.get("ignarok");
		check(def.boss && def.phases.size() == 3, "Ignarok is a boss with three phases");
		check(Mobs.EGGS.containsKey("ignarok") && Mobs.EGGS.containsKey("ember_imp"), "spawn eggs exist for both mobs");

		UsmBoss b = spawn(w, "ignarok", x, y, z);
		ctx.waitTicks(10);
		check(T.onServer(w, () -> b.getMaxHealth()) == 700f, "Ignarok has 700 HP (" + T.onServer(w, () -> b.getMaxHealth()) + ")");
		check(T.onServer(w, () -> b.fireImmune()), "Ignarok is fire immune");
		check(T.onServer(w, () -> b.getBbHeight()) > 4.0f, "Ignarok is tall (" + T.onServer(w, () -> b.getBbHeight()) + ")");
		check(T.onServer(w, () -> b.getName().getString()).contains("Ignarok"), "its name translates: " + T.onServer(w, () -> b.getName().getString()));
		check(T.onServer(w, () -> b.bossBar().getPlayers().contains(T.player(w))), "the boss bar is shown to the nearby player");
		check(T.onServer(w, () -> b.phaseIndex()) == 0, "starts in phase one");

		// ---- idle screenshot --------------------------------------------------------------------------------------------------------
		face(w, b, -13);
		T.onServer(w, () -> b.setTarget(null));
		ctx.waitTicks(10);
		T.takeShot(ctx, "boss-0-ignarok-idle");

		// ---- environmental immunities --------------------------------------------------------------------------------------------------
		float h0 = hp(w, b);
		T.onServer(w, () -> {
			ServerLevel l = T.level(w);
			b.hurtServer(l, l.damageSources().inWall(), 50);
			b.hurtServer(l, l.damageSources().drown(), 50);
			b.hurtServer(l, l.damageSources().fall(), 50);
			b.hurtServer(l, l.damageSources().cactus(), 50);
		});
		check(hp(w, b) == h0, "wall / drowning / fall / cactus damage cannot hurt the boss");

		// ---- phase thresholds: a hit cannot skip past the next phase ---------------------------------------------------------------------------
		immortal(w);
		T.onServer(w, () -> b.setHealth(b.getMaxHealth() * 0.70f));
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 400f));
		float afterBig = hp(w, b);
		check(Math.abs(afterBig - 700 * 0.66f) < 1.5f, "a 400-damage hit stops at the 66% threshold (" + afterBig + ")");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> b.phaseIndex()) == 1, "phase two begins");
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_TRANSITION)), "the transition flag is raised");
		float inTransition = hp(w, b);
		boolean hit = T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 50f));
		check(!hit && hp(w, b) == inTransition, "the boss is invulnerable during the phase transition");
		shot(ctx, w, "boss-1-phase-roar");
		ctx.waitTicks(75);
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_TRANSITION)) == false, "the transition ends");
		ctx.waitTicks(60);
		check(T.onServer(w, () -> b.minionCount()) >= 1, "phase two opens by summoning imps (" + T.onServer(w, () -> b.minionCount()) + ")");
		server.runCommand("kill @e[type=ultimatesurvival:ember_imp]");
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));

		// ---- slam: damages on the ground, a jump avoids it -----------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setTarget(T.player(w)));
		face(w, b, -4);
		ctx.waitTicks(4);
		immortal(w);
		int h0s = hits(w, b);
		T.onServer(w, () -> b.forceAttack("slam"));
		waitRunEndFrom(ctx, w, b, 17, 100, () -> shot(ctx, w, "boss-2-slam-telegraph"));
		check(hits(w, b) == h0s + 1, "the slam reached a grounded player (hits " + h0s + " -> " + hits(w, b) + ")");
		face(w, b, -4, 2.5);
		T.onServer(w, () -> T.player(w).setNoGravity(true));
		ctx.waitTicks(3);
		int j0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("slam"));
		waitRunEnd(ctx, w, b, 100);
		check(hits(w, b) == j0, "jumping over the slam avoids it (hits " + j0 + " -> " + hits(w, b) + ")");
		T.onServer(w, () -> T.player(w).setNoGravity(false));
		face(w, b, -8);
		ctx.waitTicks(10);

		// ---- fan of bolts ------------------------------------------------------------------------------------------------------------------------------
		immortal(w);
		T.onServer(w, () -> b.forceAttack("fan"));
		ctx.waitTicks(24);
		int bolts = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, b.getBoundingBox().inflate(40, 20, 40)).size());
		check(bolts >= 3, "the fan fired a spread of bolts (" + bolts + ")");
		shot(ctx, w, "boss-3-fan");
		int f0 = hits(w, b);
		waitRunEnd(ctx, w, b, 100);
		ctx.waitTicks(30);
		check(hits(w, b) > f0, "bolts reach the player (hits " + f0 + " -> " + hits(w, b) + ")");

		// ---- pools: lingering zones ------------------------------------------------------------------------------------------------------------------------
		immortal(w);
		T.onServer(w, () -> b.forceAttack("pools"));
		waitRunEndFrom(ctx, w, b, 17, 100, () -> shot(ctx, w, "boss-4-pools-telegraph"));
		check(T.onServer(w, () -> b.hazards().size()) >= 3, "pools left lingering hazards (" + T.onServer(w, () -> b.hazards().size()) + ")");
		int pl0 = hits(w, b);
		ctx.waitTicks(30);
		check(hits(w, b) > pl0, "standing in a pool keeps hurting (hits " + pl0 + " -> " + hits(w, b) + ")");
		shot(ctx, w, "boss-5-pools-burning");
		T.onServer(w, () -> b.hazards().clear());

		// ---- pillars ------------------------------------------------------------------------------------------------------------------------------------------------
		int pi0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("pillars"));
		waitRunEndFrom(ctx, w, b, 40, 170, () -> shot(ctx, w, "boss-6-pillars"));
		check(hits(w, b) > pi0, "pillars erupting under the player hit (hits " + pi0 + " -> " + hits(w, b) + ")");

		// ---- summon -------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("kill @e[type=ultimatesurvival:ember_imp]");
		ctx.waitTicks(3);
		T.onServer(w, () -> b.forceAttack("summon"));
		waitRunEnd(ctx, w, b, 120);
		check(T.onServer(w, () -> b.minionCount()) >= 2, "summon called imps (" + T.onServer(w, () -> b.minionCount()) + ")");
		shot(ctx, w, "boss-7-imps");
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, b.getBoundingBox().inflate(60, 30, 60),
				e -> e.def().id.equals("ember_imp") && e.isNoGravity()).size()) >= 2, "imps hover (no gravity)");
		server.runCommand("kill @e[type=ultimatesurvival:ember_imp]");

		// a normal hit for comparison (after armor)
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		float n0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 10f));
		float normalHit = n0 - hp(w, b);
		check(normalHit > 5f && normalHit < 9f, "a normal 10-damage hit lands for " + normalHit + " after the boss's armor");
		ctx.waitTicks(25);                                    // let the invulnerability frames lapse
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));

		// ---- charge: dash + vulnerable window ---------------------------------------------------------------------------------------------------------------------------------
		immortal(w);
		face(w, b, 16);
		ctx.waitTicks(5);
		double bx0 = T.onServer(w, () -> b.getX());
		T.onServer(w, () -> b.forceAttack("charge"));
		waitRunEndFrom(ctx, w, b, 27, 40, () -> shot(ctx, w, "boss-8-charge-telegraph"));
		ctx.waitTicks(1);
		check(T.onServer(w, () -> b.getX()) > bx0 + 6, "the charge carried the boss forward (" + bx0 + " -> " + T.onServer(w, () -> b.getX()) + ")");
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)), "after the charge the boss is stunned and vulnerable");
		float v0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 10f));
		float dealt = v0 - hp(w, b);
		check(dealt > 12f, "vulnerable boss takes extra damage (10 -> " + dealt + " after armor, x1.6)");
		check(dealt > normalHit * 1.5f, "that is well above a normal hit (" + normalHit + ")");
		shot(ctx, w, "boss-9-stunned");
		waitRunEnd(ctx, w, b, 160);
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_VULN)) == false, "the stun wears off");

		// ---- ring: jump it or take it ----------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		face(w, b, -10);
		ctx.waitTicks(5);
		int r0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("ring"));
		waitRunEndFrom(ctx, w, b, 36, 120, () -> shot(ctx, w, "boss-10-ring"));
		check(hits(w, b) == r0 + 1, "the expanding ring reached a grounded player (hits " + r0 + " -> " + hits(w, b) + ")");
		face(w, b, -10, 2.5);
		T.onServer(w, () -> T.player(w).setNoGravity(true));
		int r1 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("ring"));
		waitRunEnd(ctx, w, b, 160);
		check(hits(w, b) == r1, "jumping the ring avoids it (hits " + r1 + " -> " + hits(w, b) + ")");
		T.onServer(w, () -> T.player(w).setNoGravity(false));
		face(w, b, -8);
		ctx.waitTicks(8);

		// ---- meteors ------------------------------------------------------------------------------------------------------------------------------------------------------------------
		int m0 = hits(w, b);
		T.onServer(w, () -> b.forceAttack("meteors"));
		waitRunEndFrom(ctx, w, b, 38, 270, () -> shot(ctx, w, "boss-11-meteors"));
		check(hits(w, b) > m0, "meteors landing on the player hit (hits " + m0 + " -> " + hits(w, b) + ")");

		// ---- spiral -------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		immortal(w);
		T.onServer(w, () -> b.hazards().clear());
		T.onServer(w, () -> b.forceAttack("spiral"));
		ctx.waitTicks(60);
		int spiral = T.onServer(w, () -> T.level(w).getEntitiesOfClass(Bolt.class, b.getBoundingBox().inflate(60, 20, 60)).size());
		check(spiral >= 12, "the spiral fills the arena with bolts (" + spiral + ")");
		shot(ctx, w, "boss-12-spiral");
		waitRunEnd(ctx, w, b, 200);
		server.runCommand("kill @e[type=ultimatesurvival:bolt]");

		// ---- overheat ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		T.onServer(w, () -> b.setHealth(b.getMaxHealth()));
		T.onServer(w, () -> b.forceAttack("overheat"));
		ctx.waitTicks(40);
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_HOT)), "overheat raises the hot flag");
		float o0 = hp(w, b);
		T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(T.player(w)), 10f));
		check(o0 - hp(w, b) > normalHit * 1.3f, "an overheated boss takes extra damage (" + (o0 - hp(w, b)) + " vs " + normalHit + ")");
		shot(ctx, w, "boss-13-overheat");
		T.onServer(w, () -> b.cancelRun());
		check(T.onServer(w, () -> b.hasFlag(UsmMob.F_HOT)) == false, "cancelling the run cleans up the hot state");
		mortal(w);

		// ---- death rewards ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		check(!Gates.isCleared("ignarok"), "Ignarok's gate is closed before the kill");
		T.onServer(w, () -> b.forceAttack("summon"));
		ctx.waitTicks(80);
		T.onServer(w, () -> b.kill(T.level(w)));
		ctx.waitTicks(8);
		check(Gates.isCleared("ignarok"), "killing Ignarok opens the tier-3 gate");
		check(T.onServer(w, () -> b.minionCount()) == 0, "its minions vanish with it");
		T.takeShot(ctx, "boss-14-death");
		ctx.waitTicks(100);
		check(T.onServer(w, () -> b.isRemoved()), "the death sequence ends with the boss removed");
		int bags = T.onServer(w, () -> T.level(w).getEntitiesOfClass(ItemEntity.class, b.getBoundingBox().inflate(40, 20, 40),
				e -> e.getItem().getItem() == UsmItems.get("ignarok_bag")).size());
		check(bags == 1, "a treasure bag dropped (" + bags + ")");

		server.runCommand("usm unlock ender_dragon");
		// ---- the bag opens into the loot rows -----------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("clear @a");
		T.hold(w, "ignarok_bag");
		AbilityTests.use(w);
		ctx.waitTicks(3);
		check(T.count(w, "ember_core") >= 8, "opening the bag yields 8+ Ember Cores (" + T.count(w, "ember_core") + ")");
		check(T.count(w, "heart_of_ignarok") == 1, "and Ignarok's unique relic");
		check(T.count(w, "ignarok_bag") == 0, "the bag is consumed");
		server.runCommand("clear @a");

		ritual(ctx, w, x, y, z);
		mortal(w);
	}

	private static void ritual(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z) {
		var server = w.getServer();
		server.runCommand("usm reset");
		server.runCommand("kill @e[type=ultimatesurvival:ignarok]");
		BlockPos altar = BlockPos.containing(x + 10, y, z + 10);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		T.stand(w, altar.getX() + 0.5 - 4, y, altar.getZ() + 0.5, altar.getX() + 0.5, y + 1, altar.getZ() + 0.5);
		ctx.waitTicks(10);
		T.takeShot(ctx, "boss-15-altar");

		MobDef def = Mobs.DEFS.get("ignarok");
		ItemStack sigil = new ItemStack(UsmItems.get("cinder_sigil"));
		T.onServer(w, () -> T.player(w).setItemInHand(InteractionHand.MAIN_HAND, sigil.copy()));
		ServerPlayer p = T.onServer(w, () -> T.player(w));

		// wrong dimension in the test world: refused
		UsmConfig.get().ritualsAnywhere = false;
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "the Nether-only altar refuses the Overworld");
		// a locked sigil (dragon not beaten) is refused by the block itself
		BlockHitResult hitAt = new BlockHitResult(Vec3.atCenterOf(altar).add(0, 0.4, 0), net.minecraft.core.Direction.UP, altar, false);
		T.onServer(w, () -> T.level(w).getBlockState(altar).useItemOn(p.getMainHandItem(), T.level(w), p, InteractionHand.MAIN_HAND, hitAt));
		check(T.onServer(w, () -> p.getMainHandItem().getCount()) == 1, "a refused ritual does not consume the sigil");
		check(T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(30)).isEmpty()), "nothing spawned");

		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(3);
		UsmConfig.get().ritualsAnywhere = true;
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) == null, "with the dimension rule relaxed the ritual may start");
		T.onServer(w, () -> T.level(w).getBlockState(altar).useItemOn(p.getMainHandItem(), T.level(w), p, InteractionHand.MAIN_HAND, hitAt));
		check(T.onServer(w, () -> p.getMainHandItem().isEmpty()), "starting the ritual consumes the sigil");
		check(Ritual.busy(altar), "the altar is busy during the ritual");
		ctx.waitTicks(50);
		T.takeShot(ctx, "boss-16-ritual");
		ctx.waitTicks(Ritual.LENGTH - 50 + 2);
		var found = T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(altar).inflate(10)));
		check(found.size() == 1, "the ritual summoned Ignarok (" + found.size() + ")");
		UsmBoss b = found.get(0);
		check(T.onServer(w, () -> b.isSpawning()), "it emerges with a spawn animation");
		check(T.onServer(w, () -> b.hurtServer(T.level(w), T.level(w).damageSources().playerAttack(p), 50f)) == false, "it is invulnerable while emerging");
		ctx.waitTicks(12);
		T.takeShot(ctx, "boss-17-emerging");
		for (int i = 0; i < 150 && T.onServer(w, () -> b.isSpawning()); i++) {          // the emergence is quicker now: wait for it to end instead of for a fixed time
			ctx.waitTicks(1);
		}
		check(T.onServer(w, () -> !b.isSpawning()), "the emergence finishes");
		T.takeShot(ctx, "boss-18-awake");

		// a second ritual while it lives is refused
		T.onServer(w, () -> p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UsmItems.get("cinder_sigil"))));
		check(T.onServer(w, () -> Ritual.check(T.level(w), altar, p, def)) != null, "a second Ignarok cannot be summoned while the first lives");
		UsmConfig.get().ritualsAnywhere = false;

		// breaking the altar mid-ritual returns the sigil
		T.onServer(w, () -> b.discard());
		BlockPos altar2 = altar.east(6);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar2, UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState()));
		UsmConfig.get().ritualsAnywhere = true;
		T.onServer(w, () -> Ritual.begin(T.level(w), altar2, p, p.getMainHandItem(), def));
		ctx.waitTicks(20);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(altar2, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
		ctx.waitTicks(6);
		int refunded = T.onServer(w, () -> T.level(w).getEntitiesOfClass(ItemEntity.class, new AABB(altar2).inflate(4),
				e -> e.getItem().getItem() == UsmItems.get("cinder_sigil")).size());
		check(refunded == 1, "breaking the altar mid-ritual drops the sigil back (" + refunded + ")");
		check(!Ritual.busy(altar2), "and frees the altar");
		UsmConfig.get().ritualsAnywhere = false;
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("kill @e[type=ultimatesurvival:ignarok]");
	}
}
