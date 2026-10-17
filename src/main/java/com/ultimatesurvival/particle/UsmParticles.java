package com.ultimatesurvival.particle;

import com.google.gson.JsonElement;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.Content;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** The mod's signature particles (one per theme). Registered from the manifest; the client gives each a sprite set and a vanilla behaviour. */
public final class UsmParticles {
	public static final Map<String, SimpleParticleType> TYPES = new LinkedHashMap<>();

	private UsmParticles() {
	}

	public static void register() {
		if (!Content.root.has("particles")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("particles")) {
			String id = e.getAsString();
			SimpleParticleType t = FabricParticleTypes.simple(true);
			Registry.register(BuiltInRegistries.PARTICLE_TYPE, UltimateSurvival.id(id), t);
			TYPES.put(id, t);
		}
	}

	public static ParticleOptions get(String id, ParticleOptions fallback) {
		SimpleParticleType t = TYPES.get(id);
		return t == null ? fallback : t;
	}
}
