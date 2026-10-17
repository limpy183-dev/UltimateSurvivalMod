package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.mob.Invasions;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.phys.AABB;

/** The regular mobs: registration, natural spawn rules, every attack they own, drops, invasions and vanilla-mob scaling. */
final class MobTests {
	private MobTests() {
	}

	private static List<MobDef> regular() {
		List<MobDef> out = new ArrayList<>();
		for (MobDef d : Mobs.DEFS.values()) {
			if (!d.boss) {
				out.add(d);
			}
		}
		return out;
	}

	private static UsmMob spawn(TestSingleplayerContext w, String id, double x, double y, double z) {
		return T.onServer(w, () -> {
			UsmMob m = Mobs.TYPES.get(id).create(T.level(w), EntitySpawnReason.COMMAND);
			m.snapTo(x, y, z, 90f, 0f);
			T.level(w).addFreshEntity(m);
			m.passive = true;
			m.noMelee = true;
			return m;
		});
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		boolean scaling = UsmConfig.get().vanillaMobScalingPerTier > 0;
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 + 90;
		double y = me.getY();
		double z = me.getZ() + 0.5 + 30;
		T.stand(w, x, y, z, x + 10, y, z);
		ctx.waitTicks(40);
		BossTests.immortal(w);

		// ---- registration -----------------------------------------------------------------------------------------------------------
		List<MobDef> mobs = regular();
		check(mobs.size() >= 24, "at least twenty-four regular mobs are registered (" + mobs.size() + ")");
		for (MobDef d : mobs) {
			check(Mobs.EGGS.containsKey(d.id) && T.onServer(w, () -> T.level(w).registryAccess() != null), d.id + " has a spawn egg");
		}
		long natural = mobs.stream().filter(d -> d.spawn != null).count();
		check(natural >= 16, "sixteen or more mobs spawn naturally (" + natural + ")");

		// ---- biome spawn lists contain them ------------------------------------------------------------------------------------------------
		var plains = T.onServer(w, () -> T.level(w).getBiome(me).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
				.map(e -> e.value().type()).toList());
		check(plains.contains(Mobs.TYPES.get("thornback")), "the overworld biome spawn list includes the Thornback");
		check(!plains.contains(Mobs.TYPES.get("cinder_hound")), "...but not the Nether's Cinder Hound");

		// ---- natural spawn rules follow the progress gates -------------------------------------------------------------------------------------
		server.runCommand("time set midnight");
		ctx.waitTicks(5);
		BlockPos ground = me;
		MobDef thorn = Mobs.DEFS.get("thornback");
		MobDef stalker = Mobs.DEFS.get("cave_stalker");
		MobDef hound = Mobs.DEFS.get("cinder_hound");
		check(T.onServer(w, () -> {
			for (int i = 0; i < 60; i++) {                                // the vanilla surface check is probabilistic
				if (UsmMob.canSpawnNaturally(thorn, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())) {
					return true;
				}
			}
			return false;
		}), "a Thornback may spawn at night in the Overworld");
		check(!T.onServer(w, () -> UsmMob.canSpawnNaturally(stalker, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())), "a Cave Stalker may not before the Ender Dragon falls");
		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> {
			for (int i = 0; i < 60; i++) {
				if (UsmMob.canSpawnNaturally(stalker, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())) {
					return true;
				}
			}
			return false;
		}), "...and may afterwards");
		check(!T.onServer(w, () -> UsmMob.canSpawnNaturally(hound, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())), "a Cinder Hound never spawns in the Overworld");
		server.runCommand("time set day");
		ctx.waitTicks(5);
		check(!T.onServer(w, () -> UsmMob.canSpawnNaturally(thorn, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())), "a Thornback does not spawn in daylight");
		// the bright half of Tier 5 waits for Nihilus; the void half never leaves the End
		MobDef sentinel = Mobs.DEFS.get("radiant_sentinel");
		MobDef voidStalker = Mobs.DEFS.get("void_stalker");
		check(!T.onServer(w, () -> UsmMob.canSpawnNaturally(sentinel, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())), "a Radiant Sentinel may not spawn before Nihilus falls");
		server.runCommand("usm unlock nihilus");
		ctx.waitTicks(3);
		check(T.onServer(w, () -> {
			for (int i = 0; i < 60; i++) {
				if (UsmMob.canSpawnNaturally(sentinel, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())) {
					return true;
				}
			}
			return false;
		}), "...and may afterwards, even by day");
		check(!T.onServer(w, () -> UsmMob.canSpawnNaturally(voidStalker, T.level(w), EntitySpawnReason.NATURAL, ground, T.level(w).getRandom())), "a Void Stalker never spawns in the Overworld");
		var endBiomeList = T.onServer(w, () -> T.level(w).registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).listElements()
				.filter(b -> b.key().identifier().getPath().equals("the_end")).findFirst().get().value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
				.map(e -> e.value().type()).toList());
		check(endBiomeList.contains(Mobs.TYPES.get("void_stalker")) && endBiomeList.contains(Mobs.TYPES.get("rift_weaver")), "the End's spawn list holds the Void Stalker and the Rift Weaver");
		server.runCommand("usm reset");
		server.runCommand("usm unlock voltaris");

		// ---- the gallery: every mob in a row, photographed --------------------------------------------------------------------------------------------------
		List<String> ids = mobs.stream().map(d -> d.id).toList();
		int shot = 0;
		for (int from = 0; from < ids.size(); from += 4) {
			List<UsmMob> row = new ArrayList<>();
			for (int k = 0; k < 4 && from + k < ids.size(); k++) {
				MobDef d = Mobs.DEFS.get(ids.get(from + k));
				row.add(spawn(w, d.id, x + 11, y + (d.fly ? Math.max(1.0, d.hover) : 0), z - 7.5 + k * 5.0));
			}
			T.stand(w, x, y, z, x + 11, y + 1.2, z);
			ctx.waitTicks(25);
			T.takeShot(ctx, "mobs-" + (++shot) + "-gallery");
			for (UsmMob m : row) {
				T.onServer(w, () -> m.discard());
			}
		}

		// ---- every attack of every mob runs and reaches a player standing in front of it -------------------------------------------------------------------------
		int attacks = 0;
		for (MobDef d : mobs) {
			java.util.Map<String, Double> types = new java.util.LinkedHashMap<>();
			java.util.Map<String, MobDef.AttackSpec> specs = new java.util.HashMap<>();
			for (MobDef.AttackSpec s : d.phases.get(0).attacks()) {
				double[] band = s.p().pair("range", 0, 30);
				specs.putIfAbsent(s.type(), s);
				types.putIfAbsent(s.type(), Math.max(2.5, Math.min(14.0, (band[0] + band[1]) / 2)));      // start in the middle of the attack's usable band
			}
			for (var entry : types.entrySet()) {
				String type = entry.getKey();
				double gap = entry.getValue();
				T.stand(w, x, y, z, x + 7, y, z);
				BossTests.immortal(w);
				UsmMob m = spawn(w, d.id, x + gap, y + (d.fly ? 2.0 : 0.0), z);
				T.onServer(w, () -> m.setTarget(T.player(w)));
				ctx.waitTicks(4);
				int h0 = T.onServer(w, () -> m.hits);
				T.onServer(w, () -> m.forceAttack(type));
				if (type.equals("icicles")) {                                                   // the lines start half a step off the bearing to the target, so a player standing on that bearing sits in a gap: step onto the first line
					ctx.waitTicks(3);
					double a = Math.PI + Math.PI / Math.max(3, specs.get(type).p().i("lines", 6));
					T.stand(w, x + gap + gap * Math.cos(a), y, z + gap * Math.sin(a), x + gap, y, z);
				}
				BossTests.waitRunEnd(ctx, w, m, 260);
				ctx.waitTicks(36);
				int dh = T.onServer(w, () -> m.hits) - h0;
				if (type.equals("reflect")) {                                                  // a mirror stance hurts nobody by itself
					check(T.onServer(w, () -> m.getHealth()) > 0, d.id + " raised its mirror stance");
				} else {
					check(dh > 0, d.id + " attack '" + type + "' reached the player (hits +" + dh + "; mob at " + T.onServer(w, () -> m.position()) + " from gap " + gap
							+ ", player at " + T.onServer(w, () -> T.player(w).position()) + ", vuln=" + T.onServer(w, () -> m.hasFlag(UsmMob.F_VULN)) + ")");
				}
				attacks++;
				T.onServer(w, () -> m.discard());
				server.runCommand("kill @e[type=ultimatesurvival:bolt]");
				server.runCommand("kill @e[type=ultimatesurvival:void_spawn]");        // rifts leave real creatures behind
				ctx.waitTicks(3);
			}
		}
		check(attacks >= 30, "thirty or more distinct mob attacks were exercised (" + attacks + ")");

		// ---- drops --------------------------------------------------------------------------------------------------------------------------------------------------------
		UsmMob boar = spawn(w, "thornback", x + 6, y, z + 6);
		T.onServer(w, () -> boar.kill(T.level(w)));
		ctx.waitTicks(30);
		int pork = T.onServer(w, () -> T.level(w).getEntitiesOfClass(ItemEntity.class, new AABB(x - 20, y - 5, z - 20, x + 30, y + 10, z + 30),
				e -> e.getItem().is(net.minecraft.world.item.Items.PORKCHOP)).stream().mapToInt(e -> e.getItem().getCount()).sum());
		check(pork >= 1, "a dead Thornback drops pork (" + pork + ")");
		server.runCommand("kill @e[type=minecraft:item]");

		invasions(ctx, w, x, y, z, scaling);
		BossTests.mortal(w);
	}

	private static void invasions(ClientGameTestContext ctx, TestSingleplayerContext w, double x, double y, double z, boolean scalingWas) {
		var server = w.getServer();
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		T.stand(w, x, y, z, x + 10, y, z);
		ctx.waitTicks(10);
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		BossTests.immortal(w);

		// ---- an invasion at tier 1 ---------------------------------------------------------------------------------------------------------------------------------------
		int n = T.onServer(w, () -> Invasions.startNow(p));
		check(n >= 4, "an invasion spawned its wave (" + n + " invaders)");
		check(Invasions.activeCount() == 1, "and is being tracked");
		var invaders = T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(60, 30, 60)));
		check(invaders.size() == n, "the invaders are in the world (" + invaders.size() + ")");
		check(invaders.stream().allMatch(m -> T.onServer(w, () -> m.getTarget() == p)), "and all of them hunt the player");
		double maxDist = invaders.stream().mapToDouble(m -> T.onServer(w, () -> m.distanceTo(p))).max().orElse(0);
		check(maxDist > 12, "they close in from a distance (" + String.format("%.0f", maxDist) + " blocks)");
		ctx.waitTicks(30);
		T.takeShot(ctx, "mobs-4-invasion");
		T.onServer(w, () -> {
			for (UsmMob m : T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(60, 30, 60))) {
				m.kill(T.level(w));
			}
		});
		ctx.waitTicks(60);
		check(Invasions.activeCount() == 0, "killing the whole wave ends the invasion");
		check(T.count(w, "verdite_ingot") >= 2, "and pays out the reward (" + T.count(w, "verdite_ingot") + " Verdite Ingots)");
		server.runCommand("kill @e[type=minecraft:item]");

		// ---- invaders scale with the player's tier ---------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock ignarok");
		ctx.waitTicks(3);
		check(Gates.currentTier() == 3, "tier three reached");
		T.onServer(w, () -> Invasions.startNow(p));
		var wave3 = T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(80, 40, 80), m -> m.def().id.equals("cinder_hound")));
		check(!wave3.isEmpty(), "a tier three wave brings Cinder Hounds (" + wave3.size() + ")");
		float hp = T.onServer(w, () -> wave3.get(0).getMaxHealth());
		float expected = (float) (55 * (1 + UsmConfig.get().invaderScalingPerTier * 2));
		check(Math.abs(hp - expected) < 1.0f, "invaders scale with tier: " + hp + " HP (expected " + expected + ")");
		T.onServer(w, () -> {
			for (UsmMob m : T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(80, 40, 80))) {
				m.discard();
			}
		});
		ctx.waitTicks(30);
		check(Invasions.activeCount() == 0, "discarded invaders end the wave");

		// ---- tier five: the Unmaking -------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock nihilus");
		ctx.waitTicks(3);
		check(Gates.currentTier() == 5, "tier five reached");
		T.onServer(w, () -> Invasions.startNow(p));
		var wave5 = T.onServer(w, () -> T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(80, 40, 80)));
		var kinds = wave5.stream().map(m -> m.def().id).collect(java.util.stream.Collectors.toSet());
		check(kinds.containsAll(java.util.Set.of("void_stalker", "rift_weaver", "radiant_sentinel", "prism_drone", "void_spawn")), "a tier five wave brings all five creatures of the Unmaking (" + kinds + ")");
		UsmMob stalker = wave5.stream().filter(m -> m.def().id.equals("void_stalker")).findFirst().get();
		float shp = T.onServer(w, () -> stalker.getMaxHealth());
		check(Math.abs(shp - (float) (240 * (1 + UsmConfig.get().invaderScalingPerTier * 4))) < 1.5f, "tier five invaders are scaled up too (" + shp + " HP)");
		T.onServer(w, () -> {
			for (UsmMob m : T.level(w).getEntitiesOfClass(UsmMob.class, p.getBoundingBox().inflate(80, 40, 80))) {
				m.discard();
			}
		});
		ctx.waitTicks(30);

		// ---- vanilla hostile mobs grow with the tier ------------------------------------------------------------------------------------------------------------------------------
		UsmConfig.get().vanillaMobScalingPerTier = 0.2;
		Husk z3 = T.onServer(w, () -> {
			Husk h = new Husk(net.minecraft.world.entity.EntityTypes.HUSK, T.level(w));
			h.setNoAi(true);
			h.snapTo(x + 5, y, z + 5, 0, 0);
			T.level(w).addFreshEntity(h);
			return h;
		});
		float zhp = T.onServer(w, () -> z3.getMaxHealth());
		check(Math.abs(zhp - (float) (20 * (1 + 0.2 * (Gates.currentTier() - 1)))) < 0.5f, "a vanilla Husk spawns with +20% health per cleared tier (" + zhp + " HP at tier " + Gates.currentTier() + ")");
		check(T.onServer(w, () -> z3.getHealth()) == zhp, "at full health");
		UsmConfig.get().vanillaMobScalingPerTier = 0;
		Husk plain = T.onServer(w, () -> {
			Husk h = new Husk(net.minecraft.world.entity.EntityTypes.HUSK, T.level(w));
			h.setNoAi(true);
			h.snapTo(x + 5, y, z + 8, 0, 0);
			T.level(w).addFreshEntity(h);
			return h;
		});
		check(Math.abs(T.onServer(w, () -> plain.getMaxHealth()) - 20.0f) < 0.1f, "scaling can be switched off in the config");
		UsmConfig.get().vanillaMobScalingPerTier = scalingWas ? 0.2 : 0;
		server.runCommand("kill @e[type=minecraft:husk]");
		server.runCommand("usm reset");
		T.onServer(w, () -> T.player(w).removeEffect(MobEffects.RESISTANCE));
	}
}
