package com.ultimatesurvival.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Aim assist and safe movement helpers for abilities. */
public final class Aim {
	private Aim() {
	}

	/** The enemy closest to the crosshair within {@code range} (cone cosine {@code minCos}) with line of sight. */
	public static LivingEntity target(ServerLevel level, ServerPlayer p, double range, double minCos) {
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		LivingEntity best = null;
		double bestCos = minCos;
		for (LivingEntity e : Dmg.enemiesAround(level, p, eye, range)) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double d = to.length();
			if (d < 0.4) {
				continue;
			}
			double cos = to.scale(1.0 / d).dot(look);
			if (cos > bestCos && p.hasLineOfSight(e)) {
				bestCos = cos;
				best = e;
			}
		}
		return best;
	}

	/** Where the player points: the enemy under the crosshair, else the first solid point along the look ray (or its far end); null when that is nearer than two blocks. */
	public static Vec3 point(ServerLevel level, ServerPlayer p, double range) {
		Vec3 eye = p.getEyePosition();
		LivingEntity t = target(level, p, range, 0.9);
		Vec3 at;
		if (t != null) {
			at = t.position();
		} else {
			Vec3 end = eye.add(p.getLookAngle().scale(range));
			var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE, p));
			at = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
		}
		return at.distanceTo(eye) < 2.0 ? null : at;
	}

	/** Nearest enemy to {@code from} within {@code range} that is not in {@code skip}. */
	public static LivingEntity nearest(ServerLevel level, ServerPlayer owner, Vec3 from, double range, java.util.Set<LivingEntity> skip) {
		LivingEntity best = null;
		double bd = Double.MAX_VALUE;
		for (LivingEntity e : Dmg.enemiesAround(level, owner, from, range)) {
			if (skip.contains(e)) {
				continue;
			}
			double d = e.position().distanceToSqr(from);
			if (d < bd) {
				bd = d;
				best = e;
			}
		}
		return best;
	}

	/** Farthest collision-free point along {@code dir} (unit) up to {@code dist}; walks in half-block steps. */
	public static Vec3 dashTarget(ServerLevel level, Entity e, Vec3 dir, double dist) {
		Vec3 start = e.position();
		Vec3 best = start;
		for (double d = 0.5; d <= dist + 1e-6; d += 0.5) {
			Vec3 delta = dir.scale(d);
			AABB box = e.getBoundingBox().move(delta);
			if (!level.noCollision(e, box)) {
				break;
			}
			best = start.add(delta);
		}
		return best;
	}

	/** Shortest distance from point p to segment ab. */
	public static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double len2 = ab.lengthSqr();
		if (len2 < 1e-9) {
			return p.distanceTo(a);
		}
		double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
		return p.distanceTo(a.add(ab.scale(t)));
	}
}
