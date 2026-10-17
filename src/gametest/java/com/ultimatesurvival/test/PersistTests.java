package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.attack.Attacks;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Saving and quitting in the middle of a Sovereign fight, then coming back: the boss, its health, its phase and its boss bar must all survive. */
final class PersistTests {
	private static final String BOSS = "chronos";
	private static final double HP_FRACTION = 0.5;

	private PersistTests() {
	}

	/** Called while the first world is still open: puts a hurt, second-phase boss next to the player. */
	static void prepare(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("kill @e[type=!minecraft:player]");
		server.runCommand("difficulty normal");
		server.runCommand("gamemode creative @a");
		ctx.waitTicks(5);
		T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			@SuppressWarnings("unchecked")
			EntityType<UsmBoss> type = (EntityType<UsmBoss>) Mobs.TYPES.get(BOSS);
			UsmBoss b = type.create(T.level(w), EntitySpawnReason.TRIGGERED);
			Vec3 at = Attacks.ground(T.level(w), p.position().add(14, 0, 0));
			b.snapTo(at.x, at.y + 0.3, at.z, 0f, 0f);
			T.level(w).addFreshEntity(b);
			b.setHealth((float) (b.getMaxHealth() * HP_FRACTION));
			return null;
		});
		ctx.waitTicks(60);
		check(count(w) == 1, "a hurt Chronos stands in the arena before the world is saved");
	}

	private static int count(TestSingleplayerContext w) {
		return T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			return T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(p.position(), p.position()).inflate(80)).size();
		});
	}

	/** Called in the reopened world. */
	static void verify(ClientGameTestContext ctx, TestSingleplayerContext w) {
		ctx.waitTicks(100);
		check(count(w) == 1, "after saving and quitting mid-fight the boss is still there, exactly once");
		List<UsmBoss> found = T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			return T.level(w).getEntitiesOfClass(UsmBoss.class, new AABB(p.position(), p.position()).inflate(80));
		});
		UsmBoss b = found.get(0);
		float frac = T.onServer(w, () -> b.getHealth() / b.getMaxHealth());
		check(Math.abs(frac - HP_FRACTION) < 0.1, "it kept its wounds (" + Math.round(frac * 100) + "% health)");
		check(T.onServer(w, () -> b.phaseIndex()) >= 1, "and picked its fight up in the right phase (" + (T.onServer(w, () -> b.phaseIndex()) + 1) + ")");
		check(T.onServer(w, () -> b.bossBar().getPlayers().size()) == 1, "its boss bar is back on the player's screen");
		check(T.onServer(w, () -> b.isAlive() && !b.isRemoved()), "and it is alive and ticking");
		w.getServer().runCommand("kill @e[type=ultimatesurvival:" + BOSS + "]");
		w.getServer().runCommand("gamemode survival @a");
	}
}
