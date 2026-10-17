package com.ultimatesurvival.ability;

import com.ultimatesurvival.content.AbilityRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Everything an ability needs when it fires: where, who, with what, and its tuning parameters. */
public record Ctx(ServerLevel level, ServerPlayer player, ItemStack stack, AbilityRef p) {
	public Vec3 eye() {
		return player.getEyePosition();
	}

	public Vec3 look() {
		return player.getLookAngle();
	}

	/** Colour for effects: explicit "color" param (hex int) or the ability's "style" palette. */
	public int color(int fallback) {
		if (p.params().has("color")) {
			return p.i("color", fallback);
		}
		return p.params().has("style") ? com.ultimatesurvival.entity.BoltSpec.colorOf(p.s("style", "ember")) : fallback;
	}
}
