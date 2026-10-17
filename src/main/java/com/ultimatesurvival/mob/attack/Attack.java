package com.ultimatesurvival.mob.attack;

import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmMob;
import net.minecraft.world.entity.LivingEntity;

/** One kind of boss/mob attack, configured by its row in the content tables. Stateless: all per-use state lives in {@link Run}. */
public abstract class Attack {
	public final String type;
	public final AbilityRef p;
	public final int weight;
	private final double minRange;
	private final double maxRange;

	protected Attack(MobDef.AttackSpec spec) {
		this.type = spec.type();
		this.p = spec.p();
		this.weight = Math.max(1, spec.weight());
		double[] range = p.pair("range", 0, 64);
		this.minRange = range[0];
		this.maxRange = range[1];
	}

	/** Schedule the whole attack on {@code r}: set {@code r.windup} / {@code r.total} and add timed events. */
	public abstract void begin(Run r);

	/** Name of the rig animation played while this attack runs. */
	public String anim() {
		return p.s("anim", type);
	}

	/** Damage as a multiple of the mob's base damage. */
	public double mult() {
		return p.d("dmg", 1.0);
	}

	public boolean usable(UsmMob self, LivingEntity target) {
		double d = Math.sqrt(self.distanceToSqr(target.getX(), self.getY(), target.getZ()));
		return d >= minRange && d <= maxRange;
	}

	/** Rough total length in ticks (windup + recovery + any timed part); the Codex preview uses it to pace the animation. */
	public int estimateLength() {
		int active = p.i("duration", 0);
		int count = p.i("count", 1);
		switch (type) {
			case "pillars", "meteors", "lightning" -> active = (count - 1) * p.i("interval", type.equals("lightning") ? 8 : 7) + p.i("delay", 24);
			case "orbs" -> active = (count - 1) * p.i("interval", 14);
			case "stop" -> active = p.i("hold", 50);
			case "perch" -> active = p.i("stun", 90);
			case "exposed", "reflect" -> active = p.i("duration", 85);
			case "leap" -> active = p.i("air", 18) + 16;
			case "leech" -> active = p.i("duration", 60);
			case "fan" -> active = (p.i("volleys", 1) - 1) * p.i("interval", 8);
			case "ring" -> active = (int) (p.d("max_radius", 22) / p.d("speed", 0.6));
			case "charge" -> active = (int) Math.ceil(p.d("dist", 22) / p.d("speed", 1.05)) + p.i("stun", 50);
			case "blackhole" -> active = 6;
			case "cage" -> active = p.i("close", 56) + 12;
			case "beam" -> active = p.i("duration", 70);
			case "supernova" -> active = p.i("exhaust", 90);
			case "sunder" -> active = (int) Math.ceil(p.d("length", 44) / p.d("speed", 2.4));
			case "icicles" -> active = (int) Math.ceil(p.d("length", 30) / p.d("speed", 1.5)) + (p.i("waves", 1) - 1) * p.i("gap", 14);
			case "whiteout", "planets" -> active = p.i("duration", type.equals("planets") ? 140 : 160);
			case "glaciate" -> active = p.i("delay", 30) + p.i("shards", 3) * 6;
			case "spores" -> active = (count - 1) * 4 + p.i("flight", 28);
			case "infest" -> active = p.i("fuse", 90);
			case "constellation" -> active = p.i("hold", 44);
			case "tether" -> active = 16;
			default -> {
			}
		}
		return p.i("windup", 24) + p.i("recover", 20) + active;
	}

	public String style() {
		return p.s("style", "ember");
	}
}
