package com.ultimatesurvival.mixin;

import com.ultimatesurvival.util.AttackStrength;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements AttackStrength {
	@Unique
	private float usm$strength = 1.0f;

	@Inject(method = "onAttack", at = @At("HEAD"))
	private void usm$capture(CallbackInfo ci) {
		usm$strength = ((Player) (Object) this).getAttackStrengthScale(0.5f);
	}

	@Override
	public float usm$strength() {
		return usm$strength;
	}
}
