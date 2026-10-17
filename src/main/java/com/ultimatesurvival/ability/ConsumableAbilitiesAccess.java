package com.ultimatesurvival.ability;

import net.minecraft.server.level.ServerPlayer;

/** Public doorway to the heart-crystal bonus (the rest of ConsumableAbilities is package-private). */
public final class ConsumableAbilitiesAccess {
	private ConsumableAbilitiesAccess() {
	}

	public static void applyHearts(ServerPlayer p) {
		ConsumableAbilities.applyHearts(p);
	}
}
