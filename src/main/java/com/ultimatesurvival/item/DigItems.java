package com.ultimatesurvival.item;

import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The four digging tool classes. All delegate "take extra blocks" to {@link DigLogic}. */
public final class DigItems {
	private DigItems() {
	}

	/** Called after vanilla finished its own bookkeeping for the block that was broken. */
	private static void extra(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner, ItemDef def) {
		if (!Gates.isLocked(stack) && level instanceof ServerLevel sl && owner instanceof ServerPlayer sp) {
			DigLogic.mine(sl, sp, stack, pos, state, def);
		}
	}

	public static class Pickaxe extends Item {
		private final ItemDef def;

		public Pickaxe(Item.Properties p, ItemDef def) {
			super(p);
			this.def = def;
		}

		@Override
		public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
			if (DigLogic.isBusy()) {
				return true; // extra block of an area break: free
			}
			boolean r = super.mineBlock(stack, level, state, pos, owner);
			extra(stack, level, state, pos, owner, def);
			return r;
		}
	}

	public static class Shovel extends ShovelItem {
		private final ItemDef def;

		public Shovel(ToolMaterial m, float dmg, float spd, Item.Properties p, ItemDef def) {
			super(m, dmg, spd, p);
			this.def = def;
		}

		@Override
		public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
			if (DigLogic.isBusy()) {
				return true; // extra block of an area break: free
			}
			boolean r = super.mineBlock(stack, level, state, pos, owner);
			extra(stack, level, state, pos, owner, def);
			return r;
		}
	}

	public static class Axe extends AxeItem {
		private final ItemDef def;

		public Axe(ToolMaterial m, float dmg, float spd, Item.Properties p, ItemDef def) {
			super(m, dmg, spd, p);
			this.def = def;
		}

		@Override
		public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
			if (DigLogic.isBusy()) {
				return true; // extra block of an area break: free
			}
			boolean r = super.mineBlock(stack, level, state, pos, owner);
			extra(stack, level, state, pos, owner, def);
			return r;
		}
	}

	/** Hoe: tills / harvests / replants in an area (sneak = single block like vanilla). */
	public static class Hoe extends HoeItem {
		private final ItemDef def;

		public Hoe(ToolMaterial m, float dmg, float spd, Item.Properties p, ItemDef def) {
			super(m, dmg, spd, p);
			this.def = def;
		}

		@Override
		public InteractionResult useOn(UseOnContext ctx) {
			int r = (int) def.stat("hoe_area", 0);
			net.minecraft.world.entity.player.Player player = ctx.getPlayer();
			if (r <= 0 || player == null || player.isShiftKeyDown() || Gates.isLocked(ctx.getItemInHand())) {
				return Gates.isLocked(ctx.getItemInHand()) ? InteractionResult.FAIL : super.useOn(ctx);
			}
			Level level = ctx.getLevel();
			BlockPos centre = ctx.getClickedPos();
			if (isCrop(level.getBlockState(centre))) {
				centre = centre.below();                       // clicked the plant itself: work from the soil under it
			}
			boolean any = false;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos p = centre.offset(dx, 0, dz);
					BlockPos plant = p.above();
					if (def.stat("replant", 0) > 0 && harvest(level, player, ctx.getItemInHand(), plant, level.getBlockState(plant))) {
						any = true;
						continue;
					}
					UseOnContext sub = new UseOnContext(level, player, ctx.getHand(), ctx.getItemInHand(),
							new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
					if (super.useOn(sub).consumesAction()) {
						any = true;
					}
				}
			}
			return any ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		private static boolean isCrop(BlockState st) {
			return st.getBlock() instanceof CropBlock || st.getBlock() instanceof NetherWartBlock;
		}

		private boolean harvest(Level level, net.minecraft.world.entity.player.Player player, ItemStack tool, BlockPos pos, BlockState st) {
			boolean ripe = st.getBlock() instanceof CropBlock cb && cb.isMaxAge(st)
					|| st.getBlock() instanceof NetherWartBlock && st.getValue(NetherWartBlock.AGE) >= 3;
			if (!ripe) {
				return false;
			}
			if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
				var drops = new java.util.ArrayList<>(Block.getDrops(st, sl, pos, null, player, tool));
				ItemStack seed = st.getCloneItemStack(sl, pos, false);
				for (ItemStack d : drops) {
					if (ItemStack.isSameItemSameComponents(d, seed)) {
						d.shrink(1);
						break;
					}
				}
				ToolFx.deliver(sp, tool, drops, pos);
				BlockState fresh = st.getBlock() instanceof CropBlock cb ? cb.getStateForAge(0)
						: st.setValue(NetherWartBlock.AGE, 0);
				sl.setBlock(pos, fresh, 3);
				sl.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8f, 1f);
				sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0);
				tool.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			}
			return true;
		}
	}
}
