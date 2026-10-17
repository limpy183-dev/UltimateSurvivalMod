package com.ultimatesurvival.util;

import net.minecraft.world.entity.player.Player;

/** The attack-strength a player's last melee swing was made with. Vanilla resets the charge before item hooks run, so the swing's strength is captured first. */
public interface AttackStrength {
	float usm$strength();

	static float of(Player p) {
		return p instanceof AttackStrength a ? a.usm$strength() : p.getAttackStrengthScale(0.5f);
	}
}
