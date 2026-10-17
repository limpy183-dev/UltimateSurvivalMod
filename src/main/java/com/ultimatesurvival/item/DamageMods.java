package com.ultimatesurvival.item;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.ability.Ctx;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Applies every damage rescaling rule. Called from LivingEntityMixin before vanilla processes a hit. */
public final class DamageMods {
	private static boolean applying;

	private DamageMods() {
	}

	public static float apply(LivingEntity victim, ServerLevel level, DamageSource source, float damage) {
		if (applying || damage <= 0) {
			return damage;
		}
		applying = true;
		try {
			// melee hit by a player holding a weapon with damage modifiers (execute, backstab ...)
			if (source.getEntity() instanceof ServerPlayer p && source.getDirectEntity() == p) {
				ItemStack held = p.getMainHandItem();
				ItemDef def = UsmItems.defOf(held.getItem());
				if (def != null && !Gates.isLocked(held)) {
					for (AbilityRef ref : def.abilities) {
						Ability a = Abilities.get(ref);
						if (a != null && a.modify != null) {
							damage = a.modify.apply(new Ctx(level, p, held, ref), victim, damage);
						}
					}
				}
			}
			// damage a player deals: worn bonuses such as blood-lust
			if (source.getEntity() instanceof ServerPlayer atk && victim != atk) {
				damage = Wearer.outgoing(atk, victim, source, damage);
			}
			// damage a player is about to take: armor-set / accessory bonuses
			if (victim instanceof ServerPlayer sp) {
				Wearer.markHurt(sp);
				damage = Wearer.incoming(sp, source, damage);
			}
		} finally {
			applying = false;
		}
		return damage;
	}
}
