package com.ultimatesurvival.client.mixin;

import com.ultimatesurvival.config.UsmConfig;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Matches AnvilMenuMixin: the screen must not print "Too Expensive!" once the cap is gone. */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
	@ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
	private int usm$noTooExpensive(int vanilla) {
		return UsmConfig.get().removeAnvilTooExpensive ? Integer.MAX_VALUE : vanilla;
	}
}
