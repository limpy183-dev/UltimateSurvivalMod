package com.ultimatesurvival.mob.attack;

import com.ultimatesurvival.mob.UsmMob;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A lingering damage zone on the floor (lava pool, blood pool, static field ...). Pure particles + damage, never touches blocks. */
public class Hazard {
	public final Vec3 pos;
	public final double radius;
	public final double dmgMul;
	public final int interval;
	public final String style;
	/** Optional status effect applied to everyone inside (e.g. "slowness"). */
	public final String effect;
	public final int effectAmp;
	public int life;
	/** When set the zone ends as soon as this turns false (a rift closes when the creature it released dies). */
	public BooleanSupplier linked;
	protected int age;

	public Hazard(Vec3 pos, double radius, int life, double dmgMul, int interval, String style) {
		this(pos, radius, life, dmgMul, interval, style, "", 0);
	}

	public Hazard(Vec3 pos, double radius, int life, double dmgMul, int interval, String style, String effect, int effectAmp) {
		this.effect = effect;
		this.effectAmp = effectAmp;
		this.pos = pos;
		this.radius = radius;
		this.life = life;
		this.dmgMul = dmgMul;
		this.interval = Math.max(2, interval);
		this.style = style;
	}

	/** @return false once the zone has burnt out. */
	public boolean tick(UsmMob owner, ServerLevel sl) {
		age++;
		if (age % 4 == 0) {
			Styles.fill(sl, pos, radius, style, 8 + (int) (radius * 3));
		}
		if (age % 12 == 0) {
			Styles.rim(sl, pos, radius, style, 16 + (int) (radius * 4));
		}
		if (age % interval == 0) {
			for (LivingEntity v : Attacks.disc(sl, pos, radius, 1.2, 2.5)) {
				if (dmgMul > 0) {
					Attacks.hurt(owner, v, dmgMul, style);
				}
				if (!effect.isEmpty()) {
					com.ultimatesurvival.util.Effects.give(v, effect, interval + 20, effectAmp);
				}
			}
		}
		if (linked != null && !linked.getAsBoolean()) {
			return false;
		}
		return --life > 0;
	}
}
