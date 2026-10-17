package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.mob.BossRush;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.mob.attack.Attacks;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soak: each Sovereign fights its own natural AI from the first tick of its spawn to the end of its death sequence, against a player who cannot die, stays in the
 * arena and trades a hit a second. Nothing is forced - attacks, phases, shields and vulnerability windows all come from the boss itself. A fight runs in real time
 * (the harness steps client and server in lockstep, so a tick sprint buys nothing): one to four minutes per boss. What it proves: the fight finishes (no stuck phase,
 * no attack that never ends), every phase attacks, most of the attack library shows up on its own, performance stays sane, nothing is left behind, and the boss still
 * pays out its gate and its bag.
 */
final class SoakTests {
	/** One hit per second, worth this fraction of the boss's maximum health. */
	private static final int HITS_TO_KILL = 100;
	private static final int MAX_TICKS = 30000;
	private static final int MAX_GAP = 800;
	/** A boss that loses no health for this long, with no pylons up, is stuck (an immunity that never ends). */
	private static final int STALL_TICKS = 3000;
	private static boolean hooked;
	private static volatile Soak current;

	private SoakTests() {
	}

	private static boolean part(String name) {
		String only = System.getenv("USM_PART");
		return only == null || only.isEmpty() || java.util.Arrays.asList(only.split(",")).contains(name);
	}

	/** What one fight measured. All mutation happens on the server thread; the test thread only reads after {@link #done}. */
	private static final class Soak {
		final String id;
		final UsmBoss boss;
		final float maxHp;
		final BlockPos arena;
		volatile boolean done;
		String failure = "";
		int ticks;
		int lastStart;
		int maxGap;
		int started;
		int lastPhase;
		boolean regressed;
		String lastRun = "";
		int deadSince = -1;
		int removedAt = -1;
		int maxEntities;
		long maxMsptNanos;
		final Map<Integer, Map<String, Integer>> byPhase = new TreeMap<>();
		final Set<String> everything = new LinkedHashSet<>();
		final List<String> lootIds = new ArrayList<>();
		int leftovers = -1;
		float lastHp;
		int lastHpChange;
		int pylonsBroken;

		Soak(String id, UsmBoss boss, BlockPos arena) {
			this.id = id;
			this.boss = boss;
			this.arena = arena;
			this.maxHp = boss.getMaxHealth();
		}

		AABB box() {
			return new AABB(arena).inflate(160, 80, 160);
		}

