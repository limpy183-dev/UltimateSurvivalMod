package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 3x3 mining, tree felling, vein mining, magnet drops, hoe tilling/replanting. */
final class ToolTests {
	private ToolTests() {
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("usm reset");
		ctx.waitTicks(2);
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		int ox = me.getX() + 12;
		int oy = me.getY();
		int oz = me.getZ();

		// ---- 3x3 pickaxe: breaking the middle of a 7x7x3 stone slab clears exactly the 3x3 facing us -------------
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", ox, oy, oz, ox + 2, oy + 6, oz + 6));
		T.stand(w, ox - 2.5, oy + 3, oz + 3.5, ox + 0.5, oy + 3.5, oz + 3.5);
		T.hold(w, "verdite_pickaxe");
		T.settle(ctx);
		boolean broke = T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 3, oz + 3)));
		check(broke, "pickaxe broke the target block");
		int gone = 0;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (T.air(w, new BlockPos(ox, oy + 3 + dy, oz + 3 + dz))) {
					gone++;
				}
			}
		}
		check(gone == 9, "verdite pickaxe cleared the whole 3x3 face (cleared " + gone + ")");
		check(!T.air(w, new BlockPos(ox, oy + 3, oz + 5)) && !T.air(w, new BlockPos(ox + 1, oy + 3, oz + 3)),
				"blocks outside the 3x3 and behind it are untouched");

		// sneaking mines exactly one block
		server.runCommand("data merge entity @a {}");
		T.onServer(w, () -> T.player(w).setShiftKeyDown(true));
		boolean b2 = T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox, oy + 1, oz + 1)));
		T.onServer(w, () -> T.player(w).setShiftKeyDown(false));
		check(b2 && T.air(w, new BlockPos(ox, oy + 1, oz + 1)) && !T.air(w, new BlockPos(ox, oy + 1, oz + 2)),
				"sneaking disables the area break");

		// durability: area break costs ~1 per three extra blocks, not 9
		int dmg = T.onServer(w, () -> T.player(w).getMainHandItem().getDamageValue());
		check(dmg >= 3 && dmg <= 6, "area breaking is cheap on durability (damage " + dmg + ")");

		// ---- locked pickaxe digs like a hand -----------------------------------------------------------------------
		server.runCommand("fill " + (ox + 4) + " " + oy + " " + oz + " " + (ox + 6) + " " + (oy + 2) + " " + (oz + 2) + " minecraft:stone");
		T.hold(w, "azurite_pickaxe");                       // tier 2 -> locked in a fresh world
		T.stand(w, ox + 2.5, oy + 1, oz + 1.5, ox + 4.5, oy + 1.5, oz + 1.5);
		T.settle(ctx);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(ox + 4, oy + 1, oz + 1)));
		int cleared = 0;
		for (int dy = 0; dy <= 2; dy++) {
			for (int dz = 0; dz <= 2; dz++) {
				if (T.air(w, new BlockPos(ox + 4, oy + dy, oz + dz))) {
					cleared++;
				}
			}
		}
		check(cleared == 1, "a locked azurite pickaxe only breaks one block (cleared " + cleared + ")");
		check(T.count(w, "minecraft:stone") == 0 && T.count(w, "minecraft:cobblestone") == 0,
				"a locked pickaxe is not a valid tool, so no drops");

		// ---- tree felling ----------------------------------------------------------------------------------------------
		int tx = me.getX() - 8;
		int tz = me.getZ() + 4;
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:oak_log", tx, oy, tz, tx, oy + 6, tz));
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:oak_leaves[persistent=false,distance=1] replace minecraft:air",
				tx - 2, oy + 4, tz - 2, tx + 2, oy + 8, tz + 2));
		server.runCommand(String.format("setblock %d %d %d minecraft:oak_log", tx, oy + 7, tz));
		T.hold(w, "verdite_axe");
		T.stand(w, tx - 3.5, oy, tz + 0.5, tx + 0.5, oy + 1.5, tz + 0.5);
		T.settle(ctx);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(tx, oy, tz)));
		boolean allLogsGone = true;
		for (int y = 0; y <= 7; y++) {
			allLogsGone &= T.air(w, new BlockPos(tx, oy + y, tz));
		}
		check(allLogsGone, "verdite axe felled the whole 8-log tree from the bottom log");
		check(!T.air(w, new BlockPos(tx + 1, oy + 5, tz)), "the basic axe leaves the leaves alone");

		// ---- vein mining + magnet + leaf felling with tier 2 tools (unlocked) -----------------------------------------
		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(2);
		int vx = me.getX() - 8;
		int vz = me.getZ() - 8;
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:stone", vx - 1, oy, vz - 1, vx + 5, oy + 3, vz + 3));
		BlockPos[] ore = {new BlockPos(vx, oy + 1, vz), new BlockPos(vx + 1, oy + 1, vz), new BlockPos(vx + 2, oy + 1, vz),
				new BlockPos(vx + 2, oy + 2, vz), new BlockPos(vx + 3, oy + 2, vz + 1)};
		for (BlockPos p : ore) {
			server.runCommand(String.format("setblock %d %d %d minecraft:coal_ore", p.getX(), p.getY(), p.getZ()));
		}
		T.hold(w, "azurite_pickaxe");
		T.stand(w, vx - 0.5, oy + 1, vz + 0.5, vx + 0.5, oy + 1.5, vz + 0.5);
		T.settle(ctx);
		int coalBefore = T.count(w, "minecraft:coal");
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(ore[0]));
		boolean veinGone = true;
		for (BlockPos p : ore) {
			veinGone &= T.air(w, p);
		}
		check(veinGone, "azurite pickaxe pulled the whole 5-block coal vein");
		check(T.count(w, "minecraft:coal") - coalBefore >= 4, "magnet: vein drops went straight into the inventory (coal +" + (T.count(w, "minecraft:coal") - coalBefore) + ")");

		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:oak_log", tx, oy, tz, tx, oy + 5, tz));
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:oak_leaves[persistent=false,distance=1] replace minecraft:air",
				tx - 2, oy + 3, tz - 2, tx + 2, oy + 7, tz + 2));
		T.hold(w, "azurite_axe");
		T.stand(w, tx - 3.5, oy, tz + 0.5, tx + 0.5, oy + 1.5, tz + 0.5);
		T.settle(ctx);
		T.onServer(w, () -> T.player(w).gameMode.destroyBlock(new BlockPos(tx, oy, tz)));
		check(T.air(w, new BlockPos(tx, oy + 5, tz)) && T.air(w, new BlockPos(tx + 1, oy + 5, tz)) && T.air(w, new BlockPos(tx - 2, oy + 4, tz + 2)),
				"azurite axe felled the logs AND the leaves");

		// ---- hoe: tills a 3x3 patch, harvests and replants ripe wheat -----------------------------------------------------
		int hx = me.getX() + 4;
		int hz = me.getZ() - 12;
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:dirt", hx - 3, oy - 1, hz - 3, hx + 3, oy - 1, hz + 3));
		server.runCommand(String.format("fill %d %d %d %d %d %d minecraft:air", hx - 3, oy, hz - 3, hx + 3, oy + 2, hz + 3));
		T.hold(w, "verdite_hoe");
		T.settle(ctx);
		useOn(w, new BlockPos(hx, oy - 1, hz));
		int farmland = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (T.state(w, new BlockPos(hx + dx, oy - 1, hz + dz)).is(Blocks.FARMLAND)) {
					farmland++;
				}
			}
		}
		check(farmland == 9, "verdite hoe tilled a 3x3 patch (" + farmland + " farmland)");
		check(!T.state(w, new BlockPos(hx + 2, oy - 1, hz)).is(Blocks.FARMLAND), "tilling stopped at the patch edge");

		server.runCommand(String.format("setblock %d %d %d minecraft:wheat[age=7]", hx, oy, hz));
		int wheatBefore = T.count(w, "minecraft:wheat");
		useOn(w, new BlockPos(hx, oy - 1, hz));
		var crop = T.state(w, new BlockPos(hx, oy, hz));
		check(crop.is(Blocks.WHEAT) && crop.getValue(net.minecraft.world.level.block.CropBlock.AGE) == 0, "ripe wheat was harvested and replanted at age 0");
		ctx.waitTicks(3);
		T.takeShot(ctx, "phase2-tools-world");
	}

	/** Right-click the top of {@code ground} with the held item, exactly like the game would. */
	static void useOn(TestSingleplayerContext w, BlockPos ground) {
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			ServerLevel level = T.level(w);
			ItemStack held = p.getMainHandItem();
			UseOnContext c = new UseOnContext(level, p, InteractionHand.MAIN_HAND, held,
					new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
			held.useOn(c);
		});
	}
}
