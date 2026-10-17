package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.cheat.Cheats;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.item.AccessoryMenu;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.UsmState;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** The Creative page of the Codex: it exists only in creative mode, its buttons reach the server, the extra accessory slots work. */
final class CheatTests {
	private static final String SCREEN = "com.ultimatesurvival.client.codex.CodexScreen";

	private CheatTests() {
	}

	@SuppressWarnings("unchecked")
	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) throws Exception {
		var server = w.getServer();
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		server.runCommand("gamemode creative @a");
		Cheats.resetValues();
		ctx.waitTicks(3);
		check(Cheats.ALL.size() >= 25, "the Creative page lists many cheats (" + Cheats.ALL.size() + ")");

		// ---- the page, through the real client --------------------------------------------------------------------------------
		server.runCommand("give @a ultimatesurvival:codex");
		ctx.waitTicks(3);
		T.hold(w, "codex");
		ctx.waitTicks(3);
		ctx.runOnClient(mc -> mc.player.getMainHandItem().use(mc.level, mc.player, InteractionHand.MAIN_HAND));
		Class<? extends Screen> cls = (Class<? extends Screen>) Class.forName(SCREEN);
		ctx.waitForScreen(cls);
		ctx.waitTicks(14);
		Screen s = ctx.computeOnClient(mc -> mc.gui.screen());
		call(s, "setTabIndex", 5);
		call(s, "select", "acc_slots");
		ctx.waitTicks(4);
		check((int) call(s, "tabIndex") == 5, "creative mode shows the Creative tab");
		T.takeShot(ctx, "cheats-1-slots");

		// ---- ">>" on the slot stepper: five more slots, saved by the server ------------------------------------------------
		List<int[]> btns = (List<int[]>) call(s, "buttons");
		check(btns.size() == 4, "a number cheat has four step buttons (" + btns.size() + ")");
		click(ctx, btns.get(3));
		ctx.waitTicks(4);
		check(UsmConfig.get().accessorySlots == 11, "pressing >> raises the accessory slots from 6 to 11 (" + UsmConfig.get().accessorySlots + ")");
		check(Cheats.valueText(Cheats.find("acc_slots")).equals("11"), "the list shows the new value");

		// ---- a toggle and an action -----------------------------------------------------------------------------------------------
		call(s, "select", "no_cooldowns");
		ctx.waitTicks(3);
		btns = (List<int[]>) call(s, "buttons");
		check(btns.size() == 1, "a toggle has one button");
		click(ctx, btns.get(0));
		ctx.waitTicks(4);
		check(UsmConfig.get().noAbilityCooldowns, "the toggle reaches the server and turns on");
		T.takeShot(ctx, "cheats-2-toggle");

		call(s, "select", "unlock");
		ctx.waitTicks(3);
		btns = (List<int[]>) call(s, "buttons");
		check(btns.size() == 3, "a choice action has <, > and Do it (" + btns.size() + ")");
		click(ctx, btns.get(1));                                                              // next target: Tier 2
		ctx.waitTicks(2);
		click(ctx, btns.get(1));                                                              // Tier 3
		ctx.waitTicks(2);
		btns = (List<int[]>) call(s, "buttons");
		click(ctx, btns.get(2));
		ctx.waitTicks(4);
		check(Gates.isCleared("ender_dragon") && Gates.isCleared("ignarok") && !Gates.isCleared("voltaris"), "Unlock > Tier 3 opens the dragon and Ignarok gates only");
		T.takeShot(ctx, "cheats-3-unlock");

		call(s, "select", "discover");
		ctx.waitTicks(3);
		btns = (List<int[]>) call(s, "buttons");
		click(ctx, btns.get(0));
		ctx.waitTicks(4);
		check(UsmState.progress().isDiscovered("item:azurite_ingot") && UsmState.progress().isDiscovered("boss:omnarch"), "Reveal Entire Codex discovers everything");

		// ---- the extra slots in the real accessory menu -----------------------------------------------------------------
		ctx.runOnClient(mc -> mc.gui.setScreen(null));
		ctx.waitTicks(2);
		T.onServer(w, () -> com.ultimatesurvival.item.Accessories.open(T.player(w)));
		ctx.waitForScreen((Class<? extends Screen>) Class.forName("com.ultimatesurvival.client.gui.AccessoryScreen"));
		ctx.waitTicks(4);
		var menu = T.onServer(w, () -> (AccessoryMenu) T.player(w).containerMenu);
		check(menu.slotCount == 11 && menu.slots.size() == 11 + 36, "the accessory menu has 11 charm slots (" + menu.slotCount + ", " + menu.slots.size() + " slots)");
		T.onServer(w, () -> menu.getSlot(9).set(new ItemStack(UsmItems.get("verdite_ring"))));
		ctx.waitTicks(15);
		check(T.onServer(w, () -> com.ultimatesurvival.item.Accessories.equipped(T.player(w)).get(9).getItem() == UsmItems.get("verdite_ring")),
				"an item in an extra slot is stored");
		T.near(T.onServer(w, () -> T.player(w).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)), 2.0, 1e-6, "and its armor bonus applies");
		T.takeShot(ctx, "cheats-4-accessories");

		// a taller panel: 24 slots = three extra rows
		ctx.runOnClient(mc -> mc.gui.setScreen(null));
		ctx.waitTicks(2);
		UsmConfig.get().accessorySlots = 24;
		T.onServer(w, () -> com.ultimatesurvival.item.Accessories.open(T.player(w)));
		ctx.waitForScreen((Class<? extends Screen>) Class.forName("com.ultimatesurvival.client.gui.AccessoryScreen"));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> ((AccessoryMenu) T.player(w).containerMenu).slots.size()) == 24 + 36, "24 slots open a menu with three extra rows");
		T.takeShot(ctx, "cheats-5-many-slots");

		// the maximum: 70 slots = eight extra rows (the last holds one slot)
		ctx.runOnClient(mc -> mc.gui.setScreen(null));
		ctx.waitTicks(2);
		UsmConfig.get().accessorySlots = 999;
		UsmConfig.get().sanitize();
		check(UsmConfig.get().accessorySlots == 70, "the accessory slot count is capped at 70 (" + UsmConfig.get().accessorySlots + ")");
		T.onServer(w, () -> com.ultimatesurvival.item.Accessories.open(T.player(w)));
		ctx.waitForScreen((Class<? extends Screen>) Class.forName("com.ultimatesurvival.client.gui.AccessoryScreen"));
		ctx.waitTicks(4);
		check(T.onServer(w, () -> ((AccessoryMenu) T.player(w).containerMenu).slots.size()) == 70 + 36, "70 slots open a menu with eight extra rows");
		T.takeShot(ctx, "cheats-5b-max-slots");

		// lowering the cheat keeps the item but it stops counting
		UsmConfig.get().accessorySlots = 6;
		ctx.waitTicks(15);
		T.near(T.onServer(w, () -> T.player(w).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)), 0.0, 1e-6, "back at six slots the extra ring stops counting");
		check(UsmState.progress().accessories(T.onServer(w, () -> T.player(w).getUUID())).get(9).getItem() == UsmItems.get("verdite_ring"), "but it is not deleted");
		ctx.runOnClient(mc -> mc.gui.setScreen(null));

		// ---- summon / dismiss ----------------------------------------------------------------------------------------------------
		server.runCommand("difficulty normal");
		T.onServer(w, () -> Cheats.apply(T.player(w), "summon", 0));
		ctx.waitTicks(10);
		check(bosses(w) == 1, "Summon a Sovereign spawns one boss without an altar (" + bosses(w) + ")");
		T.onServer(w, () -> Cheats.apply(T.player(w), "dismiss", 0));
		ctx.waitTicks(5);
		check(bosses(w) == 0, "Dismiss Sovereigns removes it again");

		// ---- survival: no page, and the server ignores the packet -----------------------------------------------------------
		server.runCommand("gamemode survival @a");
		ctx.waitTicks(3);
		T.onServer(w, () -> Cheats.apply(T.player(w), "free_rituals", 1));
		check(!UsmConfig.get().freeRituals, "a survival player's cheat request is ignored");
		T.onServer(w, () -> UsmState.progress().accessories(T.player(w).getUUID()).set(9, ItemStack.EMPTY));
		server.runCommand("gamemode creative @a");
		T.onServer(w, () -> Cheats.apply(T.player(w), "free_rituals", 1));
		check(UsmConfig.get().freeRituals, "the same request from creative works");
		Cheats.resetValues();
		check(!UsmConfig.get().freeRituals && UsmConfig.get().accessorySlots == 6 && !UsmConfig.get().noAbilityCooldowns, "Reset restores the defaults");
		server.runCommand("gamemode survival @a");
		server.runCommand("usm reset");
		ctx.waitTicks(3);
	}

	private static int bosses(TestSingleplayerContext w) {
		return T.onServer(w, () -> {
			int n = 0;
			for (var e : T.level(w).getAllEntities()) {
				n += e instanceof com.ultimatesurvival.mob.UsmBoss ? 1 : 0;
			}
			return n;
		});
	}

	private static void click(ClientGameTestContext ctx, int[] b) {
		double x = b[0] + b[2] / 2.0;
		double y = b[1] + b[3] / 2.0;
		ctx.runOnClient(mc -> {
			Screen sc = mc.gui.screen();
			sc.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
			sc.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
		});
	}

	private static Object call(Screen s, String method, Object... args) {
		try {
			for (var m : s.getClass().getMethods()) {
				if (m.getName().equals(method) && m.getParameterCount() == args.length) {
					return m.invoke(s, args);
				}
			}
			throw new NoSuchMethodException(method);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
