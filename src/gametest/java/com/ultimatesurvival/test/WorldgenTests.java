package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.UsmBlocks;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Natural world generation, in a world made with the ordinary generator (not the flat test world): every ore turns up in the dimension and at the heights it is
 * meant for, and the Sovereign shrine can be built on real terrain. Chunks are force-loaded so they go through the whole generation pipeline.
 */
final class WorldgenTests {
	private WorldgenTests() {
	}

	/** What a scan of a region found of one family of blocks. */
	private record Found(int count, int minY, int maxY) {
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamerule doMobSpawning false");
		server.runCommand("gamemode creative @a");
		overworld(ctx, w);
		mountains(ctx, w);
		shrines(ctx, w);
		nether(ctx, w);
		end(ctx, w);
	}

	// ---- helpers -------------------------------------------------------------------------------------------------------------------------------------
	private static ServerLevel level(TestSingleplayerContext w, ResourceKey<Level> dim) {
		return T.level(w).getServer().getLevel(dim);
	}

	/** Force-loads the chunks around a block rectangle in a dimension and waits until every one of them is fully generated. */
	private static void load(ClientGameTestContext ctx, TestSingleplayerContext w, ResourceKey<Level> dim, int x0, int z0, int x1, int z1) {
		w.getServer().runCommand("execute in " + dim.identifier() + " run forceload add " + x0 + " " + z0 + " " + x1 + " " + z1);
		int cx0 = Math.floorDiv(x0, 16);
		int cz0 = Math.floorDiv(z0, 16);
		int cx1 = Math.floorDiv(x1, 16);
		int cz1 = Math.floorDiv(z1, 16);
		for (int i = 0; i < 180; i++) {                                                       // up to three minutes
			ctx.waitTicks(20);
			boolean all = T.onServer(w, () -> {
				ServerLevel l = level(w, dim);
				for (int cx = cx0; cx <= cx1; cx++) {
					for (int cz = cz0; cz <= cz1; cz++) {
						if (l.getChunkSource().getChunkNow(cx, cz) == null) {
							return false;
						}
					}
				}
				return true;
			});
			if (all) {
				return;
			}
		}
		throw new AssertionError("FAILED: the chunks of " + dim.identifier() + " did not finish generating");
	}

	private static void unload(TestSingleplayerContext w, ResourceKey<Level> dim) {
		w.getServer().runCommand("execute in " + dim.identifier() + " run forceload remove all");
	}

