package com.ultimatesurvival.client;

import com.ultimatesurvival.particle.UsmParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.SoulParticle;
import net.minecraft.client.particle.WaterDropParticle;

/** Gives every mod particle its sprites (assets/ultimatesurvival/particles/*.json) and the vanilla behaviour that suits it. */
final class ParticleSetup {
	private ParticleSetup() {
	}

	static void register() {
		ParticleProviderRegistry r = ParticleProviderRegistry.getInstance();
		r.register(UsmParticles.TYPES.get("ember_spark"), FlameParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("storm_arc"), GlowParticle.ElectricSparkProvider::new);
		r.register(UsmParticles.TYPES.get("blood_drop"), WaterDropParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("chrono_tick"), EndRodParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("void_mote"), SoulParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("radiant_spark"), EndRodParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("apex_glimmer"), EndRodParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("frost_flake"), EndRodParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("plague_spore"), SoulParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("cosmic_star"), EndRodParticle.Provider::new);
		r.register(UsmParticles.TYPES.get("eternal_prism"), EndRodParticle.Provider::new);
	}
}
