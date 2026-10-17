package com.ultimatesurvival.item;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.ItemDef;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The extra blocks a digging tool takes with the one you broke: NxN area, tree felling and vein mining.
 * Runs inside {@code Item#mineBlock}; extra blocks go through the player's normal break path so
 * drops, enchantments (fortune/silk touch), protection and block events all behave. Extra blocks are
 * free; the tool pays 1 durability per three extras (felling is capped).
 */
public final class DigLogic {
	/** Re-entrancy guard: breaking an extra block calls mineBlock again. */
	private static boolean busy;

	private DigLogic() {
	}

	public static boolean isBusy() {
		return busy;
	}

	public static void mine(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, BlockState state, ItemDef def) {
		if (busy || player.isShiftKeyDown()) {
			return;
		}
		List<BlockPos> targets = new ArrayList<>();
		int fell = (int) def.stat("fell", 0);
		int vein = (int) def.stat("vein", 0);
		int area = (int) def.stat("area", 0);

		if (fell > 0 && state.is(BlockTags.LOGS)) {
			targets.addAll(tree(level, pos, state, def.stat("fell_leaves", 0) > 0 && UsmConfig.get().fellLeaves));
		} else if (vein > 0 && state.is(ConventionalBlockTags.ORES)) {
			targets.addAll(veinOf(level, pos, state, Math.min(vein, UsmConfig.get().veinMineMaxBlocks)));
		} else if (area > 0) {
			targets.addAll(areaOf(level, player, tool, pos, state, area, (int) Math.max(1, def.stat("depth", 1))));
		}
		if (targets.isEmpty()) {
			return;
		}

		busy = true;
		int broken = 0;
		try {
			for (BlockPos p : targets) {
				if (tool.isEmpty() || tool.getDamageValue() >= tool.getMaxDamage() - 1 && tool.isDamageableItem()) {
					break;
				}
				if (level.getBlockState(p).isAir() || !level.mayInteract(player, p)) {
					continue;
				}
				if (player.gameMode.destroyBlock(p)) {
					broken++;
				}
			}
		} finally {
			busy = false;
		}
		if (broken > 0 && tool.isDamageableItem() && !player.getAbilities().instabuild) {
			int cost = fell > 0 ? Math.min(32, (broken + 1) / 2) : (broken + 2) / 3;
			tool.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND);
		}
	}

	// ---- area -----------------------------------------------------------------------------
	private static List<BlockPos> areaOf(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, BlockState origin, int r, int depth) {
		Direction face = faceMined(player, pos);
		Direction.Axis axis = face.getAxis();
		List<BlockPos> out = new ArrayList<>();
		float baseHardness = origin.getDestroySpeed(level, pos);
		for (int d = 0; d < depth; d++) {
			BlockPos layer = pos.relative(face.getOpposite(), d);
			for (int a = -r; a <= r; a++) {
				for (int b = -r; b <= r; b++) {
					if (a == 0 && b == 0 && d == 0) {
						continue;
					}
					BlockPos p = switch (axis) {
						case Y -> layer.offset(a, 0, b);
						case X -> layer.offset(0, a, b);
						case Z -> layer.offset(a, b, 0);
					};
					BlockState s = level.getBlockState(p);
					if (s.isAir() || s.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null) {
						continue;
					}
					// only blocks this tool is meant for, and nothing much harder than the one you mined
					if (!tool.isCorrectToolForDrops(s) || tool.getDestroySpeed(s) <= 1.0f || s.getDestroySpeed(level, p) > Math.max(baseHardness * 4, 6)) {
						continue;
					}
					out.add(p);
				}
			}
		}
		return out;
	}

	/** Which face of {@code pos} the player was digging, found by re-casting the view ray against the removed cube. */
	private static Direction faceMined(ServerPlayer player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(8));
		BlockHitResult hit = AABB.clip(List.of(new AABB(0, 0, 0, 1, 1, 1)), eye, end, pos);
		if (hit != null) {
			return hit.getDirection();
		}
		return Direction.getApproximateNearest(player.getLookAngle()).getOpposite();
	}

	// ---- trees ----------------------------------------------------------------------------
	private static List<BlockPos> tree(ServerLevel level, BlockPos start, BlockState logState, boolean leaves) {
		int max = UsmConfig.get().fellMaxLogs;
		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> q = new ArrayDeque<>();
		q.add(start);
		seen.add(start);
		while (!q.isEmpty() && logs.size() < max) {
			BlockPos cur = q.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = 0; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = cur.offset(dx, dy, dz);
						if (!seen.add(n)) {
							continue;
						}
						if (Math.abs(n.getX() - start.getX()) > 7 || Math.abs(n.getZ() - start.getZ()) > 7 || n.getY() - start.getY() > 40) {
							continue;
						}
						if (level.getBlockState(n).is(logState.getBlock())) {
							logs.add(n);
							q.add(n);
						}
					}
				}
			}
		}
		logs.remove(start);
		// felling from the bottom up looks better in the break order: sort by height
		logs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
		if (leaves && !logs.isEmpty()) {
			Set<BlockPos> leafSet = new HashSet<>();
			List<BlockPos> all = new ArrayList<>(logs);
			all.add(start);
			for (BlockPos l : all) {
				for (BlockPos n : BlockPos.betweenClosed(l.offset(-3, -2, -3), l.offset(3, 3, 3))) {
					BlockState s = level.getBlockState(n);
					if (s.is(BlockTags.LEAVES) && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)) {
						leafSet.add(n.immutable());
					}
				}
			}
			int cap = 220;
			for (BlockPos l : leafSet) {
				if (cap-- <= 0) {
					break;
				}
				logs.add(l);
			}
		}
		return logs;
	}

	// ---- veins ----------------------------------------------------------------------------
	private static List<BlockPos> veinOf(ServerLevel level, BlockPos start, BlockState origin, int max) {
		String key = oreKey(origin);
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> q = new ArrayDeque<>();
		q.add(start);
		seen.add(start);
		while (!q.isEmpty() && out.size() < max) {
			BlockPos cur = q.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = cur.offset(dx, dy, dz);
						if (!seen.add(n)) {
							continue;
						}
						BlockState s = level.getBlockState(n);
						if (s.is(ConventionalBlockTags.ORES) && oreKey(s).equals(key)) {
							out.add(n);
							q.add(n);
						}
					}
				}
			}
		}
		return out;
	}

	/** "deepslate_iron_ore" and "iron_ore" belong to the same vein family. */
	private static String oreKey(BlockState s) {
		String p = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
		for (String pre : new String[] {"deepslate_", "nether_", "end_"}) {
			if (p.startsWith(pre)) {
				p = p.substring(pre.length());
			}
		}
		return p.endsWith("_ore") ? p.substring(0, p.length() - 4) : p;
	}
}
