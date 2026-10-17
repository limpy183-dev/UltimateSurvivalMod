package com.ultimatesurvival.entity;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.util.Dmg;
import com.ultimatesurvival.util.Fx;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The one projectile of the mod. A {@link BoltSpec} decides damage, speed, piercing, homing, splash
 * and look; the sprite is a hidden "bolt_<style>" item rendered by the vanilla thrown-item renderer.
 */
public class Bolt extends ThrowableItemProjectile {
	private BoltSpec spec = BoltSpec.DEFAULT;
	private int age;
	private int holdTicks;
	private int holdTarget = -1;
	private int pierceLeft;
	private final IntSet hit = new IntOpenHashSet();

	public Bolt(EntityType<? extends Bolt> type, Level level) {
		super(type, level);
	}

	public static Bolt fire(ServerLevel level, LivingEntity owner, Vec3 pos, Vec3 dir, BoltSpec spec) {
		Bolt b = new Bolt(UsmEntities.BOLT, level);
		b.spec = spec;
		b.pierceLeft = spec.pierce();
		b.setItem(new ItemStack(UsmItems.get("bolt_" + spec.style())));
		b.setPos(pos);
		b.setOwner(owner);
		b.shoot(dir.x, dir.y, dir.z, spec.speed(), 0.0f);
		level.addFreshEntity(b);
		return b;
	}

	@Override
	protected Item getDefaultItem() {
		return UsmItems.get("bolt_ember");
	}

	@Override
	protected double getDefaultGravity() {
		return spec.gravity();
	}

	@Override
	protected float getAirDrag() {
		return 1.0f;
	}

	/** Freeze in place for {@code ticks}, then fly straight at {@code target}'s position at that moment (time-stop volleys). */
	public Bolt hold(int ticks, Entity target) {
		this.holdTicks = ticks;
		this.holdTarget = target == null ? -1 : target.getId();
		setDeltaMovement(Vec3.ZERO);
		return this;
	}

	private void release(ServerLevel sl) {
		Entity t = holdTarget >= 0 ? sl.getEntity(holdTarget) : null;
		Vec3 aim = t != null ? t.getBoundingBox().getCenter() : position().add(getLookAngle());
		Vec3 d = aim.subtract(position());
		setDeltaMovement(d.lengthSqr() < 1e-6 ? Vec3.ZERO : d.normalize().scale(spec.speed()));
	}

	@Override
	public void tick() {
		if (holdTicks > 0 && !level().isClientSide()) {
			setDeltaMovement(Vec3.ZERO);
		}
		super.tick();
		if (level().isClientSide() || isRemoved()) {
			return;
		}
		ServerLevel sl = (ServerLevel) level();
		if (holdTicks > 0) {
			if (holdTicks % 4 == 0) {
				sl.sendParticles(spec.trailParticle(), getX(), getY(), getZ(), 1, 0.08, 0.08, 0.08, 0.0);
			}
			if (--holdTicks == 0) {
				release(sl);
			}
			return;
		}
		if (++age > spec.life()) {
			impact(sl, position());
			discard();
			return;
		}
		sl.sendParticles(spec.trailParticle(), getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		if (spec.homing() && age > 4) {
			steer(sl);
		}
	}

	private void steer(ServerLevel sl) {
		LivingEntity best = null;
		double range = getOwner() instanceof Mob ? 48 : 14;                // boss orbs track across the whole arena
		double bd = range * range;
		for (LivingEntity e : Dmg.around(sl, position(), range, this::validTarget)) {
			if (hit.contains(e.getId())) {
				continue;
			}
			double d = e.position().distanceToSqr(position());
			if (d < bd) {
				bd = d;
				best = e;
			}
		}
		if (best != null) {
			Vec3 want = best.getBoundingBox().getCenter().subtract(position()).normalize();
			Vec3 v = getDeltaMovement();
			double sp = Math.max(v.length(), spec.speed());
			setDeltaMovement(v.normalize().scale(0.82).add(want.scale(0.18)).normalize().scale(sp));
		}
	}

	private boolean validTarget(Entity e) {
		Entity owner = getOwner();
		if (owner instanceof Player p) {
			return Dmg.isEnemy(p, e);
		}
		if (owner instanceof Mob) {
			return e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() || e instanceof Ally;
		}
		return e instanceof LivingEntity;
	}

	@Override
	protected boolean canHitEntity(Entity e) {
		return super.canHitEntity(e) && !hit.contains(e.getId()) && validTarget(e);
	}

	@Override
	protected void onHitEntity(EntityHitResult r) {
		super.onHitEntity(r);
		if (!(level() instanceof ServerLevel sl) || !(r.getEntity() instanceof LivingEntity target)) {
			return;
		}
		hit.add(target.getId());
		deal(sl, target, spec.damage());
		impact(sl, r.getLocation());
		if (pierceLeft-- <= 0) {
			discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult r) {
		super.onHitBlock(r);
		if (level() instanceof ServerLevel sl) {
			impact(sl, r.getLocation());
		}
		discard();
	}

	private void deal(ServerLevel sl, LivingEntity target, float amount) {
		Entity owner = getOwner();
		if (spec.style().equals("ember")) {
			target.igniteForSeconds(4);                       // ember bolts always set their target alight
		}
		if (owner instanceof Player p) {
			if (spec.magic()) {
				Dmg.magic(sl, p, target, amount);
			} else {
				Dmg.physical(sl, p, target, amount);
			}
		} else {
			if (owner instanceof com.ultimatesurvival.mob.UsmMob um) {
				um.hits++;
			}
			float a = (float) (amount * UsmConfig.get().bossDamageMultiplier);
			target.hurtServer(sl, sl.damageSources().indirectMagic(this, owner), a);
		}
	}

	private void impact(ServerLevel sl, Vec3 at) {
		int c = spec.color();
		boolean big = spec.damage() >= 60 && getOwner() instanceof Player;                     // a player's heavy bolt lands with a flash of its own
		sl.sendParticles(Fx.dust(c, 1.4f * spec.size()), at.x, at.y, at.z, Fx.n(big ? 18 : 10), 0.25, 0.25, 0.25, 0.04);
		sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, Fx.n(big ? 10 : 4), 0.15, 0.15, 0.15, 0.05);
		if (big) {
			Fx.ring(sl, at, 1.2, 14, Fx.dust(c, 1.3f), 0.0);
			sl.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, Fx.n(6), 0.3, 0.3, 0.3, 0.12);
		}
		sl.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5f, 1.4f);
		if (spec.aoe() > 0) {
			float r = spec.aoe();
			Fx.shockwave(sl, at, r, 4, Fx.dust(c, 1.2f), 28);
			Fx.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
			for (LivingEntity e : Dmg.around(sl, at, r, this::validTarget)) {
				if (!hit.contains(e.getId())) {
					hit.add(e.getId());
					deal(sl, e, spec.damage() * 0.7f);
				}
			}
		}
	}
}
