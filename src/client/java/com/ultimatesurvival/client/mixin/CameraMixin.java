package com.ultimatesurvival.client.mixin;

import com.ultimatesurvival.client.ClientShake;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the boss-fight screen shake on top of wherever the camera was just aimed. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private float xRot;
	@Shadow
	private float yRot;

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void usm$shake(float partialTicks, CallbackInfo ci) {
		if (ClientShake.active()) {
			setRotation(yRot + ClientShake.yaw(partialTicks), xRot + ClientShake.pitch(partialTicks));
		}
	}
}
