package com.ultimatesurvival.mixin;

import com.ultimatesurvival.progress.DragonGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla dragon never calls LivingEntity#die (its killing blow is intercepted and it plays a death
 * animation in tickDeath, or is removed outright by /kill), so Fabric's death events never fire for it.
 * These hooks open the tier-2 gate and drop drakescale at the right moments instead.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
	@Shadow
	public int dragonDeathTime;

	@Inject(method = "tickDeath", at = @At("HEAD"))
	private void usm$dying(CallbackInfo ci) {
		EnderDragon self = (EnderDragon) (Object) this;
		if (self.level() instanceof ServerLevel level) {
			if (dragonDeathTime == 0) {
				DragonGate.defeated(level);
			} else if (dragonDeathTime == 199) {
				DragonGate.dropLoot(level, self);
			}
		}
	}

	@Inject(method = "kill", at = @At("HEAD"))
	private void usm$killed(ServerLevel level, CallbackInfo ci) {
		DragonGate.defeated(level);
	}
}
