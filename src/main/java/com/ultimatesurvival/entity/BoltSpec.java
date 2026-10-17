package com.ultimatesurvival.entity;

import com.ultimatesurvival.content.AbilityRef;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Everything that makes one projectile different from another: look (style), damage, flight and
 * on-hit behaviour. Weapons, staffs, mobs and bosses all describe their shots with this record.
 */
public record BoltSpec(String style, float damage, float speed, int life, int pierce, boolean homing, double gravity,
		boolean magic, float aoe, String trail, float size) {

	private static final Map<String, Integer> COLORS = Map.ofEntries(
			Map.entry("verdant", 0x6FD26A), Map.entry("azure", 0x4FD0FF), Map.entry("ember", 0xFF7A1F),
			Map.entry("storm", 0xFFE94D), Map.entry("blood", 0xD0102A), Map.entry("chrono", 0x3EC6B0),
			Map.entry("void", 0xB04BFF), Map.entry("radiant", 0xFFD34D), Map.entry("apex", 0xFF6AD5),
			Map.entry("frost", 0x7FD8FF), Map.entry("plague", 0xA6E22E), Map.entry("cosmic", 0x7B7BFF), Map.entry("eternal", 0xFFFFFF));

	public static final BoltSpec DEFAULT = new BoltSpec("ember", 6f, 1.2f, 60, 0, false, 0.0, true, 0f, "dust", 1f);

	public static BoltSpec of(AbilityRef p) {
		return new BoltSpec(p.s("style", "ember"), (float) p.d("dmg", 6), (float) p.d("speed", 1.2), p.i("life", 60),
				p.i("pierce", 0), p.b("homing", false), p.d("gravity", 0.0), p.b("magic", true), (float) p.d("aoe", 0),
				p.s("trail", "dust"), (float) p.d("size", 1));
	}

	public BoltSpec withDamage(float d) {
		return new BoltSpec(style, d, speed, life, pierce, homing, gravity, magic, aoe, trail, size);
	}

	public BoltSpec scaled(float damageMul, float speedMul) {
		return new BoltSpec(style, damage * damageMul, speed * speedMul, life, pierce, homing, gravity, magic, aoe, trail, size);
	}

	public static int colorOf(String style) {
		return COLORS.getOrDefault(style, 0xFFFFFF);
	}

	public int color() {
		return colorOf(style);
	}

	/** Trail particle: "dust" = coloured dust in the style's colour, otherwise a vanilla particle id. */
	public ParticleOptions trailParticle() {
		if (trail.equals("dust")) {
			return new net.minecraft.core.particles.DustParticleOptions(color(), 1.0f * size);
		}
		ParticleType<?> t = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.withDefaultNamespace(trail));
		return t instanceof SimpleParticleType s ? s : ParticleTypes.END_ROD;
	}
}
