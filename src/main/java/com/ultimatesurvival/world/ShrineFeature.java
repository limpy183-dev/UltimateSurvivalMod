package com.ultimatesurvival.world;

import com.mojang.serialization.Codec;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.UsmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A Sovereign shrine: a small ruined platform with a Ritual Altar, four lantern-topped pillars and a chest of loot (including enchanted
 * books of levels the table never gives). Rare; stone bricks in the Overworld, purpur in the End. Only built on flat, dry ground.
 */
public class ShrineFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, UltimateSurvival.id("chests/shrine"));

	public ShrineFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		WorldGenLevel level = ctx.level();
		BlockPos o = ctx.origin();
		RandomSource r = ctx.random();
		for (int dx = -4; dx <= 4; dx += 2) {
			for (int dz = -4; dz <= 4; dz += 2) {
				BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, o.offset(dx, 0, dz));
				if (Math.abs(top.getY() - o.getY()) > 2 || !level.getFluidState(top.below()).isEmpty()) {
					return false;
				}
			}
		}
		boolean end = level.getLevel().dimension() == Level.END;
		if (end && Math.abs(o.getX()) < 160 && Math.abs(o.getZ()) < 160) {
			return false;                                                                         // leave the dragon's island alone
		}
		BlockState floor = end ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
		BlockState worn = end ? Blocks.END_STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		BlockState cracked = end ? Blocks.PURPUR_PILLAR.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
		int y = o.getY() - 1;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos p = new BlockPos(o.getX() + dx, y, o.getZ() + dz);
				boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == 4;
				BlockState s = ring ? (r.nextInt(3) == 0 ? worn : floor) : (r.nextInt(5) == 0 ? cracked : r.nextInt(4) == 0 ? worn : floor);
				if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
					s = gold;                                                                      // the altar sits on gold
				}
				level.setBlock(p, s, Block.UPDATE_CLIENTS);
				for (int up = 1; up <= 5; up++) {                                                  // clear a little headroom (trees, grass)
					level.setBlock(p.above(up), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
				for (int down = 1; down <= 3; down++) {                                            // and fill any gap below so it does not float
					BlockPos below = p.below(down);
					if (level.isEmptyBlock(below) || !level.getFluidState(below).isEmpty()) {
						level.setBlock(below, floor, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		BlockState altar = UsmBlocks.BY_ID.containsKey("ritual_altar") ? UsmBlocks.BY_ID.get("ritual_altar").defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
		level.setBlock(o, altar, Block.UPDATE_CLIENTS);
		BlockState pillar = end ? Blocks.PURPUR_PILLAR.defaultBlockState() : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
		for (int sx : new int[] {-3, 3}) {
			for (int sz : new int[] {-3, 3}) {
				for (int h = 0; h < 3; h++) {
					level.setBlock(o.offset(sx, h, sz), r.nextInt(4) == 0 && h == 2 ? worn : pillar, Block.UPDATE_CLIENTS);
				}
				level.setBlock(o.offset(sx, 3, sz), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		BlockPos chest = o.offset(0, 0, r.nextBoolean() ? 3 : -3);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, chest.getZ() > o.getZ() ? Direction.NORTH : Direction.SOUTH), Block.UPDATE_CLIENTS);
		RandomizableContainer.setBlockEntityLootTable(level, r, chest, LOOT);
		return true;
	}

	public static Codec<NoneFeatureConfiguration> codec() {
		return NoneFeatureConfiguration.CODEC;
	}
}
