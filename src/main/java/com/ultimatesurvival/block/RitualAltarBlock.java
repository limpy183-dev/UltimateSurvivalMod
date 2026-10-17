package com.ultimatesurvival.block;

import com.ultimatesurvival.item.SigilItem;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.Ritual;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The altar a summoning sigil is used on. All the logic lives in {@link Ritual}. */
public class RitualAltarBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

	public RitualAltarBlock(Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(stack.getItem() instanceof SigilItem sigil)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			MobDef boss = Mobs.DEFS.get(sigil.boss());
			if (boss == null) {
				return InteractionResult.FAIL;
			}
			var gate = com.ultimatesurvival.progress.Gates.gateOf(stack.getItem());
			if (gate != null && !com.ultimatesurvival.progress.Gates.isCleared(gate)) {
				sp.sendOverlayMessage(Component.translatable("ultimatesurvival.ritual.fail.locked", gate.name()).withStyle(net.minecraft.ChatFormatting.RED));
				return InteractionResult.FAIL;
			}
			Component why = Ritual.check(sl, pos, sp, boss);
			if (why != null) {
				sp.sendOverlayMessage(why);
				return InteractionResult.FAIL;
			}
			Ritual.begin(sl, pos, sp, stack, boss);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("ultimatesurvival.ritual.altar_hint"));
		}
		return InteractionResult.SUCCESS;
	}
}
