package com.ultimatesurvival.ability;

import net.minecraft.world.entity.LivingEntity;

/**
 * One behaviour a weapon/armor/accessory can have. An ability fills in whichever hooks it uses:
 * {@code use} (right-click, returns true if it fired so the cooldown starts), {@code onHit} (after
 * a melee hit), {@code onKill}, or {@code modify} (rescales the damage of a melee hit before it lands).
 */
public final class Ability {
	@FunctionalInterface
	public interface Use {
		boolean fire(Ctx c);
	}

	@FunctionalInterface
	public interface Hit {
		void on(Ctx c, LivingEntity target);
	}

	@FunctionalInterface
	public interface Modify {
		float apply(Ctx c, LivingEntity target, float damage);
	}

	/** Set-bonus / accessory hooks (per-player, not per-weapon). */
	@FunctionalInterface
	public interface PlayerTick {
		void run(net.minecraft.server.level.ServerPlayer p, com.ultimatesurvival.content.AbilityRef r);
	}

	@FunctionalInterface
	public interface Incoming {
		float apply(net.minecraft.server.level.ServerPlayer victim, net.minecraft.world.damagesource.DamageSource src, float dmg,
				com.ultimatesurvival.content.AbilityRef r);
	}

	/** Rescales (or reacts to) damage the wearer deals. */
	@FunctionalInterface
	public interface Outgoing {
		float apply(net.minecraft.server.level.ServerPlayer attacker, net.minecraft.world.entity.LivingEntity victim,
				net.minecraft.world.damagesource.DamageSource src, float dmg, com.ultimatesurvival.content.AbilityRef r);
	}

	public final String id;
	public Use use;
	public Hit onHit;
	public Hit onKill;
	public Modify modify;
	public PlayerTick tick;      // every tick while an armor set / accessory grants this
	public PlayerTick onEquip;   // once when it starts being granted
	public PlayerTick onRemove;  // once when it stops
	public Incoming incoming;    // rescales damage the wearer takes
	public Outgoing outgoing;    // rescales damage the wearer deals
	public java.util.function.Predicate<Ctx> canUse; // consumables: refuse to start (e.g. already at max)
	/** Default cooldown in ticks used when the content row does not give "cd". */
	public int defaultCooldown = 100;

	Ability(String id) {
		this.id = id;
	}

	public boolean isActive() {
		return use != null;
	}
}
