package com.ultimatesurvival.mob;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.progress.Gates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;

/** Vanilla hostile mobs grow stronger with each cleared tier, so the old world does not become a walkover. */
public final class MobScaling {
	private MobScaling() {
	}

	public static void apply(Entity entity, ServerLevel level) {
		double per = UsmConfig.get().vanillaMobScalingPerTier;
		if (per <= 0 || !(entity instanceof Monster m) || entity instanceof UsmMob || !(entity instanceof Enemy)) {
			return;
		}
		if (entity.getType() == EntityTypes.WITHER || entity.getType() == EntityTypes.WARDEN || entity.getType() == EntityTypes.ELDER_GUARDIAN) {
			return;
		}
		double bonus = per * (Gates.currentTier() - 1);
		var id = UltimateSurvival.id("tier_health");
		var hp = m.getAttribute(Attributes.MAX_HEALTH);
		if (bonus <= 0 || hp == null || hp.getModifier(id) != null) {
			return;                                                      // nothing to add, or already scaled (the modifier is saved with the mob)
		}
		hp.addPermanentModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		m.setHealth(m.getMaxHealth());
		var atk = m.getAttribute(Attributes.ATTACK_DAMAGE);
		if (atk != null) {
			atk.addPermanentModifier(new AttributeModifier(UltimateSurvival.id("tier_damage"), bonus * 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}
}
