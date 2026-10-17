package com.ultimatesurvival.item;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.ability.Ctx;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Drinkable / edible items whose effects are abilities (heal, effects, permanent upgrades ...). */
public class UsmConsumable extends Item {
	private final ItemDef def;

	public UsmConsumable(Item.Properties p, ItemDef def) {
		super(p);
		this.def = def;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (Gates.isLocked(stack)) {
			if (player instanceof ServerPlayer sp) {
				UsmWeaponItem.sendLocked(sp, stack);
			}
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			for (AbilityRef r : def.abilities) {
				Ability a = Abilities.get(r);
				if (a != null && a.canUse != null && !a.canUse.test(new Ctx(sl, sp, stack, r))) {
					return InteractionResult.FAIL;
				}
			}
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (level instanceof ServerLevel sl && entity instanceof ServerPlayer sp && !Gates.isLocked(stack)) {
			for (AbilityRef r : def.abilities) {
				Ability a = Abilities.get(r);
				if (a != null && a.use != null) {
					a.use.fire(new Ctx(sl, sp, stack, r));
				}
			}
		}
		return super.finishUsingItem(stack, level, entity);
	}
}
