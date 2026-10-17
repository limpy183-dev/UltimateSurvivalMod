package com.ultimatesurvival.mixin;

import com.ultimatesurvival.progress.Gates;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Locked items contribute nothing: no attribute modifiers (damage, armor ...), no mining power. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	/** Vanilla applies equipment attributes through this overload (LivingEntity.collectEquipmentChanges). */
	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
			at = @At("HEAD"), cancellable = true)
	private void usm$lockedNoModifiersSlot(CallbackInfo ci) {
		if (Gates.isLocked((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}

	/** Tooltip attribute lines use this overload. */
	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
			at = @At("HEAD"), cancellable = true)
	private void usm$lockedNoModifiersGroup(CallbackInfo ci) {
		if (Gates.isLocked((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void usm$lockedNoSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (Gates.isLocked((ItemStack) (Object) this)) {
			cir.setReturnValue(1.0F);
		}
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void usm$lockedNoDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Gates.isLocked((ItemStack) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
