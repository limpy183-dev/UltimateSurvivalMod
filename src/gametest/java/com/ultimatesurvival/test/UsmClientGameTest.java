package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Runs the real client. Each phase of the build adds its checks here; a failed check throws and
 * fails the Gradle task, so "verified" in the README means this class passed.
 */
@SuppressWarnings("UnstableApiUsage")
public class UsmClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		com.ultimatesurvival.config.UsmConfig.get().vanillaMobScalingPerTier = 0;     // suites assume 20-HP husks; MobTests switches scaling on itself
		com.ultimatesurvival.config.UsmConfig.get().invasionsEnabled = false;         // invasions are started by hand
		context.runOnClient(mc -> {
			mc.options.pauseOnLostFocus = false;                                      // a long run must not freeze because somebody clicked another window
			mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
			mc.options.enableVsync().set(false);                                      // a window hidden behind others is presented at a crawl otherwise, and every tick waits for a frame
		});
		if ("worldgen".equals(System.getenv("USM_ONLY"))) {
			worldgen(context);                                      // never returns: it ends the run
		}
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			world.getConnection().waitForChunksRender();
			String only = System.getenv("USM_ONLY");           // e.g. USM_ONLY=boss,fire re-runs just those suites while iterating
			boolean all = only == null || only.isEmpty();
			if (all) {
				CodexTests.handout(context, world);
				phase1(context, world);
			}
			try {
				if (all || want(only, "tools")) {
					quiet();
					ToolTests.run(context, world);
				}
				if (all || want(only, "ability")) {
					quiet();
					AbilityTests.run(context, world);
				}
				if (all || want(only, "wear")) {
					quiet();
					WearTests.run(context, world);
				}
				if (all || want(only, "fire")) {
					quiet();
					FireTests.run(context, world);
				}
				if (all || want(only, "rules")) {
					quiet();
					RulesTests.run(context, world);
				}
				if (all || want(only, "codex")) {
					quiet();
					CodexTests.run(context, world);
				}
				if (all || want(only, "cheats")) {
					quiet();
					CheatTests.run(context, world);
				}
				if (all || want(only, "boss")) {
					quiet();
					BossTests.run(context, world);
				}
				if (all || want(only, "boss2")) {
					quiet();
					Boss2Tests.run(context, world);
				}
				if (all || want(only, "boss3")) {
					quiet();
					Boss3Tests.run(context, world);
				}
				if (all || want(only, "boss4")) {
					quiet();
					Boss4Tests.run(context, world);
				}
				if (all || want(only, "t5")) {
					quiet();
					T5Tests.run(context, world);
				}
				if (all || want(only, "t6")) {
					quiet();
					T6Tests.run(context, world);
				}
				if (all || want(only, "polish")) {
					quiet();
					PolishTests.run(context, world);
				}
				if (all || want(only, "mobs")) {
					quiet();
					MobTests.run(context, world);
				}
				if (want(only, "soak")) {                          // long: natural fights at server-sprint speed, so it only runs when asked for
					quiet();
					SoakTests.run(context, world);
				}
			} catch (Throwable t) {
				fail(t);
			}
			world.getServer().runCommand("usm reset");
			world.getServer().runCommand("clear @a");
			check(world.getServer().computeOnServer(s -> com.ultimatesurvival.codex.Codex.giveOnFirstJoin(world.getConnection().getServerPlayer())),
					"after a reset the Codex is handed out again");
			world.getServer().runCommand("usm unlock voltaris");   // must survive the reload below
			world.getServer().runCommand("usm discover item:verdite_ingot");
			context.waitTicks(5);
			if (all) {
				PersistTests.prepare(context, world);               // a wounded boss in the arena when the world is saved
			}
			if (!all) {                                         // partial run: closing the world here can deadlock inside the test framework - report success and leave
				com.ultimatesurvival.UltimateSurvival.LOGGER.info("[gametest] partial run complete");
				System.out.flush();
				Runtime.getRuntime().halt(0);
			}
		}
		if (System.getenv("USM_ONLY") != null && !System.getenv("USM_ONLY").isEmpty()) {
			return;                                             // partial run: skip the persistence checks that need every suite
		}
		// reopen the same world: progression has to come back from disk
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			check(Gates.isCleared("voltaris"), "voltaris gate persisted across save/load");
			check(com.ultimatesurvival.progress.UsmState.progress().isDiscovered("item:verdite_ingot"), "codex discoveries persisted across save/load");
			check(T.count(world, "codex") == 1, "rejoining the world does not hand out a second Codex (" + T.count(world, "codex") + ")");
			check(Gates.isCleared("ender_dragon") && Gates.isCleared("ignarok"), "earlier gates persisted too");
			check(!Gates.isCleared("hemorath"), "later gates stayed locked after reload");
		PersistTests.verify(context, world);
			world.getServer().runCommand("usm reset");
			context.waitTicks(2);
			check(!Gates.isCleared("voltaris"), "reset clears progression");
			context.takeScreenshot("final-after-reload");
		}
		worldgen(context);                                          // the last act of a full run
	}

	/** Natural world generation needs the ordinary generator, so it gets a world of its own (fixed seed). */
	private static void worldgen(ClientGameTestContext context) {
		try (TestSingleplayerContext gen = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> s.setSeed("ultimate-survival")).create()) {
			gen.getConnection().waitForChunksRender();
			try {
				WorldgenTests.run(context, gen);
			} catch (Throwable t) {
				fail(t);
			}
			context.takeScreenshot("worldgen-done");
			com.ultimatesurvival.UltimateSurvival.LOGGER.info("[gametest] last suite complete");
			System.out.flush();
			Runtime.getRuntime().halt(0);                           // the last world: skip the framework's teardown, which can deadlock after a long run - every check has passed
		}
	}

	/** The two switches every suite assumes: no random invasions and no vanilla-mob scaling (the creative "reset cheats" action puts both back to their defaults). */
	private static void quiet() {
		com.ultimatesurvival.config.UsmConfig.get().vanillaMobScalingPerTier = 0;
		com.ultimatesurvival.config.UsmConfig.get().invasionsEnabled = false;
	}

	/** A failed check inside the world would deadlock the client's teardown: report it and exit non-zero right away. */
	private static void fail(Throwable t) {
		com.ultimatesurvival.UltimateSurvival.LOGGER.error("GAMETEST FAILED: {}", t.toString(), t);
		System.err.println("GAMETEST FAILED: " + t);
		System.err.flush();
		Runtime.getRuntime().halt(1);
	}

	/** True when the USM_ONLY list (comma separated suite names) contains {@code name}. */
	private static boolean want(String only, String name) {
		return only != null && java.util.Arrays.asList(only.split(",")).contains(name);
	}

	private void phase1(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		check(UsmItems.BY_ID.size() >= 10, "items registered: " + UsmItems.BY_ID.size());
		check(!Gates.isCleared("ender_dragon"), "fresh world starts with only the first gate");
		check(Gates.currentTier() == 1, "fresh world is tier 1");

		// tooltip: locked item names its boss, unlocked one does not
		ItemStack cinder = new ItemStack(UsmItems.get("cinder_brand"));
		String lockedTip = tooltip(context, cinder);
		check(lockedTip.contains("Locked") && lockedTip.contains("Ignarok"), "locked tooltip names the boss: " + lockedTip.replace('\n', '|'));
		check(lockedTip.contains("Tier 3"), "tooltip shows the tier");

		server.runCommand("usm status");
		equip(world, "verdite_sword");
		context.waitTicks(3);
		double t1 = attack(world);
		check(Math.abs(t1 - T.stat("verdite_sword", "damage")) < 1e-6, "verdite sword (start gate) hits for its table damage, got " + t1);

		equip(world, "cinder_brand");
		context.waitTicks(3);
		double locked = attack(world);
		check(Math.abs(locked - 1.0) < 1e-6, "locked cinder brand gives fist damage, got " + locked);

		server.runCommand("usm unlock ignarok");
		context.waitTicks(3);
		double unlocked = attack(world);
		check(Math.abs(unlocked - T.stat("cinder_brand", "damage")) < 1e-6, "unlocked cinder brand hits for its table damage, got " + unlocked);
		check(Gates.currentTier() == 3, "tier 3 after the third gate");
		check(!tooltip(context, cinder).contains("Locked"), "no lock line once unlocked");

		server.runCommand("usm lock 3");
		context.waitTicks(3);
		check(Math.abs(attack(world) - 1.0) < 1e-6, "re-locking removes the damage again");

		server.runCommand("usm unlock 5");
		context.waitTicks(2);
		check(Gates.isCleared("omnarch") && Gates.currentTier() == 5, "unlock 5 clears every gate");
		server.runCommand("usm reset");
		context.waitTicks(3);
		check(!Gates.isCleared("ender_dragon"), "reset re-locks everything");
	}

	private static String tooltip(ClientGameTestContext context, ItemStack stack) {
		return context.computeOnClient(mc -> String.join("\n", stack
				.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL)
				.stream().map(c -> c.getString()).toList()));
	}

	private static void equip(TestSingleplayerContext world, String id) {
		world.getServer().runOnServer(s -> world.getConnection().getServerPlayer()
				.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UsmItems.get(id))));
	}

	private static double attack(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(s -> world.getConnection().getServerPlayer().getAttributeValue(Attributes.ATTACK_DAMAGE));
	}
}
