package com.ultimatesurvival.mixin;

import com.ultimatesurvival.config.UsmConfig;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Removes the "Too Expensive!" cutoff (the third literal 40 in createResult is the final result gate). */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@ModifyConstant(method = "createResult", constant = @Constant(intValue = 40, ordinal = 2))
	private int usm$noTooExpensive(int vanilla) {
		return UsmConfig.get().removeAnvilTooExpensive ? Integer.MAX_VALUE : vanilla;
	}
}
