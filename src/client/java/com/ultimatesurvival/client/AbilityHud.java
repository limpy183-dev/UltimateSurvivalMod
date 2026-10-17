package com.ultimatesurvival.client;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.Ability;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Small panel beside the hotbar for the held weapon's right-click abilities: name, cooldown bar,
 * seconds left, and a pulse the moment an ability becomes ready again. Sneak highlights the second ability.
 */
public final class AbilityHud implements HudElement {
	private Item lastItem;
	private boolean wasCooling;
	private long readyFlashUntil;

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, UltimateSurvival.id("ability_hud"), new AbilityHud());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		if (p == null) {
			return;
		}
		ItemStack held = p.getMainHandItem();
		ItemDef def = UsmItems.defOf(held.getItem());
		if (def == null) {
			lastItem = null;
			return;
		}
		Language lang = Language.getInstance();
		String base = held.getItem().getDescriptionId();
		java.util.List<String[]> rows = new java.util.ArrayList<>();      // [title, cdSeconds]
		java.util.List<AbilityRef> refs = new java.util.ArrayList<>();
		for (int n = 0; n < def.abilities.size(); n++) {
			AbilityRef r = def.abilities.get(n);
			Ability a = Abilities.get(r);
			if (a != null && a.isActive() && lang.has(base + ".ab." + n)) {
				String[] part = lang.getOrDefault(base + ".ab." + n).split("\\|", -1);
				rows.add(new String[] {part[1], String.valueOf(r.i("cd", a.defaultCooldown))});
				refs.add(r);
			}
		}
		if (rows.isEmpty()) {
			lastItem = null;
			return;
		}
		boolean locked = Gates.isLocked(held);
		float pct = p.getCooldowns().getCooldownPercent(held, delta.getGameTimeDeltaPartialTick(false));
		boolean cooling = pct > 0.0f;
		long now = mc.level == null ? 0 : mc.level.getGameTime();
		if (lastItem == held.getItem() && wasCooling && !cooling) {
			readyFlashUntil = now + 14;
		}
		lastItem = held.getItem();
		wasCooling = cooling;

		Font font = mc.font;
		int titleW = 0;
		for (String[] row : rows) {
			titleW = Math.max(titleW, font.width(row[0]));
		}
		int w = Math.max(112, 24 + titleW + 6 + font.width("READY!") + 4);   // wide enough that the status text never overlaps any title
		int h = 8 + rows.size() * 15;
		int x = g.guiWidth() / 2 + 98 + 24;
		int y = g.guiHeight() - 22 - h + 20;
		if (x + w > g.guiWidth() - 4) {
			x = g.guiWidth() - w - 4;
		}
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xCC0B0A14);
		g.outline(x - 1, y - 1, w + 2, h + 2, locked ? 0xFF8A2A2A : 0xFF6E5CAA);
		g.item(held, x + 3, y + 3);
		int selected = p.isShiftKeyDown() && rows.size() > 1 ? 1 : 0;
		for (int i = 0; i < rows.size(); i++) {
			int ry = y + 4 + i * 15;
			int tx = x + 24;
			boolean sel = i == selected;
			g.text(font, rows.get(i)[0], tx, ry, locked ? 0xFFAA4444 : sel ? 0xFFFFE066 : 0xFF8F8AA8, true);
			int barX = tx;
			int barW = w - 28;
			int barY = ry + 10;
			g.fill(barX, barY, barX + barW, barY + 2, 0xFF1C1830);
			if (locked) {
				g.fill(barX, barY, barX + barW, barY + 2, 0xFF5A2020);
			} else if (cooling && sel) {
				int fill = (int) (barW * (1.0f - pct));
				g.fill(barX, barY, barX + fill, barY + 2, 0xFF58B8FF);
				float secs = pct * Integer.parseInt(rows.get(i)[1]) / 20.0f;
				String s = String.format(java.util.Locale.ROOT, "%.1fs", secs);
				g.text(font, s, x + w - 4 - font.width(s), ry, 0xFFB8D8FF, true);
			} else {
				boolean flash = now < readyFlashUntil;
				int pulse = (int) (150 + 100 * Math.sin(now * 0.35));
				int col = flash ? 0xFFFFFFFF : 0xFF000000 | (0x60 << 16) | (pulse << 8) | 0x60;
				g.fill(barX, barY, barX + barW, barY + 2, col);
				if (sel) {
					String s = flash ? "READY!" : "ready";
					g.text(font, s, x + w - 4 - font.width(s), ry, flash ? 0xFFFFFFFF : 0xFF7FE07F, true);
				}
			}
		}
		if (locked) {
			g.text(font, "LOCKED", x + 3, y + h - 9, 0xFFFF5555, true);
		}
	}
}
