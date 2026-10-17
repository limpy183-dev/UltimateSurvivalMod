package com.ultimatesurvival.item;

import com.ultimatesurvival.content.ItemDef;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A boss summoning item. It does nothing alone - it is spent on a Ritual Altar (see RitualAltarBlock). */
public class SigilItem extends Item {
	private final String boss;

	public SigilItem(Item.Properties p, ItemDef def) {
		super(p);
		this.boss = def.str("boss", "");
	}

	public String boss() {
		return boss;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("ultimatesurvival.ritual.sigil_hint"));
		}
		return InteractionResult.SUCCESS;
	}
}
