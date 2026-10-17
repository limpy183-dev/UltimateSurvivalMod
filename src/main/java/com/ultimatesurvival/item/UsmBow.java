package com.ultimatesurvival.item;

import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.entity.Bolt;
import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.util.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bows come in two flavours chosen by the "ammo" stat: arrow bows (vanilla arrows, damage multiplied)
 * and energy bows that fire {@link Bolt}s without ammo using the item's "bow_shot" ability parameters.
 */
public class UsmBow extends BowItem {
	private final ItemDef def;
	private final AbilityRef shot;

	public UsmBow(Properties p, ItemDef def) {
		super(p);
		this.def = def;
		AbilityRef s = null;
		for (AbilityRef r : def.abilities) {
			if (r.id().equals("bow_shot")) {
				s = r;
			}
		}
		this.shot = s;
	}

	private boolean energy() {
		return def.stat("ammo", 1) <= 0 && shot != null;
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
		if (energy()) {
			player.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		return super.use(level, player, hand);
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
		if (Gates.isLocked(stack)) {
			return false;
		}
		if (!energy()) {
			return super.releaseUsing(stack, level, entity, remaining);
		}
		if (!(entity instanceof Player player)) {
			return false;
		}
		float draw = (float) def.stat("draw", 18);
		float t = (getUseDuration(stack, entity) - remaining) / draw;
		float pow = Math.min(1.0f, (t * t + t * 2.0f) / 3.0f);
		if (pow < 0.12f) {
			return false;
		}
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			BoltSpec spec = BoltSpec.of(shot).scaled(0.35f + 0.65f * pow, 0.5f + 0.5f * pow);
			int n = Math.max(1, shot.i("count", 1));
			double spread = Math.toRadians(shot.d("spread", 10));
			Vec3 look = sp.getLookAngle();
			Vec3 origin = sp.getEyePosition().add(look.scale(0.5)).add(0, -0.2, 0);
			for (int i = 0; i < n; i++) {
				double a = n == 1 ? 0 : -spread / 2 + spread * i / (n - 1);
				Bolt.fire(sl, sp, origin, look.yRot((float) -a), spec);
			}
			Fx.sound(sl, origin, SoundEvents.ARROW_SHOOT, 0.9f, 1.5f + pow * 0.4f);
			Fx.sound(sl, origin, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 1.2f + pow * 0.5f);
			Fx.burst(sl, origin, Fx.dust(spec.color(), 1.1f), 6, 0.15, 0.05);
			Fx.burst(sl, origin.add(look.scale(0.6)), net.minecraft.core.particles.ParticleTypes.END_ROD, 4 + def.tier * 2, 0.25, 0.2 + 0.05 * def.tier);
			if (def.tier >= 4) {
				Fx.ring(sl, origin.add(look.scale(0.8)), 0.6 + 0.1 * def.tier, 12 + def.tier * 2, Fx.dust(spec.color(), 1.0f), 0.0);
			}
			stack.hurtAndBreak(1, sp, player.getUsedItemHand().asEquipmentSlot());
		}
		return true;
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
		Projectile p = super.createProjectile(level, shooter, weapon, ammo, crit);
		if (p instanceof AbstractArrow a) {
			a.setBaseDamage(2.0 * def.stat("arrow_mult", 1.4)); // vanilla arrow base damage is 2.0
			if (def.stat("nograv", 0) > 0) {
				a.setNoGravity(true);
			}
		}
		return p;
	}
}
