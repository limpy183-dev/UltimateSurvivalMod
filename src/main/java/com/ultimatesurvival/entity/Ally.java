package com.ultimatesurvival.entity;

import net.minecraft.world.entity.player.Player;

/** Marker for mod entities that fight for a player (summoned allies), so abilities never hit them. */
public interface Ally {
	boolean ownerIs(Player player);
}
