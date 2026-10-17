package com.ultimatesurvival.util;

import com.ultimatesurvival.config.UsmConfig;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Damage + target selection for abilities (players) and boss attacks. */
public final class Dmg {
	private Dmg() {
	}

	/** Is {@code e} something a player's ability may hurt? (no players, pets, stands, dead things) */
	public static boolean isEnemy(Player owner, Entity e) {
		if (!(e instanceof LivingEntity le) || !le.isAlive() || e == owner || e instanceof Player || e instanceof ArmorStand) {
			return false;
		}
		if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) {
			return false;
		}
		return !e.isAlliedTo(owner) && !(e instanceof com.ultimatesurvival.entity.Ally a && a.ownerIs(owner));
	}

	public static List<LivingEntity> around(ServerLevel level, Vec3 c, double r, Predicate<LivingEntity> filter) {
		AABB box = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
		return level.getEntitiesOfClass(LivingEntity.class, box, e -> e.position().distanceToSqr(c) <= r * r && filter.test(e));
	}

	public static List<LivingEntity> enemiesAround(ServerLevel level, Player owner, Vec3 c, double r) {
		return around(level, c, r, e -> isEnemy(owner, e));
	}

	/** Melee-flavoured ability damage credited to the player (armor applies). */
	public static boolean physical(ServerLevel level, Player attacker, LivingEntity target, float amount) {
		return target.hurtServer(level, level.damageSources().playerAttack(attacker), scale(amount));
	}

	/** Spell damage: armor-bypassing (vanilla 'magic'), still credited to the player. */
	public static boolean magic(ServerLevel level, Player attacker, LivingEntity target, float amount) {
		return target.hurtServer(level, level.damageSources().indirectMagic(attacker, attacker), scale(amount));
	}

	/** Bonus damage riding on a hit that just landed: clears the target's hurt cooldown first so the extra is not swallowed by it. */
	public static boolean extra(ServerLevel level, Player attacker, LivingEntity target, float amount) {
		target.invulnerableTime = 0;
		return magic(level, attacker, target, amount);
	}

	public static float scale(float amount) {
		return (float) (amount * UsmConfig.get().abilityDamageMultiplier);
	}

	/** Push {@code target} away from {@code from} with an upward kick. */
	public static void shove(LivingEntity target, Vec3 from, double power, double lift) {
		Vec3 d = target.position().subtract(from);
		Vec3 h = new Vec3(d.x, 0, d.z);
		h = h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : h.normalize();
		double resist = 1.0 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
		target.setDeltaMovement(target.getDeltaMovement().add(h.x * power * resist, lift * Math.max(0.2, resist), h.z * power * resist));
		target.hurtMarked = true;
	}
}
