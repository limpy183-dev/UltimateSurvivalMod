package com.ultimatesurvival.item;

import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.util.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A boss's treasure bag: right-click to open it and roll the boss's loot rows. */
public class BagItem extends Item {
	private final String boss;

	public BagItem(Item.Properties p, ItemDef def) {
		super(p);
		this.boss = def.str("boss", "");
	}

	public String boss() {
		return boss;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack bag = player.getItemInHand(hand);
		if (!(level instanceof ServerLevel sl)) {
			return InteractionResult.SUCCESS;
		}
		MobDef def = Mobs.DEFS.get(boss);
		if (def == null) {
			return InteractionResult.FAIL;
		}
		// the game replaces the held stack with the result below, so the loot must not be put into the (still occupied) hand slot first
		ItemStack remaining = player.getAbilities().instabuild ? bag : bag.copyWithCount(bag.getCount() - 1);
		Fx.burst(sl, player.position().add(0, 0.9, 0), ParticleTypes.END_ROD, 24, 1.1, 0.12);
		Fx.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.1f);
		Fx.sound(sl, player.position(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);
		for (MobDef.Drop d : def.bag) {
			if (sl.getRandom().nextDouble() > d.chance()) {
				continue;
			}
			int n = d.min() + (d.max() > d.min() ? sl.getRandom().nextInt(d.max() - d.min() + 1) : 0);
			ItemStack st = Mobs.stack(d.item(), n);
			if (st.isEmpty()) {
				continue;
			}
			Component name = Mobs.displayName(st);
			if (!player.getInventory().add(st)) {
				player.drop(st, false);
			}
			player.sendSystemMessage(Component.literal("  + " + n + "x ").withStyle(ChatFormatting.GRAY).append(name));
		}
		return InteractionResult.SUCCESS.heldItemTransformedTo(remaining);
	}
}
