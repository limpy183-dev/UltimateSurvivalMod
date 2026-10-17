package com.ultimatesurvival.client.rig;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;

/** Registers a model layer and a renderer for every manifest mob. */
public final class Rigs {
	private Rigs() {
	}

	public static ModelLayerLocation layerOf(String id) {
		return new ModelLayerLocation(UltimateSurvival.id(id), "main");
	}

	public static void register() {
		for (MobDef d : Mobs.DEFS.values()) {
			RigDef rig = RigDef.load(d.id);
			ModelLayerLocation loc = layerOf(d.id);
			ModelLayerRegistry.registerModelLayer(loc, () -> RigModel.createLayer(rig));
			Identifier tex = UltimateSurvival.id("textures/entity/" + d.id + ".png");
			Identifier glow = UltimateSurvival.id("textures/entity/" + d.id + "_glow.png");
			float shadow = Math.max(0.4f, d.width * 0.55f);
			EntityRenderers.register(Mobs.TYPES.get(d.id), ctx -> new RigRenderer(ctx, rig, loc, tex, glow, shadow));
		}
	}
}
