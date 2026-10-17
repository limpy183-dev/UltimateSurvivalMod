package com.ultimatesurvival.mixin;

import com.ultimatesurvival.config.UsmConfig;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Config: stackable items (max stack above 1) hold up to stackSizeCap (never above 99, the save-format limit). */
@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
	@Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
	private void usm$biggerStacks(CallbackInfoReturnable<Integer> cir) {
		int cap = Math.min(99, UsmConfig.get().stackSizeCap);
		int v = cir.getReturnValue();
		if (cap > v && v > 1) {
			cir.setReturnValue(cap);
		}
	}
}
