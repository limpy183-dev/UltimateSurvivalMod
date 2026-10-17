package com.ultimatesurvival.codex;

import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Progress;
import com.ultimatesurvival.progress.UsmState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Handing out the Codex: once per player per world on first join, again on request. */
public final class Codex {
	private Codex() {
	}

	/** Gives the Codex if this player never received one. Returns true if a book was handed out. */
	public static boolean giveOnFirstJoin(ServerPlayer p) {
		Progress pr = UsmState.progress();
		if (!UsmConfig.get().giveCodexOnFirstJoin || pr.hasReceivedCodex(p.getUUID())) {
			return false;
		}
		pr.markCodexGiven(p.getUUID());
		give(p);
		return true;
	}

	/** Unconditionally gives a fresh Codex (command / recovery). */
	public static void give(ServerPlayer p) {
		ItemStack book = new ItemStack(UsmItems.get("codex"));
		if (!p.getInventory().add(book)) {
			p.drop(book, false);
		}
		p.sendSystemMessage(Component.translatable("codex.given").withStyle(ChatFormatting.GOLD), false);
	}
}