	private static Found scan(TestSingleplayerContext w, ResourceKey<Level> dim, int x0, int z0, int x1, int z1, Block... blocks) {
		Set<Block> want = new HashSet<>(java.util.List.of(blocks));
		return T.onServer(w, () -> {
			ServerLevel l = level(w, dim);
			int n = 0;
			int lo = Integer.MAX_VALUE;
			int hi = Integer.MIN_VALUE;
			for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
				for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
					LevelChunk ch = l.getChunkSource().getChunkNow(cx, cz);
					if (ch == null) {
						continue;
					}
					LevelChunkSection[] sections = ch.getSections();
					for (int s = 0; s < sections.length; s++) {
						LevelChunkSection sec = sections[s];
						if (sec.hasOnlyAir() || !sec.maybeHas(bs -> want.contains(bs.getBlock()))) {
							continue;
						}
						int baseY = ch.getMinY() + s * 16;
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								for (int x = 0; x < 16; x++) {
									if (want.contains(sec.getBlockState(x, y, z).getBlock())) {
										n++;
										lo = Math.min(lo, baseY + y);
										hi = Math.max(hi, baseY + y);
									}
								}
							}
						}
					}
				}
			}
			return new Found(n, lo, hi);
		});
	}

	private static Block block(String id) {
		return UsmBlocks.BY_ID.get(id);
	}

	private static void report(String name, Found f) {
		UltimateSurvival.LOGGER.info("[worldgen] {}: {} blocks between y={} and y={}", name, f.count, f.minY, f.maxY);
	}

	// ---- the Overworld: Verdite and Azurite ----------------------------------------------------------------------------------------------------------------------
	private static void overworld(ClientGameTestContext ctx, TestSingleplayerContext w) {
		int r = 112;
		load(ctx, w, Level.OVERWORLD, -r, -r, r, r);
		Found verdite = scan(w, Level.OVERWORLD, -r, -r, r, r, block("verdite_ore"), block("deepslate_verdite_ore"));
		report("verdite (overworld, 225 chunks)", verdite);
		check(verdite.count >= 400, "Verdite ore generates in the Overworld (" + verdite.count + " blocks in 225 chunks)");
		check(verdite.minY >= -64 && verdite.maxY <= 90, "and stays within its band (y " + verdite.minY + ".." + verdite.maxY + ")");
		Found azurite = scan(w, Level.OVERWORLD, -r, -r, r, r, block("azurite_ore"), block("deepslate_azurite_ore"));
		report("azurite (overworld, 225 chunks)", azurite);
		check(azurite.count >= 60, "Azurite ore generates deep in the Overworld (" + azurite.count + " blocks)");
		check(azurite.minY >= -64 && azurite.maxY <= 12, "in the deep layers only (y " + azurite.minY + ".." + azurite.maxY + ")");
		Found stray = scan(w, Level.OVERWORLD, -r, -r, r, r, block("cinderite_ore"), block("aetherite_ore"));
		check(stray.count == 0, "Nether and End ores never turn up in the Overworld (" + stray.count + ")");
		// keep this region loaded: the shrine test builds on it
	}

	// ---- mountain peaks: Stormite ------------------------------------------------------------------------------------------------------------------------------------------
	private static void mountains(ClientGameTestContext ctx, TestSingleplayerContext w) {
		BlockPos peak = T.onServer(w, () -> {
			var found = T.level(w).findClosestBiome3d(h -> h.is(Biomes.JAGGED_PEAKS) || h.is(Biomes.FROZEN_PEAKS) || h.is(Biomes.STONY_PEAKS), new BlockPos(0, 100, 0), 4000, 32, 64);
			return found == null ? null : found.getFirst();
		});
		check(peak != null, "mountain peaks exist within 4000 blocks of the spawn of this world");
		int r = 64;
		int x0 = peak.getX() - r;
		int z0 = peak.getZ() - r;
		int x1 = peak.getX() + r;
		int z1 = peak.getZ() + r;
		load(ctx, w, Level.OVERWORLD, x0, z0, x1, z1);
		Found storm = scan(w, Level.OVERWORLD, x0, z0, x1, z1, block("stormite_ore"));
		report("stormite (peaks around " + peak.toShortString() + ")", storm);
		check(storm.count >= 60, "Stormite ore generates high up in the mountains, plentifully enough to find (" + storm.count + " blocks in 81 chunks)");
		check(storm.minY >= 62, "and only high up (lowest y " + storm.minY + ")");
		unload(w, Level.OVERWORLD);
	}

	// ---- shrines on natural terrain ---------------------------------------------------------------------------------------------------------------------------------
	private static void shrines(ClientGameTestContext ctx, TestSingleplayerContext w) {
		load(ctx, w, Level.OVERWORLD, -112, -112, 112, 112);                                  // the spawn region again (the peak load released it)
		int attempts = 0;
		BlockPos built = null;
		for (int x = -100; x <= 100 && built == null; x += 7) {
			for (int z = -100; z <= 100 && built == null; z += 7) {
				final int fx = x;
				final int fz = z;
				attempts++;
				built = T.onServer(w, () -> {
					ServerLevel l = T.level(w);
					int y = l.getHeight(Heightmap.Types.WORLD_SURFACE_WG, fx, fz);
					BlockPos at = new BlockPos(fx, y, fz);
					var cf = l.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
							.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, UltimateSurvival.id("sovereign_shrine"))).value();
					return cf.place(l, l.getChunkSource().getGenerator(), RandomSource.create(fx * 31L + fz), at) ? at : null;
				});
			}
		}
		UltimateSurvival.LOGGER.info("[worldgen] shrine: built at {} after {} attempts on natural terrain", built, attempts);
		check(built != null, "a Sovereign shrine can be built on natural terrain (" + attempts + " spots tried)");
		BlockPos o = built;
		check(T.onServer(w, () -> T.level(w).getBlockState(o).is(block("ritual_altar"))), "it has its Ritual Altar in the middle");
		boolean chest = T.onServer(w, () -> {
			for (BlockPos p : new BlockPos[] {o.offset(0, 0, 3), o.offset(0, 0, -3)}) {
				if (T.level(w).getBlockEntity(p) instanceof RandomizableContainerBlockEntity c && c.getLootTable() != null) {
					return true;
				}
			}
			return false;
		});
		check(chest, "and a chest with the shrine's loot table");
		int lanterns = T.onServer(w, () -> {
			int n = 0;
			for (int sx : new int[] {-3, 3}) {
				for (int sz : new int[] {-3, 3}) {
					if (T.level(w).getBlockState(o.offset(sx, 3, sz)).is(Blocks.LANTERN)) {
						n++;
					}
				}
			}
			return n;
		});
		check(lanterns == 4, "and four lantern-topped pillars (" + lanterns + ")");
		unload(w, Level.OVERWORLD);
	}

	// ---- the Nether: Cinderite ---------------------------------------------------------------------------------------------------------------------------------------------
	private static void nether(ClientGameTestContext ctx, TestSingleplayerContext w) {
		int r = 96;
		load(ctx, w, Level.NETHER, -r, -r, r, r);
		Found cinder = scan(w, Level.NETHER, -r, -r, r, r, block("cinderite_ore"));
		report("cinderite (nether, 169 chunks)", cinder);
		check(cinder.count >= 150, "Cinderite ore generates in the Nether (" + cinder.count + " blocks in 169 chunks)");
		check(cinder.minY >= 4 && cinder.maxY <= 126, "within the Nether's rock (y " + cinder.minY + ".." + cinder.maxY + ")");
		unload(w, Level.NETHER);
	}

	// ---- the End: Aetherite ------------------------------------------------------------------------------------------------------------------------------------------------------
	private static void end(ClientGameTestContext ctx, TestSingleplayerContext w) {
		int r = 96;
		load(ctx, w, Level.END, -r, -r, r, r);
		Found aether = scan(w, Level.END, -r, -r, r, r, block("aetherite_ore"));
		report("aetherite (end main island, 169 chunks)", aether);
		check(aether.count >= 25, "Aetherite ore generates in the End (" + aether.count + " blocks around the main island)");
		check(aether.minY >= 4 && aether.maxY <= 100, "within the End stone (y " + aether.minY + ".." + aether.maxY + ")");
		unload(w, Level.END);
	}
}
