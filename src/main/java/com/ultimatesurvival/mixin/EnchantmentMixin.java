package com.ultimatesurvival.mixin;

import com.ultimatesurvival.config.UsmConfig;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raises the max level of every multi-level enchantment to the configured cap (anvil, /enchant, loot all follow). */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true)
	private void usm$raiseCap(CallbackInfoReturnable<Integer> cir) {
		int cap = UsmConfig.get().enchantmentMaxLevel;
		int v = cir.getReturnValue();
		if (cap > 0 && v > 1 && v < cap) {
			cir.setReturnValue(cap);
		}
	}
}
