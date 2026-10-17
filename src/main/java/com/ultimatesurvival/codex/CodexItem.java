package com.ultimatesurvival.codex;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The guide book. Right-click opens the Codex screen on the client (the hook is installed by the client entrypoint). */
public class CodexItem extends Item {
	/** Set by the client entrypoint; null on a dedicated server. */
	public static Runnable openScreen;

	public CodexItem(Item.Properties p) {
		super(p);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() && openScreen != null) {
			openScreen.run();
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean carries(Player p) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			if (p.getInventory().getItem(i).getItem() instanceof CodexItem) {
				return true;
			}
		}
		return p.getOffhandItem().getItem() instanceof CodexItem;
	}
}
