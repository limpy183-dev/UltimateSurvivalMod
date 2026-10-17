package com.ultimatesurvival.client.codex;

import com.ultimatesurvival.UltimateSurvival;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small drawing helpers shared by the Codex pages (parchment-friendly colours, wrapped text, slots, icons). */
final class UiKit {
	static final int INK = 0xFF2A1A0A;
	static final int BODY = 0xFF4A3520;
	static final int MUTED = 0xFF8A7254;
	static final int GOLD = 0xFF8A5A10;
	static final int GREEN = 0xFF2E6B2E;
	static final int RED = 0xFF8A2020;
	static final int BLUE = 0xFF2A4F8A;
	static final int PURPLE = 0xFF5A2E8A;

	static final Identifier ICONS = UltimateSurvival.id("textures/gui/codex_icons.png");
	static final int ICON_BOSS = 0;
	static final int ICON_ITEMS = 1;
	static final int ICON_MOB = 2;
	static final int ICON_PROGRESS = 3;
	static final int ICON_TIP = 4;
	static final int ICON_LOCK = 5;
	static final int ICON_QUESTION = 6;
	static final int ICON_CHECK = 7;

	private UiKit() {
	}

	/** Draws wrapped text; returns the y below the last line. */
	static int paragraph(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			g.text(font, line, x, y, color, false);
			y += 9;
		}
		return y;
	}

	static int paragraph(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
		return paragraph(g, font, Component.literal(text), x, y, width, color);
	}

	static int lines(Font font, Component text, int width) {
		return font.split(text, width).size();
	}

	static void icon(GuiGraphicsExtractor g, int index, int x, int y, int size, int tint) {
		g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, index * 16f, 0f, size, size, 16, 16, 128, 16, tint);
	}

	/** Inset item slot (dark box) used for recipe grids and reward rows. */
	static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, 0xFF2A2018);
		g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF4A3C2C);
		g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF1C1610);
		g.fill(x + 1, y + 1, x + 2, y + 17, 0xFF1C1610);
	}

	static void rule(GuiGraphicsExtractor g, int x, int y, int w, int color) {
		g.fill(x, y, x + w, y + 1, color);
	}

	/** Section heading with a thin rule underneath. */
	static int heading(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int w) {
		g.text(font, text, x, y, GOLD, false);
		rule(g, x, y + 9, w, 0x66A0782C);
		return y + 13;
	}

	static ItemStack stackOf(String id, long time) {
		if (id.startsWith("#")) {
			var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.parse(id.substring(1)));
			var holders = BuiltInRegistries.ITEM.getTagOrEmpty(tag);
			java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> list = new java.util.ArrayList<>();
			holders.forEach(list::add);
			if (list.isEmpty()) {
				return ItemStack.EMPTY;
			}
			return new ItemStack(list.get((int) ((time / 30) % list.size())).value());
		}
		Identifier key = id.contains(":") ? Identifier.parse(id) : UltimateSurvival.id(id);
		return BuiltInRegistries.ITEM.get(key).map(h -> new ItemStack(h.value())).orElse(new ItemStack(Items.BARRIER));
	}

	static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	static int tierRgb(int tier) {
		return switch (tier) {
			case 1 -> 0x3F8F3F;
			case 2 -> 0x2F7FB8;
			case 3 -> 0xC27A18;
			case 4 -> 0xB83030;
			case 5 -> 0x8A44C8;
			default -> 0x2A9DB8;
		};
	}
}
