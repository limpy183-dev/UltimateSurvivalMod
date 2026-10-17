package com.ultimatesurvival.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets us raise the vanilla 1024 cap of the max-health attribute (bosses have more health than that). */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
	@Mutable
	@Accessor("maxValue")
	void usm$setMaxValue(double max);
}
