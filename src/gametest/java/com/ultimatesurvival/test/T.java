package com.ultimatesurvival.test;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.item.UsmItems;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Shared helpers for the in-game verification suite. Every call that touches the world runs on the server thread. */
final class T {
	private T() {
	}

	static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("FAILED: " + what);
		}
		UltimateSurvival.LOGGER.info("[gametest] ok: {}", what);
	}

	static void near(double actual, double expected, double eps, String what) {
		check(Math.abs(actual - expected) <= eps, what + " (expected " + expected + ", got " + actual + ")");
	}

	/** One parameter of an item's ability, read from the content so the checks follow the balance table instead of repeating it. */
	static double ab(String itemId, String abilityId, String key) {
		for (var r : UsmItems.defOf(UsmItems.get(itemId)).abilities) {
			if (r.id().equals(abilityId)) {
				return r.d(key, Double.NaN);
			}
		}
		throw new AssertionError(itemId + " has no ability " + abilityId);
	}

	/** A number the way the tooltips print it (Python's {:g}: no trailing zeros). */
	static String g(double v) {
		return new java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	/** One numeric stat of an item row (damage, durability, draw ...). */
	static double stat(String itemId, String key) {
		return UsmItems.defOf(UsmItems.get(itemId)).stat(key, Double.NaN);
	}

	/** One parameter of an armor-set bonus, read from the content. */
	static double bonus(String set, String abilityId, String key) {
		for (var b : com.ultimatesurvival.armor.ArmorSets.get(set).bonuses()) {
			if (b.ref().id().equals(abilityId)) {
				return b.ref().d(key, Double.NaN);
			}
		}
		throw new AssertionError(set + " has no bonus " + abilityId);
	}

	/** Total armor points of a full set, from the content (the balance table). */
	static double setArmor(String set) {
		double sum = 0;
		for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
			sum += stat(set + "_" + piece, "armor");
		}
		return sum;
	}

	/** The amount an item's content row gives to a named attribute ("max_health" ...). */
	static double attr(String itemId, String attribute) {
		for (var a : com.ultimatesurvival.content.AttrSpec.of(UsmItems.defOf(UsmItems.get(itemId)))) {
			if (a.name().equals(attribute)) {
				return a.amount();
			}
		}
		throw new AssertionError(itemId + " gives no " + attribute);
	}

	static ServerPlayer player(TestSingleplayerContext w) {
		return w.getConnection().getServerPlayer();
	}

	static ServerLevel level(TestSingleplayerContext w) {
		return w.getConnection().getServerLevel();
	}

	static <R> R onServer(TestSingleplayerContext w, java.util.function.Supplier<R> fn) {
		return w.getServer().computeOnServer(s -> fn.get());
	}

	static void onServer(TestSingleplayerContext w, Runnable fn) {
		w.getServer().runOnServer(s -> fn.run());
	}

	static void hold(TestSingleplayerContext w, String id) {
		onServer(w, () -> player(w).setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(UsmItems.get(id))));
	}

	static void wear(TestSingleplayerContext w, String set) {
		onServer(w, () -> {
			ServerPlayer p = player(w);
			p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(UsmItems.get(set + "_helmet")));
			p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(UsmItems.get(set + "_chestplate")));
			p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(UsmItems.get(set + "_leggings")));
			p.setItemSlot(EquipmentSlot.FEET, new ItemStack(UsmItems.get(set + "_boots")));
		});
	}

	static void stripArmor(TestSingleplayerContext w) {
		onServer(w, () -> {
			for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				player(w).setItemSlot(s, ItemStack.EMPTY);
			}
		});
	}

	static BlockState state(TestSingleplayerContext w, BlockPos p) {
		return onServer(w, () -> level(w).getBlockState(p));
	}

	static boolean air(TestSingleplayerContext w, BlockPos p) {
		return state(w, p).isAir();
	}

	/** Teleport the player to (x,y,z) looking at (lx,ly,lz). */
	static void stand(TestSingleplayerContext w, double x, double y, double z, double lx, double ly, double lz) {
		w.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @a %f %f %f facing %f %f %f", x, y, z, lx, ly, lz));
	}

	static void takeShot(ClientGameTestContext ctx, String name) {
		ctx.runOnClient(mc -> {                                                                   // chat and toasts left over from earlier checks hide the scene
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.toastManager().clear();
		});
		ctx.waitTicks(2);
		ctx.takeScreenshot(name);
	}

	static void settle(ClientGameTestContext ctx) {
		ctx.waitTicks(3);
	}

	static int count(TestSingleplayerContext w, String itemId) {
		return onServer(w, () -> {
			int n = 0;
			var inv = player(w).getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (!s.isEmpty() && s.getItem() == net.minecraft.core.registries.BuiltInRegistries.ITEM
						.getValue(net.minecraft.resources.Identifier.parse(itemId.contains(":") ? itemId : UltimateSurvival.MOD_ID + ":" + itemId))) {
					n += s.getCount();
				}
			}
			return n;
		});
	}

	/** Puts the 9 ingredient ids into a crafting table menu and returns what the result slot shows. */
	static ItemStack craft(TestSingleplayerContext w, String[] ids) {
		return onServer(w, () -> {
			ServerPlayer p = player(w);
			net.minecraft.world.inventory.CraftingMenu menu = new net.minecraft.world.inventory.CraftingMenu(88, p.getInventory(),
					net.minecraft.world.inventory.ContainerLevelAccess.create(level(w), BlockPos.ZERO));
			for (int i = 0; i < 9; i++) {
				if (!ids[i].isEmpty()) {
					var item = ids[i].contains(":") ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(ids[i])) : UsmItems.get(ids[i]);
					menu.getSlot(1 + i).set(new ItemStack(item));
				}
			}
			menu.slotsChanged(p.getInventory());
			return menu.getSlot(0).getItem().copy();
		});
	}

	static String tooltip(ClientGameTestContext ctx, ItemStack stack) {
		return ctx.computeOnClient(mc -> String.join("\n", stack
				.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL)
				.stream().map(c -> c.getString()).toList()));
	}
}
