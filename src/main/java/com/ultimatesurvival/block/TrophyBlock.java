package com.ultimatesurvival.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A boss trophy: a plinth with the Sovereign's emblem on a plaque. Pure decoration (the glowing ones give light). */
public class TrophyBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

	public TrophyBlock(Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
