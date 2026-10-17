package com.ultimatesurvival.ability;

import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Shake;
import com.ultimatesurvival.util.Tasks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Tier 6 (Morbane) abilities: infection that jumps from corpse to corpse, lingering clouds, and an aura that rots and heals. */
final class PlagueAbilities {
	private record Infection(UUID owner, float dmg, double radius, long until, int gen) {
	}

	private static final Map<UUID, Infection> INFECTED = new HashMap<>();
	private static final int MAX_GENERATIONS = 6;

	private PlagueAbilities() {
	}

	/** Marks a creature as carrying the plague: poison now, and when it dies the sickness bursts out and finds everything near it. */
	private static void infect(LivingEntity e, ServerPlayer owner, float dmg, double radius, int gen, int ticks) {
		if (e instanceof com.ultimatesurvival.mob.UsmBoss) {
			return;                                                                    // bosses shrug off effects
		}
		INFECTED.put(e.getUUID(), new Infection(owner.getUUID(), dmg, radius, e.level().getGameTime() + 1200, gen));
		rot(e, ticks, 2);
		Effects.give(e, "wither", ticks, 1);
		if (INFECTED.size() > 512) {
			long now = e.level().getGameTime();
			INFECTED.values().removeIf(i -> i.until < now);
		}
	}

	/** Poison for the living; the undead are immune to it, so they rot with wither instead (the plague must show on everything). */
	private static void rot(LivingEntity e, int ticks, int amp) {
		Effects.give(e, e.is(net.minecraft.tags.EntityTypeTags.UNDEAD) ? "wither" : "poison", ticks, amp);
	}