		void tick(MinecraftServer server) {
			ticks++;
			ServerLevel level = (ServerLevel) boss.level();
			List<ServerPlayer> players = server.getPlayerList().getPlayers();
			if (players.isEmpty()) {
				fail("no player");
				return;
			}
			ServerPlayer p = players.get(0);
			if (!boss.isRemoved()) {
				if (!boss.isDeadOrDying()) {
					p.setHealth(p.getMaxHealth());
					p.getFoodData().setFoodLevel(20);
					keepClose(level, p);
					if (boss.hasNodes()) {                                                      // shielded: a sensible player breaks the pylons first
						if (ticks % 20 == 10) {
							for (UsmMob m : level.getEntitiesOfClass(UsmMob.class, boss.getBoundingBox().inflate(40), e -> e != boss && e.isAlive() && e.master() == boss)) {
								if (m.hurtServer(level, p.damageSources().playerAttack(p), Math.max(8f, m.getMaxHealth() / 6f)) && !m.isAlive()) {
									pylonsBroken++;
								}
							}
						}
					} else if (ticks % 20 == 0) {
						boss.hurtServer(level, p.damageSources().playerAttack(p), maxHp / HITS_TO_KILL);
					}
					if (boss.getHealth() != lastHp) {
						lastHp = boss.getHealth();
						lastHpChange = ticks;
					} else if (ticks - lastHpChange > STALL_TICKS && !boss.hasNodes()) {
						fail("the boss took no damage for " + STALL_TICKS + " ticks (phase " + boss.phaseIndex() + ", run '" + boss.runType() + "', flags " + boss.flagBits() + ")");
						finish();
						return;
					}
					sampleAttacks();
				} else if (deadSince < 0) {
					deadSince = ticks;
				}
			} else if (removedAt < 0) {
				removedAt = ticks;
				for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box())) {
					lootIds.add(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath());
				}
			}
			if (ticks % 20 == 0) {
				List<Entity> all = level.getEntities((Entity) null, box(), e -> !(e instanceof ServerPlayer));
				maxEntities = Math.max(maxEntities, all.size());
				maxMsptNanos = Math.max(maxMsptNanos, server.getAverageTickTimeNanos());
			}
			if (removedAt >= 0 && ticks - removedAt >= 120) {
				List<Entity> left = level.getEntities((Entity) null, box(), e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(UltimateSurvival.MOD_ID));
				leftovers = left.size();
				finish();
			} else if (ticks >= MAX_TICKS) {
				fail("the boss was still alive after " + MAX_TICKS + " ticks (phase " + boss.phaseIndex() + ", hp " + boss.getHealth() + "/" + maxHp + ", run '" + boss.runType() + "')");
				finish();
			}
		}

		private void keepClose(ServerLevel level, ServerPlayer p) {
			Vec3 b = boss.position();
			double dx = p.getX() - b.x;
			double dz = p.getZ() - b.z;
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d > 22 || p.getY() < b.y - 8 || p.level() != level) {
				double a = d < 1e-3 ? 0 : Math.atan2(dz, dx);
				Vec3 spot = Attacks.ground(level, new Vec3(b.x + Math.cos(a) * 9, b.y, b.z + Math.sin(a) * 9));
				p.connection.teleport(spot.x, spot.y, spot.z, p.getYRot(), p.getXRot());
			}
		}

		private void sampleAttacks() {
			int ph = boss.phaseIndex();
			if (ph < lastPhase) {
				regressed = true;
			}
			lastPhase = ph;
			String run = boss.runType();
			if (!run.equals(lastRun)) {
				if (!run.isEmpty()) {
					started++;
					maxGap = Math.max(maxGap, ticks - lastStart);
					lastStart = ticks;
					byPhase.computeIfAbsent(ph, k -> new TreeMap<>()).merge(run, 1, Integer::sum);
					everything.add(run);
				}
				lastRun = run;
			}
		}

		void fail(String why) {
			if (failure.isEmpty()) {
				failure = why;
			}
		}

		void finish() {
			done = true;
		}
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		if (!hooked) {
			hooked = true;
			ServerTickEvents.END_SERVER_TICK.register(s -> {
				Soak soak = current;
				if (soak != null && !soak.done) {
					try {
						soak.tick(s);
					} catch (Throwable t) {
						soak.fail("exception in the soak hook: " + t);
						soak.finish();
						UltimateSurvival.LOGGER.error("soak hook failed", t);
					}
				}
			});
		}
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("gamerule doMobSpawning false");
		server.runCommand("gamerule doDaylightCycle false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		int bossNo = 0;
		for (String id : BossRush.ORDER) {
			if (!part(id)) {
				continue;
			}
			soak(ctx, w, id, me.getX() + 500 + 200 * bossNo, me.getY(), me.getZ() + 500);
			bossNo++;
		}
		server.runCommand("gamerule doMobSpawning true");
		server.runCommand("gamerule doDaylightCycle true");
		server.runCommand("effect clear @a");
		server.runCommand("usm reset");
	}

	private static void soak(ClientGameTestContext ctx, TestSingleplayerContext w, String id, int x, int y, int z) {
		var server = w.getServer();
		MobDef def = Mobs.DEFS.get(id);
		int index = BossRush.ORDER.indexOf(id);
		String previous = index == 0 ? "ender_dragon" : BossRush.ORDER.get(index - 1);
		server.runCommand("usm reset");
		server.runCommand("usm unlock " + previous);
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=!minecraft:player]");
		T.stand(w, x + 0.5, y, z + 0.5, x + 0.5, y + 1.5, z + 20.5);
		ctx.waitTicks(60);                                                            // let the arena chunks arrive
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1_000_000, 4, false, false));
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(UsmItems.get("verdite_sword")));
		});
		check(!T.onServer(w, () -> Gates.isCleared(id)), id + ": its gate is still closed before the fight");

		Soak soak = T.onServer(w, () -> {
			@SuppressWarnings("unchecked")
			EntityType<UsmBoss> type = (EntityType<UsmBoss>) Mobs.TYPES.get(id);
			UsmBoss b = type.create(T.level(w), EntitySpawnReason.TRIGGERED);
			Vec3 at = Attacks.ground(T.level(w), new Vec3(x + 0.5, y, z + 20.5));
			b.snapTo(at.x, at.y + 0.3, at.z, 0f, 0f);
			T.level(w).addFreshEntity(b);
			b.beginSpawn();
			Soak s = new Soak(id, b, new BlockPos(x, y, z));
			current = s;
			return s;
		});
		long wall = System.currentTimeMillis();
		int waited = 0;
		while (!soak.done && waited < MAX_TICKS + 600) {                              // the test harness ticks client and server in lockstep: real time
			ctx.waitTicks(10);
			waited += 10;
		}
		long seconds = (System.currentTimeMillis() - wall) / 1000;
		current = null;
		ctx.waitTicks(5);

		// ---- verdict ----------------------------------------------------------------------------------------------------------------------------
		check(soak.done, id + ": the soak fight ended");
		check(soak.failure.isEmpty(), id + ": the fight ran cleanly" + (soak.failure.isEmpty() ? "" : " - " + soak.failure));
		Set<String> defined = new LinkedHashSet<>();
		for (MobDef.Phase ph : def.phases) {
			ph.attacks().forEach(a -> defined.add(a.type()));
			ph.enter().forEach(a -> defined.add(a.type()));
		}
		UltimateSurvival.LOGGER.info("[soak] {}: {} ticks ({} s wall), {} attacks started, {}/{} types seen, longest silence {} ticks, up to {} entities, mspt {}, {} pylons broken",
				id, soak.ticks, seconds, soak.started, soak.everything.size(), defined.size(), soak.maxGap, soak.maxEntities,
				String.format(java.util.Locale.ROOT, "%.2f", soak.maxMsptNanos / 1e6), soak.pylonsBroken);
		for (var e : soak.byPhase.entrySet()) {
			UltimateSurvival.LOGGER.info("[soak] {} phase {}: {}", id, e.getKey(), e.getValue());
		}
		check(soak.removedAt > 0 && soak.deadSince > 0, id + ": it died and its death sequence ended (dying at " + soak.deadSince + ", removed at " + soak.removedAt + ")");
		check(!soak.regressed, id + ": phases only ever advanced");
		for (int ph = 0; ph < def.phases.size(); ph++) {
			Map<String, Integer> seen = soak.byPhase.get(ph);
			int n = seen == null ? 0 : seen.values().stream().mapToInt(Integer::intValue).sum();
			check(n >= 2, id + ": phase " + (ph + 1) + " ('" + def.phases.get(ph).name() + "') attacked on its own (" + n + " attacks, " + (seen == null ? "[]" : seen.keySet()) + ")");
		}
		check(soak.everything.size() * 2 >= defined.size(), id + ": at least half of its attack types came up by themselves (" + soak.everything.size() + "/" + defined.size()
				+ ", missing " + missing(defined, soak.everything) + ")");
		check(soak.maxGap <= MAX_GAP, id + ": it never went quiet for more than " + MAX_GAP + " ticks (longest " + soak.maxGap + ")");
		check(soak.maxMsptNanos < 40_000_000L, id + ": the server kept its tick budget (peak average "
				+ String.format(java.util.Locale.ROOT, "%.1f", soak.maxMsptNanos / 1e6) + " ms)");
		check(soak.maxEntities < 500, id + ": the fight never flooded the world with entities (peak " + soak.maxEntities + ")");
		check(soak.leftovers == 0, id + ": nothing of the boss's is left in the world after its death (" + soak.leftovers + " entities)");
		check(T.onServer(w, () -> Gates.isCleared(id)), id + ": its gate opened when it fell");
		check(soak.lootIds.contains(id + "_bag") || T.count(w, id + "_bag") > 0, id + ": its treasure bag dropped (on the ground: " + soak.lootIds + ")");
		check(T.onServer(w, () -> T.player(w).isAlive()), id + ": the player lived");
		w.getServer().runCommand("kill @e[type=!minecraft:player]");
	}

	private static String missing(Set<String> defined, Set<String> seen) {
		List<String> out = new ArrayList<>();
		for (String d : defined) {
			if (!seen.contains(d)) {
				out.add(d);
			}
		}
		return out.toString();
	}
}
