package com.ultimatesurvival.progress;

import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.util.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The vanilla Ender Dragon is the gate for tier 2 and sheds drakescale as it dies. */
public final class DragonGate {
	private DragonGate() {
	}

	public static void defeated(ServerLevel level) {
		Gate g = com.ultimatesurvival.content.Content.gate("ender_dragon");
		if (g != null) {
			Gates.defeat(level.getServer(), g);
		}
	}

	/** 4-8 drakescale rain from the dying dragon (the vanilla loot table is empty and never consulted). */
	public static void dropLoot(ServerLevel level, EnderDragon dragon) {
		if (!UsmItems.BY_ID.containsKey("drakescale")) {
			return;
		}
		int n = 4 + level.getRandom().nextInt(5);
		Vec3 at = dragon.position().add(0, 2, 0);
		for (int i = 0; i < n; i++) {
			ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(UsmItems.get("drakescale")),
					(level.getRandom().nextDouble() - 0.5) * 0.4, 0.3 + level.getRandom().nextDouble() * 0.3, (level.getRandom().nextDouble() - 0.5) * 0.4);
			e.setGlowingTag(true);
			level.addFreshEntity(e);
		}
		var trophy = com.ultimatesurvival.mob.Mobs.stack("trophy_ender_dragon", 1);
		if (!trophy.isEmpty()) {
			ItemEntity t = new ItemEntity(level, at.x, at.y, at.z, trophy, 0, 0.35, 0);
			t.setGlowingTag(true);
			t.setExtendedLifetime();
			t.setInvulnerable(true);
			level.addFreshEntity(t);
		}
		Fx.burst(level, at, ParticleTypes.END_ROD, 80, 2.0, 0.3);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
	}
}
