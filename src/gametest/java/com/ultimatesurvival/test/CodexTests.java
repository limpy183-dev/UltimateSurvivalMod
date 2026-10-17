package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.client.codex.CodexScreen;
import com.ultimatesurvival.codex.Codex;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.UsmState;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;

/** The Codex: handout, discovery, recovery, opening, interaction and a screenshot of every page. */
final class CodexTests {
	private static final String SCREEN = "com.ultimatesurvival.client.codex.CodexScreen";

	private CodexTests() {
	}

	/** Runs right after the world is created, before other suites clear the inventory. */
	static void handout(ClientGameTestContext ctx, TestSingleplayerContext w) {
		check(T.count(w, "codex") == 1, "the Codex was handed to the player on first join (" + T.count(w, "codex") + ")");
		check(T.onServer(w, () -> UsmState.progress().hasReceivedCodex(T.player(w).getUUID())), "the handout is remembered in the world data");
		check(!T.onServer(w, () -> Codex.giveOnFirstJoin(T.player(w))), "a second join does not hand out another Codex");
		check(T.count(w, "codex") == 1, "still exactly one Codex after the second attempt");
	}

	@SuppressWarnings("unchecked")
	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) throws Exception {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		// ---- recovery: craftable, and a command ----------------------------------------------------------------------
		ItemStack made = T.craft(w, new String[] {"minecraft:book", "minecraft:redstone", "", "", "", "", "", "", ""});
		check(made.getItem() == UsmItems.get("codex"), "a Book + Redstone crafts a new Codex (recovery recipe)");
		server.runCommand("usm codex");
		ctx.waitTicks(2);
		check(T.count(w, "codex") == 1, "/usm codex gives a fresh Codex");

		// ---- discovery ---------------------------------------------------------------------------------------------------
		check(!T.onServer(w, () -> UsmState.progress().isDiscovered("item:verdite_ingot")), "nothing is discovered in a fresh world");

		// ---- Tier 1 is listed from the first minute (faded until you own a piece); later tiers stay "???" ------------------------------------------------------
		var fresh = CodexScreen.entryStates();
		java.util.Set<String> tierOne = tierOneIds();
		check(tierOne.size() >= 20 && fresh.keySet().containsAll(tierOne), "the Arsenal lists every Tier 1 item and block (" + tierOne.size() + " entries)");
		check(tierOne.stream().allMatch(id -> fresh.get(id).equals("faded")), "all of them are open but faded in a fresh world (not collected yet)");
		check(fresh.entrySet().stream().filter(e -> !tierOne.contains(e.getKey())).allMatch(e -> e.getValue().equals("hidden")), "every later tier is still hidden behind '???'");
		check(fresh.get("verdite_pickaxe").equals("faded") && fresh.get("verdite_ore").equals("faded") && fresh.get("azurite_pickaxe").equals("hidden"), "e.g. the Verdite pickaxe and ore are faded, the Azurite pickaxe hidden");
		server.runCommand("give @a ultimatesurvival:verdite_ingot 3");
		server.runCommand("give @a ultimatesurvival:cinder_brand");          // tier 3: locked, but obtaining it still counts
		server.runCommand("give @a ultimatesurvival:verdite_ore");           // block item
		ctx.waitTicks(30);
		var pr = UsmState.progress();
		check(pr.isDiscovered("item:verdite_ingot"), "carrying a Verdite Ingot discovers its entry");
		check(pr.isDiscovered("item:cinder_brand"), "a locked item is discovered the moment you obtain it");
		check(pr.isDiscovered("item:verdite_ore"), "block items are discovered too");
		check(!pr.isDiscovered("item:azurite_ingot"), "items you never held stay hidden");
		check(!pr.isDiscovered("item:bolt_ember"), "hidden projectile sprites never appear in the Codex");
		var held = CodexScreen.entryStates();
		check(held.get("verdite_ingot").equals("collected") && held.get("verdite_ore").equals("collected"), "what you carry turns from faded to collected");
		check(held.get("verdite_pickaxe").equals("faded") && held.get("healing_salve").equals("faded"), "the Tier 1 pieces you do not own stay faded");
		check(held.get("cinder_brand").equals("collected") && held.get("azurite_ingot").equals("hidden"), "a later-tier item opens only once you hold it");

		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		EnderDragon dragon = T.onServer(w, () -> {
			EnderDragon d = new EnderDragon(EntityTypes.ENDER_DRAGON, T.level(w));
			d.snapTo(me.getX() + 15, me.getY() + 4, me.getZ(), 0, 0);
			T.level(w).addFreshEntity(d);
			return d;
		});
		ctx.waitTicks(30);
		check(pr.isDiscovered("boss:ender_dragon"), "meeting the Ender Dragon discovers its page");
		T.onServer(w, () -> dragon.discard());

		// ---- tooltips carry the generated ability + set text -------------------------------------------------------
		String tip = T.tooltip(ctx, new ItemStack(UsmItems.get("wyrmfang")));
		check(tip.contains("Right-click: Dash Strike") && tip.contains("Cooldown: " + T.g(T.ab("wyrmfang", "dash_strike", "cd") / 20) + "s")
				&& tip.contains(T.g(T.ab("wyrmfang", "dash_strike", "dist")) + " blocks"),
				"Wyrmfang tooltip lists Dash Strike with its real numbers: " + tip.replace('\n', '|'));
		String setTip = T.tooltip(ctx, new ItemStack(UsmItems.get("verdite_helmet")));
		check(setTip.contains("Verdite Armor (0/4)") && setTip.contains("Verdant Resolve"), "armor tooltip shows set progress and bonus: " + setTip.replace('\n', '|'));

		// ---- opening the screen -----------------------------------------------------------------------------------------------
		server.runCommand("give @a ultimatesurvival:azurite_pickaxe");
		server.runCommand("give @a ultimatesurvival:verdite_helmet");
		server.runCommand("give @a ultimatesurvival:verdite_sword");
		server.runCommand("give @a ultimatesurvival:drakescale");
		server.runCommand("give @a ultimatesurvival:healing_salve");
		server.runCommand("give @a ultimatesurvival:azurite_helmet");
		ctx.waitTicks(30);

		T.hold(w, "codex");
		ctx.waitTicks(3);
		ctx.runOnClient(mc -> {
			ItemStack st = mc.player.getMainHandItem();
			st.use(mc.level, mc.player, InteractionHand.MAIN_HAND);
		});
		Class<? extends Screen> cls = (Class<? extends Screen>) Class.forName(SCREEN);
		ctx.waitForScreen(cls);
		ctx.waitTicks(2);
		T.takeShot(ctx, "codex-0-opening");
		ctx.waitTicks(25);
		T.takeShot(ctx, "codex-1-items");

		// ---- the scrollbars: grab the thumb and drag, click the bare track, the wheel still works ---------------------------------------------------------------------------------
		int[] bar = bar(ctx, 0);
		check(bar != null && bar[6] > 0, "the Arsenal list draws a scrollbar (" + java.util.Arrays.toString(bar) + ")");
		double scale = ctx.computeOnClient(mc -> mc.getWindow().getGuiScale());
		int bx = bar[0] + 1;
		ctx.getInput().setCursorPos(bx * scale, (bar[3] + bar[4] / 2) * scale);
		ctx.waitTicks(2);
		ctx.getInput().holdMouse(0);
		ctx.waitTicks(2);
		ctx.getInput().setCursorPos(bx * scale, (bar[1] + (bar[2] - bar[1]) / 2) * scale);              // half way down the track
		ctx.waitTicks(4);
		int[] half = bar(ctx, 0);
		check(Math.abs(half[5] - half[6] / 2) <= 2, "dragging the thumb with the real mouse scrolls the list to match (" + half[5] + " of " + half[6] + ")");
		ctx.getInput().setCursorPos(bx * scale, (bar[2] + 40) * scale);                                   // far past the end: clamps
		ctx.waitTicks(4);
		int[] end = bar(ctx, 0);
		check(end[5] == end[6], "dragging past the bottom stops at the last row (" + end[5] + " of " + end[6] + ")");
		ctx.getInput().releaseMouse(0);
		ctx.waitTicks(2);
		ctx.getInput().setCursorPos(bx * scale, (bar[1] + 10) * scale);
		ctx.waitTicks(3);
		check(bar(ctx, 0)[5] == end[6], "once released, moving the mouse no longer drags");
		clickAt(ctx, bx + 2, bar[1] + 3);                                                                  // the bare track near the top: the thumb jumps there
		ctx.waitTicks(2);
		check(bar(ctx, 0)[5] <= 2, "clicking the track near the top jumps the list back to the start (" + bar(ctx, 0)[5] + ")");
		ctx.runOnClient(mc -> mc.gui.screen().mouseScrolled(0, 0, 0, -1));
		ctx.waitTicks(2);
		check(bar(ctx, 0)[5] == 1, "the mouse wheel still scrolls one row at a time");
		ctx.runOnClient(mc -> mc.gui.screen().mouseScrolled(0, 0, 0, 1));

		ctx.runOnClient(mc -> select(mc.gui.screen(), "verdite_helmet"));
		ctx.waitTicks(3);
		int[] page = bar(ctx, 1);
		check(page != null && page[6] > 0, "a long entry draws a scrollbar on the right-hand page (" + java.util.Arrays.toString(page) + ")");
		ctx.getInput().setCursorPos((page[0] + 1) * scale, (page[3] + page[4] / 2) * scale);
		ctx.waitTicks(2);
		ctx.getInput().holdMouse(0);
		ctx.waitTicks(2);
		ctx.getInput().setCursorPos((page[0] + 1) * scale, (page[2] + 30) * scale);
		ctx.waitTicks(4);
		ctx.getInput().releaseMouse(0);
		int[] pageEnd = bar(ctx, 1);
		check(pageEnd[5] == pageEnd[6] && pageEnd[6] > 0, "dragging the page's thumb to the bottom scrolls the entry to its end (" + pageEnd[5] + " of " + pageEnd[6] + ")");
		T.takeShot(ctx, "codex-1b-scrolled-page");
		ctx.runOnClient(mc -> select(mc.gui.screen(), "verdite_ingot"));

		// real mouse click on the 'Sovereigns' tab (GUI scale 2 in the 854x480 test window)
		int[] tab = ctx.computeOnClient(mc -> {
			Screen s = mc.gui.screen();
			double sc = mc.getWindow().getGuiScale();
			int gx = (mc.getWindow().getGuiScaledWidth() - 360) / 2 - 20 + 13;
			int gy = (mc.getWindow().getGuiScaledHeight() - 220) / 2 + 14 + 13;
			return new int[] {(int) (gx * sc), (int) (gy * sc)};
		});
		ctx.getInput().setCursorPos(tab[0], tab[1]);
		ctx.waitTicks(2);
		ctx.getInput().pressMouse(0);
		ctx.waitTicks(3);
		int idx = ctx.computeOnClient(mc -> screenTab(mc.gui.screen()));
		check(idx == 0, "clicking the first tab switched to the Sovereigns page (tab " + idx + ")");
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-2-bosses");

		ctx.runOnClient(mc -> select(mc.gui.screen(), "ignarok"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-3-boss-locked-tips");
		server.runCommand("usm unlock ignarok");
		ctx.waitTicks(3);
		ctx.runOnClient(mc -> select(mc.gui.screen(), "ignarok"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-4-boss-defeated");
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 1));
		ctx.runOnClient(mc -> select(mc.gui.screen(), "azurite_pickaxe"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-5-item-recipe");
		ctx.runOnClient(mc -> select(mc.gui.screen(), "verdite_pickaxe"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-5b-faded-item");
		ctx.runOnClient(mc -> select(mc.gui.screen(), "verdite_helmet"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-6-armor-set");
		ctx.runOnClient(mc -> select(mc.gui.screen(), "verdite_ore"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-7-block");
		ctx.runOnClient(mc -> select(mc.gui.screen(), "azurite_ingot"));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-8-unknown");

		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 3));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-9-progress");
		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 4));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-10-tips");
		int[] tipBar = bar(ctx, 3);
		check(tipBar != null && tipBar[6] > 0, "the Tips tab has a scrollbar too (" + java.util.Arrays.toString(tipBar) + ")");
		clickAt(ctx, tipBar[0] + 1, tipBar[2] - 3);                                                        // the bare track near the bottom
		ctx.waitTicks(2);
		check(bar(ctx, 3)[5] >= tipBar[6] - 2, "clicking the track scrolls the tips to the end (" + bar(ctx, 3)[5] + " of " + tipBar[6] + ")");
		T.takeShot(ctx, "codex-10b-tips-end");
		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 4));
		server.runCommand("usm discover mob:ember_imp");
		server.runCommand("usm discover boss:ignarok");
		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 2));
		ctx.runOnClient(mc -> select(mc.gui.screen(), "ember_imp"));
		ctx.waitTicks(45);
		T.takeShot(ctx, "codex-11-mobs");
		ctx.runOnClient(mc -> setTab(mc.gui.screen(), 0));
		ctx.runOnClient(mc -> select(mc.gui.screen(), "ignarok"));
		server.runCommand("usm unlock ignarok");
		ctx.waitTicks(60);
		T.takeShot(ctx, "codex-12-boss-preview");
		ctx.runOnClient(mc -> mc.gui.screen().mouseScrolled(0, 0, 0, -6));
		ctx.waitTicks(3);
		T.takeShot(ctx, "codex-13-boss-attacks");
		server.runCommand("usm reset");
		ctx.waitTicks(3);

		// the key closes it again, and opens it when the book is carried
		ctx.getInput().pressKey(com.ultimatesurvival.client.UsmKeys.codex);
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "pressing J inside the Codex closes it");
		ctx.getInput().pressKey(com.ultimatesurvival.client.UsmKeys.codex);
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> mc.gui.screen() != null && mc.gui.screen().getClass().getName().equals(SCREEN)), "pressing J opens the Codex when you carry it");
		ctx.runOnClient(mc -> mc.gui.setScreen(null));

		server.runCommand("clear @a ultimatesurvival:codex");
		ctx.waitTicks(3);
		ctx.getInput().pressKey(com.ultimatesurvival.client.UsmKeys.codex);
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "without the book, J does not open the Codex");
		server.runCommand("usm codex");
		ctx.waitTicks(2);
	}

	/** The ids of every Tier 1 item and block: the start gate's. */
	private static java.util.Set<String> tierOneIds() {
		java.util.Set<String> out = new java.util.LinkedHashSet<>();
		for (var d : Content.ITEMS) {
			if (!d.json.has("hidden") && d.gate.equals("start")) {
				out.add(d.id);
			}
		}
		for (var e : Content.root.getAsJsonArray("blocks")) {
			var b = e.getAsJsonObject();
			if (!b.has("gate") || b.get("gate").getAsString().equals("start")) {
				out.add(b.get("id").getAsString());
			}
		}
		return out;
	}

	/** The scrollbar with this id on the open Codex page: {x, top, bottom, thumbTop, thumbHeight, value, max}, or null. */
	private static int[] bar(ClientGameTestContext ctx, int id) {
		return ctx.computeOnClient(mc -> ((CodexScreen) mc.gui.screen()).scrollbarInfo(id));
	}

	/** A single left click at GUI coordinates, delivered straight to the open screen. */
	private static void clickAt(ClientGameTestContext ctx, int x, int y) {
		ctx.runOnClient(mc -> {
			Screen s = mc.gui.screen();
			s.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
			s.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
		});
	}

	private static int screenTab(Screen s) {
		try {
			return (int) s.getClass().getMethod("tabIndex").invoke(s);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void select(Screen s, String id) {
		try {
			s.getClass().getMethod("select", String.class).invoke(s, id);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void setTab(Screen s, int t) {
		try {
			s.getClass().getMethod("setTabIndex", int.class).invoke(s, t);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
