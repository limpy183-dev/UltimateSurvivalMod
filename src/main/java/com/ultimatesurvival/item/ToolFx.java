package com.ultimatesurvival.item;

import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;

/** Auto-smelt and magnet handling for drops produced by a tool that has those traits. */
public final class ToolFx {
	private ToolFx() {
	}

	/** True if this tool post-processes drops (so the Block mixin should take over). */
	public static boolean handles(ItemStack tool) {
		if (tool.isEmpty() || Gates.isLocked(tool)) {
			return false;
		}
		ItemDef d = UsmItems.defOf(tool.getItem());
		return d != null && (d.stat("smelt", 0) > 0 || d.stat("magnet", 0) > 0);
	}

	/** Smelt (if the tool can) then hand the drops to the player (magnet) or drop them in the world. */
	public static void deliver(ServerPlayer player, ItemStack tool, List<ItemStack> drops, BlockPos pos) {
		ServerLevel level = player.level();
		ItemDef d = tool.isEmpty() ? null : UsmItems.defOf(tool.getItem());
		boolean live = d != null && !Gates.isLocked(tool);
		List<ItemStack> out = new ArrayList<>(drops);
		if (live && d.stat("smelt", 0) > 0) {
			out = smelt(level, out, pos);
		}
		boolean magnet = live && d.stat("magnet", 0) > 0;
		for (ItemStack s : out) {
			if (s.isEmpty()) {
				continue;
			}
			if (magnet) {
				player.getInventory().add(s);
				if (s.isEmpty()) {
					continue;
				}
			}
			Block.popResource(level, pos, s);
		}
	}

	private static List<ItemStack> smelt(ServerLevel level, List<ItemStack> in, BlockPos pos) {
		List<ItemStack> out = new ArrayList<>();
		boolean cooked = false;
		for (ItemStack s : in) {
			Optional<RecipeHolder<SmeltingRecipe>> r = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(s), level);
			if (r.isPresent()) {
				ItemStack res = r.get().value().assemble(new SingleRecipeInput(s));
				if (!res.isEmpty()) {
					res.setCount(res.getCount() * s.getCount());
					out.add(res);
					cooked = true;
					continue;
				}
			}
			out.add(s);
		}
		if (cooked) {
			level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.25, 0.2, 0.25, 0.01);
		}
		return out;
	}
}
