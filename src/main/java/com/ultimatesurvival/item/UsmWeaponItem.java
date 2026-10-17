package com.ultimatesurvival.item;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.ability.Ctx;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.util.Fx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Weapons and staffs: stats come from the attribute modifiers set at registration, behaviour from
 * the item's ability list. Right-click fires the first active ability (sneak + right-click the
 * second, if any); passive abilities react to hits and kills. Locked weapons do nothing.
 */
public class UsmWeaponItem extends Item {
	private final ItemDef def;
	private final List<AbilityRef> actives = new ArrayList<>();

	public UsmWeaponItem(Item.Properties p, ItemDef def) {
		super(p);
		this.def = def;
		for (AbilityRef r : def.abilities) {
			Ability a = Abilities.get(r);
			if (a != null && a.isActive()) {
				actives.add(r);
			}
		}
	}

	public ItemDef def() {
		return def;
	}

	public List<AbilityRef> actives() {
		return actives;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (actives.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (Gates.isLocked(stack)) {
			if (player instanceof ServerPlayer sp) {
				sendLocked(sp, stack);
			}
			return InteractionResult.FAIL;
		}
		AbilityRef ref = player.isShiftKeyDown() && actives.size() > 1 ? actives.get(1) : actives.get(0);
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			Ability a = Abilities.get(ref);
			Ctx ctx = new Ctx(sl, sp, stack, ref);
			if (a.use.fire(ctx)) {
				sp.getCooldowns().addCooldown(stack, (int) Math.max(1, ref.i("cd", a.defaultCooldown) * com.ultimatesurvival.armor.Wearer.cdMult(sp)));
				Fx.cast(sl, sp, ctx.color(Fx.tierColor(def.tier)), def.tier);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
		super.hurtEnemy(stack, mob, attacker);
		if (attacker instanceof ServerPlayer sp && !Gates.isLocked(stack) && sp.level() instanceof ServerLevel sl) {
			Fx.hit(sl, mob.getBoundingBox().getCenter(), def.abilities.isEmpty() ? Fx.tierColor(def.tier) : new Ctx(sl, sp, stack, def.abilities.get(0)).color(Fx.tierColor(def.tier)), def.tier);
			for (AbilityRef r : def.abilities) {
				Ability a = Abilities.get(r);
				if (a != null && a.onHit != null) {
					a.onHit.on(new Ctx(sl, sp, stack, r), mob);
				}
			}
		}
	}

	/** Called by the kill event for the player's main-hand weapon. */
	public void killed(ServerPlayer player, ItemStack stack, LivingEntity victim) {
		if (Gates.isLocked(stack) || !(player.level() instanceof ServerLevel sl)) {
			return;
		}
		Fx.burst(sl, victim.getBoundingBox().getCenter(), ParticleTypes.SOUL, 6 + def.tier * 4, 0.4, 0.12);
		for (AbilityRef r : def.abilities) {
			Ability a = Abilities.get(r);
			if (a != null && a.onKill != null) {
				a.onKill.on(new Ctx(sl, player, stack, r), victim);
			}
		}
	}

	static void sendLocked(ServerPlayer sp, ItemStack stack) {
		Gate g = Gates.gateOf(stack.getItem());
		if (g != null) {
			sp.sendSystemMessage(Component.translatable("ultimatesurvival.msg.locked_use", g.name()).withStyle(ChatFormatting.RED), true);
		}
	}
}