	static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Infection inf = INFECTED.remove(entity.getUUID());
			if (inf == null || !(entity.level() instanceof ServerLevel l) || inf.until < l.getGameTime() || inf.gen >= MAX_GENERATIONS) {
				return;
			}
			ServerPlayer owner = l.getServer().getPlayerList().getPlayer(inf.owner);
			if (owner == null) {
				return;
			}
			Vec3 at = entity.getBoundingBox().getCenter();
			int n = 0;
			for (LivingEntity e : Dmg.enemiesAround(l, owner, at, inf.radius)) {
				if (e == entity || n++ >= 14) {
					continue;
				}
				Dmg.extra(l, owner, e, inf.dmg);
				infect(e, owner, inf.dmg, inf.radius, inf.gen + 1, 200);
				Fx.line(l, at, e.getBoundingBox().getCenter(), Fx.dust(0xA6E22E, 1.1f), 0.5);
			}
			Fx.burst(l, at, Fx.dust(0xA6E22E, 2.0f), 50, 1.0, 0.2);
			Fx.burst(l, at, ParticleTypes.SPORE_BLOSSOM_AIR, 40, 1.4, 0.15);
			Fx.ring(l, at.subtract(0, entity.getBbHeight() * 0.4, 0), inf.radius, 40, Fx.dust(0xA6E22E, 1.4f), 0.2);
			Fx.sound(l, at, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
		});

		// every hit gives the target the plague
		Abilities.onHit("contagion", (c, t) -> {
			infect(t, c.player(), (float) c.p().d("dmg", 60), c.p().d("radius", 8), 0, c.p().i("ticks", 160));
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), Fx.dust(c.color(0xA6E22E), 1.4f), 14, 0.4, 0.05);
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), ParticleTypes.SPORE_BLOSSOM_AIR, 8, 0.4, 0.05);
		});

		// a cloud of spores where you look
		Abilities.active("toxic_cloud", 260, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			Vec3 at = Aim.point(l, p, c.p().d("range", 36));
			if (at == null) {
				return false;
			}
			cloud(l, p, com.ultimatesurvival.mob.attack.Attacks.ground(l, at), c.p().d("radius", 7), c.p().i("ticks", 160), (float) c.p().d("dmg", 36), c.color(0xA6E22E));
			return true;
		});

		// everything near you is infected, and the ground around you blooms with clouds
		Abilities.active("plague_burst", 320, c -> {
			ServerLevel l = c.level();
			ServerPlayer p = c.player();
			double radius = c.p().d("radius", 12);
			float dmg = (float) c.p().d("dmg", 140);
			int col = c.color(0xA6E22E);
			Vec3 at = p.position();
			for (LivingEntity e : Dmg.enemiesAround(l, p, at, radius)) {
				Dmg.extra(l, p, e, dmg);
				infect(e, p, dmg * 0.5f, radius * 0.7, 0, 200);
			}
			var rnd = l.getRandom();
			for (int i = 0; i < c.p().i("clouds", 5); i++) {
				double a = rnd.nextDouble() * Math.PI * 2;
				double d = radius * (0.35 + 0.5 * rnd.nextDouble());
				cloud(l, p, com.ultimatesurvival.mob.attack.Attacks.ground(l, at.add(Math.cos(a) * d, 0, Math.sin(a) * d)), 4.5, 80, dmg * 0.25f, col);
			}
			Fx.shockwave(l, at, radius, 10, Fx.dust(col, 2.4f), 90);
			Fx.burst(l, at.add(0, 1.2, 0), ParticleTypes.SPORE_BLOSSOM_AIR, 140, 3.0, 0.4);
			Fx.burst(l, at.add(0, 1.2, 0), Fx.dust(col, 2.6f), 100, 2.4, 0.3);
			Fx.sound(l, at, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.8f, 0.4f);
			Fx.sound(l, at, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.4f, 0.5f);
			Shake.at(l, at, radius * 2.5, 0.4f, 12);
			return true;
		});

		// worn: everything near you rots, and every rotting thing feeds you
		Abilities.passive("blight_aura").tick = (p, r) -> {
			if (!(p.level() instanceof ServerLevel l)) {
				return;
			}
			if (p.tickCount % 3 == 0) {
				double a = p.tickCount * 0.17;
				l.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX() + Math.cos(a) * 1.6, p.getY() + 0.4 + (p.tickCount % 30) / 18.0, p.getZ() + Math.sin(a) * 1.6, 1, 0, 0, 0, 0);
			}
			int every = Math.max(10, r.i("every", 20));
			if (p.tickCount % every != 0) {
				return;
			}
			double rad = r.d("radius", 10);
			int hit = 0;
			for (LivingEntity e : Dmg.enemiesAround(l, p, p.position(), rad)) {
				Dmg.extra(l, p, e, (float) r.d("dmg", 40));
				Effects.give(e, "wither", every + 20, 1);
				Fx.burst(l, e.getBoundingBox().getCenter(), Fx.dust(0xA6E22E, 1.1f), 5, 0.3, 0.05);
				hit++;
			}
			if (hit > 0) {
				p.heal((float) (r.d("heal", 4) * Math.min(hit, 6)));
				Fx.burst(l, p.position().add(0, 1.6, 0), ParticleTypes.HEART, 2, 0.3, 0);
			}
			Fx.ring(l, p.position(), rad, 36, Fx.dust(0xA6E22E, 1.2f), 0.15);
		};
	}

	/** A cloud that poisons and eats whatever stands in it for {@code ticks}. */
	private static void cloud(ServerLevel l, ServerPlayer p, Vec3 centre, double radius, int ticks, float dmg, int col) {
		Fx.sound(l, centre, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.4f, 0.5f);
		for (int k = 0; k < ticks; k += 2) {
			final int age = k;
			Tasks.later(k, () -> {
				var rnd = l.getRandom();
				for (int i = 0; i < Fx.n(8); i++) {
					double a = rnd.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(rnd.nextDouble()) * radius;
					l.sendParticles(i % 3 == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : Fx.dust(col, 1.7f), centre.x + Math.cos(a) * d, centre.y + 0.2 + rnd.nextDouble() * 1.8,
							centre.z + Math.sin(a) * d, 1, 0.1, 0.15, 0.1, 0.01);
				}
				if (age % 10 == 0) {
					Fx.ring(l, centre, radius, 32, Fx.dust(col, 1.3f), 0.15);
					for (LivingEntity e : Dmg.enemiesAround(l, p, centre, radius)) {
						Dmg.extra(l, p, e, dmg);
						rot(e, 40, 2);
						Effects.give(e, "slowness", 30, 1);
					}
				}
			});
		}
	}
}
