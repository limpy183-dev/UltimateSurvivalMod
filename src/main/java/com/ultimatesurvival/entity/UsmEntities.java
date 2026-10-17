package com.ultimatesurvival.entity;

import com.ultimatesurvival.UltimateSurvival;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity type registry. Mobs and bosses are added from the content manifest in later phases. */
public final class UsmEntities {
	public static final EntityType<Bolt> BOLT = register("bolt",
			EntityType.Builder.<Bolt>of(Bolt::new, MobCategory.MISC).sized(0.35f, 0.35f).clientTrackingRange(8).updateInterval(1).noSave());

	private UsmEntities() {
	}

	public static void init() {
		// class load registers the static fields
	}

	public static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, UltimateSurvival.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
