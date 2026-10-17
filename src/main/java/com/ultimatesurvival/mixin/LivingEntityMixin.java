package com.ultimatesurvival.mixin;

import com.ultimatesurvival.item.DamageMods;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** One choke point for damage rescaling: weapon modifiers, set bonuses, accessories, boss caps. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float usm$modifyDamage(float damage, ServerLevel level, DamageSource source) {
		return DamageMods.apply((LivingEntity) (Object) this, level, source, damage);
	}
}
