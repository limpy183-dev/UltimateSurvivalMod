package com.ultimatesurvival.client.gui;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.item.AccessoryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The accessory menu: six charm slots over the normal inventory, plus extra rows when the Creative cheat raised the slot count. */
public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
	private static final Identifier BG = UltimateSurvival.id("textures/gui/accessories.png");
	/** The texture is cut here (an empty strip of panel between the charm grid and the inventory) and the extra rows are drawn in the gap. */
	private static final int SPLIT = 78;
	private static final int H = 166;

	private final int extra;

	public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, H + AccessoryMenu.extraHeight(menu.slotCount));
		this.extra = AccessoryMenu.extraHeight(menu.slotCount);
		this.inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		if (extra == 0) {
			g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
			return;
		}
		g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, SPLIT, 256, 256);
		g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos + SPLIT + extra, 0.0F, (float) SPLIT, imageWidth, H - SPLIT, 256, 256);
		int top = topPos + SPLIT;
		g.fill(leftPos, top, leftPos + imageWidth, top + extra, 0xFF282242);                       // panel body
		g.fill(leftPos, top, leftPos + 1, top + extra, 0xFF0E0C16);                                // the same three-step frame as the texture
		g.fill(leftPos + 1, top, leftPos + 2, top + extra, 0xFF6E5CAA);
		g.fill(leftPos + 2, top, leftPos + 3, top + extra, 0xFF3E3468);
		g.fill(leftPos + imageWidth - 1, top, leftPos + imageWidth, top + extra, 0xFF0E0C16);
		g.fill(leftPos + imageWidth - 2, top, leftPos + imageWidth - 1, top + extra, 0xFF6E5CAA);
		g.fill(leftPos + imageWidth - 3, top, leftPos + imageWidth - 2, top + extra, 0xFF3E3468);
		for (int i = AccessoryMenu.BASE; i < menu.slotCount; i++) {
			int x = leftPos + 7 + ((i - AccessoryMenu.BASE) % AccessoryMenu.PER_ROW) * 18;
			int y = topPos + AccessoryMenu.EXTRA_Y + ((i - AccessoryMenu.BASE) / AccessoryMenu.PER_ROW) * 18;
			g.fill(x, y, x + 18, y + 18, 0xFF8C7EC4);
			g.fill(x, y, x + 17, y + 17, 0xFF0C0A14);
			g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF181428);
		}
	}
}
