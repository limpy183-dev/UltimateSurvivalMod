package com.ultimatesurvival.ability;

import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.util.Aim;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Weapon abilities shared across tiers; tiers differ by the parameters in the content tables. */
final class WeaponAbilities {
	private WeaponAbilities() {
	}

	static void register() {
		// energy bows read their shot from this ability's parameters (UsmBow); nothing to run here
		Abilities.marker("bow_shot");

		// ---- right-click abilities ---------------------------------------------------------
		Abilities.active("dash_strike", 120, c -> {
			double dist = c.p().d("dist", 6);
			float dmg = (float) c.p().d("dmg", 8);
			Vec3 look = c.look();
			Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.3, 0.45), look.z).normalize();
			Vec3 start = c.player().position();
			Vec3 end = Aim.dashTarget(c.level(), c.player(), dir, dist);
			if (end.distanceToSqr(start) < 1.0) {
				return false;
			}
			Vec3 a = start.add(0, 0.9, 0);
			Vec3 b = end.add(0, 0.9, 0);
			AABB sweep = c.player().getBoundingBox().expandTowards(end.subtract(start)).inflate(1.2);
			for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, sweep, e -> Dmg.isEnemy(c.player(), e))) {
				if (Aim.distToSegment(e.getBoundingBox().getCenter(), a, b) < 1.7) {
					Dmg.physical(c.level(), c.player(), e, dmg);
					Dmg.shove(e, start, 0.7, 0.3);
					Fx.burst(c.level(), e.getBoundingBox().getCenter(), ParticleTypes.CRIT, 12, 0.3, 0.3);
				}
			}
			int col = c.color(0x7FE8FF);
			Fx.line(c.level(), a, b, Fx.dust(col, 1.4f), 0.3);
			Fx.line(c.level(), a.add(0, 0.3, 0), b.add(0, 0.3, 0), ParticleTypes.END_ROD, 0.9);
			Fx.burst(c.level(), a, ParticleTypes.SWEEP_ATTACK, 3, 0.4, 0);
			Fx.sound(c.level(), start, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
			Fx.sound(c.level(), end, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.4f);
			c.player().teleportTo(end.x, end.y, end.z);
			c.player().fallDistance = 0;
			return true;
		});

		Abilities.active("blink", 80, c -> {
			Vec3 look = c.look();
			Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.5, 0.5), look.z).normalize();
			Vec3 start = c.player().position();
			Vec3 end = Aim.dashTarget(c.level(), c.player(), dir, c.p().d("dist", 10));
			if (end.distanceToSqr(start) < 1.5) {
				return false;
			}
			Fx.line(c.level(), start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 0.3);
			Fx.burst(c.level(), start.add(0, 1, 0), ParticleTypes.PORTAL, 24, 0.4, 0.4);
			Fx.sound(c.level(), start, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.1f);
			c.player().teleportTo(end.x, end.y, end.z);
			c.player().fallDistance = 0;
			Fx.burst(c.level(), end.add(0, 1, 0), ParticleTypes.PORTAL, 24, 0.4, 0.4);
			Fx.sound(c.level(), end, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.3f);
			return true;
		});

		Abilities.active("ground_slam", 160, c -> {
			double r = c.p().d("radius", 5);
			float dmg = (float) c.p().d("dmg", 10);
			Vec3 at = c.player().position();
			int hits = 0;
			for (LivingEntity e : Dmg.enemiesAround(c.level(), c.player(), at, r)) {
				if (Math.abs(e.getY() - at.y) > 3.5) {
					continue;
				}
				double f = 1.0 - 0.5 * (e.position().distanceTo(at) / r);
				Dmg.physical(c.level(), c.player(), e, (float) (dmg * f));
				Dmg.shove(e, at, c.p().d("kb", 1.1), 0.55);
				if (c.p().i("stun", 0) > 0) {
					Effects.give(e, "slowness", c.p().i("stun", 0), 4);
				}
				hits++;
			}
			BlockState below = c.level().getBlockState(c.player().blockPosition().below());
			if (!below.isAir()) {
				c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), at.x, at.y + 0.2, at.z,
						Fx.n(60), r * 0.45, 0.15, r * 0.45, 0.25);
			}
			Fx.shockwave(c.level(), at, r, 6, Fx.dust(c.color(0xB8C4D8), 1.6f), 40);
			Fx.burst(c.level(), at.add(0, 0.2, 0), ParticleTypes.EXPLOSION, 3, r * 0.3, 0);
			Fx.sound(c.level(), at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
			Fx.sound(c.level(), at, SoundEvents.IRON_GOLEM_DAMAGE, 1f, 0.5f);
			return hits >= 0;
		});

		Abilities.active("projectile_wave", 70, c -> {
			int n = Math.max(1, c.p().i("count", 5));
			double spread = Math.toRadians(c.p().d("spread", 50));
			BoltSpec spec = BoltSpec.of(c.p());
			Vec3 look = c.look();
			Vec3 origin = c.eye().add(look.scale(0.6)).add(0, -0.25, 0);
			for (int i = 0; i < n; i++) {
				double a = n == 1 ? 0 : -spread / 2 + spread * i / (n - 1);
				Vec3 d = look.yRot((float) -a);
				Bolt.fire(c.level(), c.player(), origin, d, spec);
			}
			Fx.sound(c.level(), origin, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
			Fx.burst(c.level(), origin, Fx.dust(spec.color(), 1.2f), 8, 0.2, 0.05);
			return true;
		});

		Abilities.active("chain_lightning", 110, c -> {
			int jumps = c.p().i("jumps", 4);
			double range = c.p().d("range", 18);
			double jumpRange = c.p().d("jump_range", 6);
			float dmg = (float) c.p().d("dmg", 9);
			LivingEntity cur = Aim.target(c.level(), c.player(), range, 0.93);
			if (cur == null) {
				return false;
			}
			Set<LivingEntity> seen = new HashSet<>();
			Vec3 prev = c.eye().add(c.look().scale(0.5)).add(0, -0.2, 0);
			for (int k = 0; k <= jumps && cur != null; k++) {
				Vec3 to = cur.getBoundingBox().getCenter();
				Fx.line(c.level(), prev, to, ParticleTypes.ELECTRIC_SPARK, 0.35);
				Fx.line(c.level(), prev, to, Fx.dust(0xFFF27A, 1.0f), 0.8);
				Fx.sparks(c.level(), to, 14);
				Dmg.magic(c.level(), c.player(), cur, (float) (dmg * Math.pow(0.85, k)));
				seen.add(cur);
				prev = to;
				cur = Aim.nearest(c.level(), c.player(), to, jumpRange, seen);
			}
			Fx.sound(c.level(), c.player().position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.6f);
			Fx.sound(c.level(), prev, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.2f);
			return true;
		});

		Abilities.active("heal_pulse", 300, c -> {
			float amount = (float) c.p().d("amount", 8);
			c.player().heal(amount);
			Vec3 at = c.player().position();
			Fx.ring(c.level(), at, 1.2, 24, ParticleTypes.HEART, 1.0);
			Fx.shockwave(c.level(), at, c.p().d("radius", 4), 6, ParticleTypes.HAPPY_VILLAGER, 24);
			for (var ally : c.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
					new AABB(at, at).inflate(c.p().d("radius", 4)))) {
				if (ally != c.player()) {
					ally.heal(amount * 0.5f);
				}
			}
			Fx.sound(c.level(), at, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.3f);
			Fx.sound(c.level(), at, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.5f);
			return true;
		});

		// ---- passive on-hit ----------------------------------------------------------------
		Abilities.onHit("lifesteal", (c, t) -> {
			float base = (float) c.player().getAttributeValue(Attributes.ATTACK_DAMAGE);
			float heal = base * com.ultimatesurvival.util.AttackStrength.of(c.player()) * (float) c.p().d("pct", 0.1);
			if (heal > 0.05f) {
				c.player().heal(heal);
				c.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(),
						Fx.n(3), 0.2, 0.2, 0.2, 0.0);
				c.level().sendParticles(ParticleTypes.HEART, c.player().getX(), c.player().getY() + 1.6, c.player().getZ(),
						1, 0.3, 0.2, 0.3, 0.0);
			}
		});

		Abilities.onHit("knockback_burst", (c, t) -> {
			Dmg.shove(t, c.player().position(), c.p().d("power", 1.4), 0.35);
			Fx.burst(c.level(), t.getBoundingBox().getCenter(), ParticleTypes.POOF, 6, 0.2, 0.1);
		});

		Abilities.onHit("bleed", (c, t) -> {
			Effects.give(t, "poison", c.p().i("ticks", 80), c.p().i("amp", 0));
			c.level().sendParticles(Fx.dust(0xB01020, 1.1f), t.getX(), t.getY() + 1.0, t.getZ(), Fx.n(8), 0.25, 0.4, 0.25, 0.0);
		});

		// ---- damage modifiers (applied before the hit lands, see LivingEntityMixin) ---------
		Abilities.modify("execute", (c, t, d) -> {
			double thr = c.p().d("threshold", 0.25);
			if (t.getHealth() / t.getMaxHealth() <= thr) {
				c.level().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), Fx.n(18), 0.3, 0.4, 0.3, 0.4);
				return d * (float) c.p().d("mult", 2.0);
			}
			return d;
		});

		Abilities.modify("backstab", (c, t, d) -> {
			Vec3 toPlayer = c.player().position().subtract(t.position()).normalize();
			Vec3 facing = t.getLookAngle();
			// target looks away from the attacker
			if (toPlayer.x * facing.x + toPlayer.z * facing.z < -0.2) {
				c.level().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), Fx.n(14), 0.3, 0.3, 0.3, 0.3);
				return d * (float) c.p().d("mult", 1.6);
			}
			return d;
		});

		// ---- on kill ---------------------------------------------------------------------
		Abilities.onKill("on_kill_buff", (c, victim) -> {
			Effects.give(c.player(), c.p().s("effect", "speed"), c.p().i("ticks", 100), c.p().i("amp", 0));
			c.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, c.player().getX(), c.player().getY() + 1, c.player().getZ(),
					Fx.n(6), 0.4, 0.5, 0.4, 0.0);
		});

		Abilities.onKill("on_kill_heal", (c, victim) -> {
			c.player().heal((float) c.p().d("amount", 2));
			c.level().sendParticles(ParticleTypes.HEART, c.player().getX(), c.player().getY() + 1.7, c.player().getZ(), 2, 0.3, 0.2, 0.3, 0.0);
		});
	}
}
