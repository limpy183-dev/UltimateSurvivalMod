package com.ultimatesurvival.client.codex;

import static com.ultimatesurvival.client.codex.UiKit.*;

import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.armor.ArmorSets;
import com.ultimatesurvival.cheat.Cheats;
import com.ultimatesurvival.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.ultimatesurvival.client.codex.CodexModel.Kind;
import com.ultimatesurvival.client.codex.CodexModel.Row;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.UsmState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Codex: a custom book GUI (not the vanilla book screen). Five tabs, a list on the left page and the
 * detail of the selected entry on the right. Undiscovered entries read "???" until you find, meet or beat them.
 */
public class CodexScreen extends Screen {
	private static final int PW = 360;
	private static final int PH = 220;
	private static final Identifier PANEL = UltimateSurvival.id("textures/gui/codex.png");
	private static final int ROW_H = 17;
	private static final String[] TAB_KEYS = {"bosses", "items", "mobs", "progress", "tips", "cheats"};
	private static final int TAB_CHEATS = 5;
	private static final int[] TAB_ICON = {ICON_BOSS, ICON_ITEMS, ICON_MOB, ICON_PROGRESS, ICON_TIP, -1};   // -1: a command block item
	private static final int[] TAB_COL = {0xFFB83A3A, 0xFF3A6EB8, 0xFF3A9A52, 0xFFC8A030, 0xFF8A5AC8, 0xFF2A9A9A};
	/** Parchment seen through the row highlight: the veil laid over a faded icon takes the colour of what is behind it, so only the icon fades. */
	private static final int VEIL_PLAIN = 0xA6E0D0A8;
	private static final int VEIL_HOVER = 0xA6C3B592;
	private static final int VEIL_SELECTED = 0xA6D6BD7E;	/** How far either side of a scrollbar the mouse may be and still grab it: the bars are only three pixels wide. */
	private static final int GRAB = 4;
	private static final int BAR_LIST = 0;
	private static final int BAR_DETAIL = 1;
	private static final int BAR_PROGRESS = 2;
	private static final int BAR_TIPS = 3;
	/** The tips are one flow of lines across both pages: both must wrap at the same width or the seam repeats text. */
	private static final int TIP_WRAP = 148;

	/** Where the book was left (tab, tier filter, selected entry per tab), so it reopens on the same page. Cleared only by restarting the game. */
	private static int lastTab = 1;
	private static int lastTier;
	private static final int[] lastSelected = {-1, -1, -1, -1, -1, 0};

	private int tab = lastTab;
	private final int[] scroll = new int[6];
	private final int[] selected = lastSelected.clone();
	private int tierFilter = lastTier;
	private int detailScroll;
	private int detailHeight;
	private int leftHeight;
	private int openTicks;
	private List<Row> rows = new ArrayList<>();
	private int px;
	private int py;
	private int mouseX;
	private int mouseY;
	private final List<Bar> bars = new ArrayList<>();
	private int dragging = -1;
	private int grab;
	/** Clickable controls of the Creative page as drawn this frame. */
	private final List<Btn> btns = new ArrayList<>();
	private final java.util.Map<String, Integer> choice = new java.util.HashMap<>();

	private record Btn(int x, int y, int w, int h, Runnable run) {
	}

	/** A scrollbar as drawn this frame, remembered so the mouse can grab it. Values are in the bar's own units (rows, pixels or lines). */
	private record Bar(int id, int x, int top, int bottom, int content, int view, IntSupplier value, IntConsumer setter) {
		int thumbHeight() {
			return Math.max(14, (bottom - top) * view / Math.max(1, content));                    // never smaller than something a mouse can pick up
		}

		int max() {
			return Math.max(0, content - view);
		}

		int travel() {
			return Math.max(1, bottom - top - thumbHeight());
		}

		int thumbTop() {
			return top + travel() * Mth.clamp(value.getAsInt(), 0, max()) / Math.max(1, max());
		}

		boolean grabs(int mx, int my) {
			return mx >= x - GRAB && mx < x + 3 + GRAB && my >= top && my < bottom;
		}
	}

	public CodexScreen() {
		super(Component.translatable("codex.title"));
	}

	public CodexScreen openAt(int tabIndex) {
		this.tab = tabIndex;
		return this;
	}

	@Override
	protected void init() {
		leaveCheatsIfSurvival();
		rebuildRows();
		if (openTicks == 0) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9f, 0.9f));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.55f));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		openTicks++;
		MobPreview.tick();
	}

	@Override
	public void removed() {
		lastTab = tab;
		lastTier = tierFilter;
		System.arraycopy(selected, 0, lastSelected, 0, selected.length);
		MobPreview.clear();
		super.removed();
	}

	/** The Creative page exists only in creative mode (the server ignores cheats from anyone else); a book left open there falls back to Progress. */
	private boolean creative() {
		return minecraft != null && minecraft.player != null && minecraft.player.isCreative();
	}

	private int tabCount() {
		return creative() ? 6 : 5;
	}

	private void leaveCheatsIfSurvival() {
		if (tab == TAB_CHEATS && !creative()) {
			tab = 3;
			dragging = -1;
			detailScroll = 0;
			rebuildRows();
		}
	}

	/** Entries in the current tab's left-hand list. */
	private int listSize() {
		return tab == TAB_CHEATS ? Cheats.ALL.size() : rows.size();
	}

	private void rebuildRows() {
		if (tab == TAB_CHEATS) {
			selected[tab] = Mth.clamp(selected[tab], 0, Cheats.ALL.size() - 1);
			bringSelectionIntoView();
			return;
		}
		rows = switch (tab) {
			case 0 -> CodexModel.bosses();
			case 1 -> CodexModel.items(tierFilter);
			case 2 -> CodexModel.mobs();
			default -> new ArrayList<>();
		};
		if (selected[tab] >= rows.size()) {
			selected[tab] = -1;
		}
		if (selected[tab] < 0 && !rows.isEmpty() && (tab == 0 || tab == 1)) {
			selected[tab] = 0;
			for (int i = 0; i < rows.size(); i++) {
				if (rows.get(i).known()) {
					selected[tab] = i;
					break;
				}
			}
		}
		bringSelectionIntoView();
	}

	private void bringSelectionIntoView() {
		int visible = Math.max(1, (188 - 31) / ROW_H);                   // bring a restored selection into view
		if (selected[tab] >= scroll[tab] + visible) {
			scroll[tab] = selected[tab] - visible + 1;
		}
	}

	private void setTab(int t) {
		if (t != tab) {
			tab = t;
			dragging = -1;
			detailScroll = 0;
			rebuildRows();
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f + 0.1f * t, 0.7f));
		}
	}

	private Row selectedRow() {
		int s = selected[tab];
		return s >= 0 && s < rows.size() ? rows.get(s) : null;
	}

	// ====================================================================================================
	// rendering
	// ====================================================================================================
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		mouseX = mx;
		mouseY = my;
		leaveCheatsIfSurvival();
		bars.clear();
		btns.clear();
		extractTransparentBackground(g);
		px = (width - PW) / 2;
		py = (height - PH) / 2;
		float t = Mth.clamp((openTicks + partial) / 9.0f, 0.0f, 1.0f);
		float e = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
		int alpha = (int) (255 * Mth.clamp(e * 1.4f, 0f, 1f));
		g.pose().pushMatrix();
		float cx = width / 2.0f;
		float cy = height / 2.0f;
		g.pose().translate(cx, cy);
		float sc = 0.82f + 0.18f * e;
		g.pose().scale(sc, sc);
		g.pose().translate(-cx, -cy);

		drawTabs(g);
		g.blit(RenderPipelines.GUI_TEXTURED, PANEL, px, py, 0f, 0f, PW, PH, PW, PH, PW, PH, argb(alpha, 0xFFFFFF));
		if (t > 0.45f) {
			drawLeft(g);
			drawRight(g);
		}
		g.pose().popMatrix();
		drawFlourish(g, partial);
	}

	private void drawTabs(GuiGraphicsExtractor g) {
		for (int i = 0; i < tabCount(); i++) {
			int x = px - 20 - (i == tab ? 3 : 0);
			int y = py + 14 + i * 31;
			boolean hover = inRect(mouseX, mouseY, x, y, 26, 26);
			int base = TAB_COL[i];
			g.fill(x - 1, y - 1, x + 27, y + 27, 0xFF120A1E);
			g.fill(x, y, x + 26, y + 26, i == tab ? base : darker(base, hover ? 0.78f : 0.55f));
			g.fill(x, y, x + 26, y + 2, 0x55FFFFFF);
			if (TAB_ICON[i] < 0) {
				g.item(new ItemStack(Items.COMMAND_BLOCK), x + 5, y + 5);
			} else {
				icon(g, TAB_ICON[i], x + 5, y + 5, 16, i == tab || hover ? 0xFFFFFFFF : 0xFFB8B0C8);
			}
			if (hover) {
				g.setTooltipForNextFrame(font, Component.translatable("codex.tab." + TAB_KEYS[i]), mouseX, mouseY);
			}
		}
	}

	private static int darker(int argb, float f) {
		int r = (int) (((argb >> 16) & 0xFF) * f);
		int gr = (int) (((argb >> 8) & 0xFF) * f);
		int b = (int) ((argb & 0xFF) * f);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}

	private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void drawFlourish(GuiGraphicsExtractor g, float partial) {
		float age = openTicks + partial;
		if (age > 36) {
			return;
		}
		for (int i = 0; i < 26; i++) {
			float phase = (age - i * 0.9f) / 22.0f;
			if (phase <= 0 || phase >= 1) {
				continue;
			}
			int h = (i * 7919) % 1000;
			float fx = px + 10 + (h / 1000.0f) * (PW - 20);
			float fy = py + PH - phase * (PH + 24) + ((h * 31) % 13) - 6;
			int a = (int) (230 * Math.sin(phase * Math.PI));
			int col = argb(a, (i % 3 == 0) ? 0xFFF3B0 : 0xE8C860);
			int x = (int) fx;
			int y = (int) fy;
			g.fill(x, y, x + 2, y + 2, col);
			g.fill(x - 1, y, x, y + 2, argb(a / 2, 0xFFE070));
			g.fill(x + 2, y, x + 3, y + 2, argb(a / 2, 0xFFE070));
			g.fill(x, y - 1, x + 2, y, argb(a / 2, 0xFFE070));
		}
	}

	// ---- left page -------------------------------------------------------------------------------
	private void drawLeft(GuiGraphicsExtractor g) {
		int lx = px + 20;
		int ly = py + 17;
		int lw = 150;
		Component title = Component.translatable("codex.tab." + TAB_KEYS[tab]);
		g.text(font, title, lx, ly, INK, false);
		rule(g, lx, ly + 10, lw, 0x88A0782C);
		if (openTicks < 36) {                                   // gold shimmer sweeping across the page title
			int sx = lx + (int) ((openTicks % 36) / 36.0f * (lw + 30)) - 15;
			g.fill(Math.max(lx, sx), ly - 1, Math.min(lx + lw, sx + 12), ly + 10, 0x30FFF0A0);
		}
		switch (tab) {
			case 3 -> {
				int vt = ly + 14;
				int vb = py + 205;
				g.enableScissor(lx - 3, vt, lx + lw + 4, vb);
				int endY = drawProgressLeft(g, lx, ly + 16 - scroll[3], lw - 6);
				g.disableScissor();
				leftHeight = endY + scroll[3] - (ly + 16);
				int view = vb - vt - 4;
				scroll[3] = Mth.clamp(scroll[3], 0, Math.max(0, leftHeight - view));
				scrollbar(g, BAR_PROGRESS, lx + lw - 1, vt, vb, leftHeight, view, () -> scroll[3], v -> scroll[3] = v);
			}
			case 4 -> drawTipsPage(g, lx, ly + 16, lw, 0);
			case TAB_CHEATS -> drawCheatList(g, lx, ly + 16, lw);
			default -> drawList(g, lx, ly + (tab == 1 ? 14 : 0) + 16, lw);
		}
	}

	private void drawList(GuiGraphicsExtractor g, int lx, int top, int lw) {
		int bottom = py + 204;
		if (tab == 1) {
			// tier filter chips
			String[] chips = {"All", "I", "II", "III", "IV", "V", "VI"};
			for (int i = 0; i < chips.length; i++) {
				int x = lx + i * 22;
				int y = top - 14;
				boolean sel = tierFilter == i;
				boolean hover = inRect(mouseX, mouseY, x, y, 20, 11);
				g.fill(x, y, x + 20, y + 11, sel ? 0xFF8A5A10 : hover ? 0x55A0782C : 0x33A0782C);
				g.text(font, chips[i], x + 10 - font.width(chips[i]) / 2, y + 2, sel ? 0xFFFFF3C8 : BODY, false);
			}
		}
		if (rows.isEmpty()) {
			paragraph(g, font, Component.translatable("codex.empty"), lx, top + 4, lw, MUTED);
			return;
		}
		int visible = Math.max(1, (bottom - top) / ROW_H);
		scroll[tab] = Mth.clamp(scroll[tab], 0, Math.max(0, rows.size() - visible));
		g.enableScissor(lx - 2, top - 1, lx + lw + 1, bottom);
		for (int i = scroll[tab]; i < Math.min(rows.size(), scroll[tab] + visible); i++) {
			Row r = rows.get(i);
			int y = top + (i - scroll[tab]) * ROW_H;
			boolean sel = i == selected[tab];
			boolean hover = inRect(mouseX, mouseY, lx - 2, y, lw - 2, ROW_H);
			if (sel) {
				g.fill(lx - 2, y, lx + lw - 4, y + ROW_H - 1, 0x66C8A040);
				g.fill(lx - 2, y, lx - 1, y + ROW_H - 1, 0xFF8A5A10);
			} else if (hover) {
				g.fill(lx - 2, y, lx + lw - 4, y + ROW_H - 1, 0x22000000);
			}
			boolean known = r.known();
			boolean ghost = r.ghost();
			if (r.kind == Kind.BOSS) {
				if (known) {
					g.blit(RenderPipelines.GUI_TEXTURED, r.emblem, lx, y, 0f, 0f, 16, 16, 32, 32, 32, 32);
				} else {
					icon(g, ICON_QUESTION, lx, y, 16, 0xFFFFFFFF);
				}
			} else if (known) {
				itemIcon(g, r.icon(), lx, y, ghost ? (sel ? VEIL_SELECTED : hover ? VEIL_HOVER : VEIL_PLAIN) : 0);
			} else {
				icon(g, ICON_QUESTION, lx, y, 16, 0xFFFFFFFF);
			}
			Component name = known ? (r.kind == Kind.BOSS ? Component.literal(r.shortName()) : r.name()) : Component.translatable("codex.unknown");
			if (ghost) {
				name = Component.empty().append(name).withStyle(ChatFormatting.ITALIC);
			}
			int col = !known || ghost ? MUTED : r.locked() ? 0xFF7A3030 : r.kind == Kind.BOSS && r.defeated() ? GREEN : INK;
			g.text(font, name, lx + 21, y + 4, col, false);
			if (known && r.locked()) {
				icon(g, ICON_LOCK, lx + lw - 20, y + 1, 14, 0xFFFFFFFF);
			} else if (r.kind == Kind.BOSS && known && r.defeated()) {
				icon(g, ICON_CHECK, lx + lw - 20, y + 1, 14, 0xFFFFFFFF);
			}
		}
		g.disableScissor();
		int t = tab;
		scrollbar(g, BAR_LIST, lx + lw - 1, top, bottom, rows.size(), visible, () -> scroll[t], v -> scroll[t] = v);
	}

	/** An item icon; with a veil colour it fades into the page (a listed entry the player does not own yet). */
	private void itemIcon(GuiGraphicsExtractor g, ItemStack st, int x, int y, int veil) {
		g.item(st, x, y);
		if (veil != 0) {
			g.fill(x, y, x + 16, y + 16, veil);
		}
	}

	/** Draws a vertical scrollbar (when there is something to scroll) and registers it so it can be clicked and dragged. */
	private void scrollbar(GuiGraphicsExtractor g, int id, int x, int top, int bottom, int content, int view, IntSupplier value, IntConsumer setter) {
		if (content <= view) {
			return;
		}
		Bar bar = new Bar(id, x, top, bottom, content, view, value, setter);
		bars.add(bar);
		boolean hot = dragging == id || (dragging < 0 && bar.grabs(mouseX, mouseY));
		int thumb = bar.thumbTop();
		g.fill(x, top, x + 3, bottom, hot ? 0x38000000 : 0x22000000);
		g.fill(x, thumb, x + 3, thumb + bar.thumbHeight(), hot ? 0xFFB07A18 : 0xAA8A5A10);
		if (hot) {
			g.fill(x, thumb, x + 3, thumb + 1, 0x66FFFFFF);
		}
	}

	// ---- right page ------------------------------------------------------------------------------
	private void drawRight(GuiGraphicsExtractor g) {
		int rx = px + 192;
		int rw = 148;
		int top = py + 17;
		int bottom = py + 205;
		g.enableScissor(rx - 3, top - 2, rx + rw + 4, bottom);
		int y = top - detailScroll;
		int end = switch (tab) {
			case 0 -> drawBossDetail(g, rx, y, rw);
			case 1 -> drawItemDetail(g, rx, y, rw);
			case 2 -> drawMobDetail(g, rx, y, rw);
			case 3 -> drawProgressRight(g, rx, y, rw);
			case TAB_CHEATS -> drawCheatDetail(g, rx, y, rw);
			default -> drawTipsPage(g, rx, y, rw, 1);
		};
		detailHeight = end + detailScroll - top;
		g.disableScissor();
		if (tab == 4) {
			scrollbar(g, BAR_TIPS, rx + rw + 2, top, bottom, tipLines(TIP_WRAP).size(), 2 * TIP_LINES, () -> scroll[4], v -> scroll[4] = v);
		} else {
			scrollbar(g, BAR_DETAIL, rx + rw + 2, top, bottom, detailHeight, bottom - top, () -> detailScroll, v -> detailScroll = v);
		}
	}

	private int unknownDetail(GuiGraphicsExtractor g, int rx, int y, int rw) {
		icon(g, ICON_QUESTION, rx + rw / 2 - 16, y + 30, 32, 0xFFFFFFFF);
		g.text(font, Component.translatable("codex.unknown"), rx + rw / 2 - font.width("???") / 2, y + 70, MUTED, false);
		return paragraph(g, font, Component.translatable("codex.unknown.detail"), rx, y + 88, rw, MUTED);
	}

	private int drawItemDetail(GuiGraphicsExtractor g, int rx, int y, int rw) {
		Row r = selectedRow();
		if (r == null) {
			return paragraph(g, font, Component.translatable("codex.empty"), rx, y, rw, MUTED);
		}
		if (!r.known()) {
			return unknownDetail(g, rx, y, rw);
		}
		String did = r.item.getDescriptionId();
		Language lang = Language.getInstance();
		boolean ghost = r.ghost();
		itemIcon(g, r.icon(), rx, y, ghost ? VEIL_PLAIN : 0);
		g.text(font, r.name(), rx + 22, y + 1, ghost ? MUTED : INK, false);
		g.text(font, Component.translatable("codex.tier", r.tier), rx + 22, y + 11, argb(255, tierRgb(r.tier)), false);
		y += 22;
		if (ghost) {
			y = paragraph(g, font, Component.translatable("codex.uncollected").withStyle(ChatFormatting.ITALIC), rx, y, rw, MUTED) + 3;
		}
		if (r.locked()) {
			y = paragraph(g, font, Component.translatable("codex.locked_item", r.gate.name()), rx, y, rw, RED) + 2;
		}
		if (lang.has(did + ".lore")) {
			y = paragraph(g, font, Component.translatable(did + ".lore"), rx, y, rw, MUTED) + 3;
		}
		if (lang.has(did + ".obtain")) {
			y = paragraph(g, font, Component.translatable("codex.obtain").append(Component.translatable(did + ".obtain")), rx, y, rw, BODY) + 3;
		}
		if (r.def != null) {
			var stats = CodexModel.stats(r.def);
			if (!stats.isEmpty()) {
				y = heading(g, font, Component.literal("Stats"), rx, y, rw);
				for (var s : stats) {
					g.text(font, s.label(), rx, y, BODY, false);
					g.text(font, s.value(), rx + rw - font.width(s.value()), y, INK, false);
					y += 10;
				}
				y += 3;
			}
			var abil = CodexModel.abilities(did);
			if (!abil.isEmpty()) {
				y = heading(g, font, Component.literal("Abilities"), rx, y, rw);
				for (var a : abil) {
					g.text(font, a.title(), rx, y, abilityColor(a.kind()), false);
					g.text(font, kindName(a.kind()), rx + rw - font.width(kindName(a.kind())), y, MUTED, false);
					y += 10;
					for (String line : a.body()) {
						y = paragraph(g, font, line, rx + 3, y, rw - 3, BODY);
					}
					if (!a.cooldown().isEmpty()) {
						g.text(font, "Cooldown " + a.cooldown() + "s", rx + 3, y, MUTED, false);
						y += 10;
					}
					y += 2;
				}
			}
			ArmorSets.SetDef set = ArmorSets.of(r.item);
			if (set != null) {
				y = heading(g, font, Component.literal(lang.getOrDefault(set.langKey())), rx, y, rw);
				for (int n = 0; lang.has(set.langKey() + ".bonus." + n); n++) {
					String[] p = lang.getOrDefault(set.langKey() + ".bonus." + n).split("\\|", -1);
					g.text(font, "(" + set.bonuses().get(n).need() + ") " + p[0], rx, y, GREEN, false);
					y += 10;
					y = paragraph(g, font, p[1].replace("\n", " "), rx + 3, y, rw - 3, BODY);                 // one flowing paragraph, not hand-wrapped lines
				}
				int ix = rx;
				for (String piece : set.pieces()) {
					g.item(new ItemStack(CodexModel.itemOf(piece)), ix, y + 2);
					if (inRect(mouseX, mouseY, ix, y + 2, 16, 16)) {
						g.setTooltipForNextFrame(font, new ItemStack(CodexModel.itemOf(piece)), mouseX, mouseY);
					}
					ix += 20;
				}
				y += 22;
			}
		}
		var recipes = r.def != null ? CodexModel.recipes(r.def) : CodexModel.blockRecipes(r.id);
		if (!recipes.isEmpty()) {
			y = heading(g, font, Component.translatable("codex.recipe"), rx, y, rw);
			for (var rec : recipes) {
				y = drawRecipe(g, rec, rx, y, r) + 4;
			}
		}
		return y;
	}

	private int drawRecipe(GuiGraphicsExtractor g, CodexModel.Recipe rec, int rx, int y, Row row) {
		long time = minecraft.level != null ? minecraft.level.getGameTime() : System.currentTimeMillis() / 50;
		boolean smelt = rec.type().equals("smelting");
		if (smelt) {
			String[] alts = rec.cells()[4].split("\\|");
			slot(g, rx + 4, y + 18);
			ItemStack in = stackOf(alts[(int) ((time / 30) % alts.length)], time);
			g.item(in, rx + 5, y + 19);
			hoverTip(g, in, rx + 5, y + 19);
			g.text(font, "Furnace", rx + 24, y + 6, MUTED, false);
			g.text(font, "->", rx + 30, y + 23, GOLD, false);
			slot(g, rx + 52, y + 18);
			ItemStack out = new ItemStack(row.item, rec.count());
			g.item(out, rx + 53, y + 19);
			g.itemDecorations(font, out, rx + 53, y + 19);
			hoverTip(g, out, rx + 53, y + 19);
			g.text(font, Component.translatable("codex.recipe.smelt"), rx + 76, y + 23, MUTED, false);
			return y + 40;
		}
		for (int i = 0; i < 9; i++) {
			int gx = rx + (i % 3) * 18;
			int gy = y + (i / 3) * 18;
			slot(g, gx, gy);
			String cell = rec.cells()[i];
			if (!cell.isEmpty()) {
				ItemStack st = stackOf(cell.split("\\|")[0], time);
				g.item(st, gx + 1, gy + 1);
				hoverTip(g, st, gx + 1, gy + 1);
			}
		}
		g.text(font, "->", rx + 58, y + 23, GOLD, false);
		slot(g, rx + 74, y + 18);
		ItemStack out = new ItemStack(row.item, rec.count());
		g.item(out, rx + 75, y + 19);
		g.itemDecorations(font, out, rx + 75, y + 19);
		hoverTip(g, out, rx + 75, y + 19);
		if (rec.type().equals("shapeless")) {
			g.text(font, Component.translatable("codex.recipe.shapeless"), rx + 96, y + 23, MUTED, false);
		}
		return y + 54;
	}

	private void hoverTip(GuiGraphicsExtractor g, ItemStack st, int x, int y) {
		if (!st.isEmpty() && inRect(mouseX, mouseY, x, y, 16, 16)) {
			g.setTooltipForNextFrame(font, st, mouseX, mouseY);
		}
	}

	private static int abilityColor(String kind) {
		return switch (kind) {
			case "active" -> GOLD;
			case "hit" -> BLUE;
			case "kill" -> RED;
			case "wear" -> GREEN;
			case "use" -> PURPLE;
			default -> 0xFF2E6B4A;
		};
	}

	private static String kindName(String kind) {
		return switch (kind) {
			case "active" -> "Right-click";
			case "hit" -> "On hit";
			case "kill" -> "On kill";
			case "wear" -> "Worn";
			case "use" -> "On use";
			default -> "Passive";
		};
	}

	// ---- bosses ----------------------------------------------------------------------------------
	private int drawBossDetail(GuiGraphicsExtractor g, int rx, int y, int rw) {
		Row r = selectedRow();
		if (r == null) {
			return y;
		}
		if (!r.known()) {
			return unknownDetail(g, rx, y, rw);
		}
		Language lang = Language.getInstance();
		String id = r.id;
		g.blit(RenderPipelines.GUI_TEXTURED, r.emblem, rx, y, 0f, 0f, 40, 40, 32, 32, 32, 32);
		int tx = rx + 46;
		int ty = y + 2;
		for (FormattedCharSequence line : font.split(r.name(), rw - 46)) {
			g.text(font, line, tx, ty, INK, false);
			ty += 9;
		}
		ty = paragraph(g, font, Component.translatable("codex.boss." + id + ".rank"), tx, ty + 1, rw - 46, argb(255, tierRgb(r.tier)));
		y = Math.max(y + 44, ty + 3);
		if (r.defeated()) {
			icon(g, ICON_CHECK, rx + rw - 16, y - 40, 14, 0xFFFFFFFF);
		}
		if (MobPreview.has(id)) {
			y = previewBox(g, id, rx, y, rw, 108);
		}
		y = heading(g, font, Component.translatable("codex.lore"), rx, y, rw);
		y = paragraph(g, font, Component.translatable("codex.boss." + id + ".lore"), rx, y, rw, BODY) + 3;
		y = heading(g, font, Component.translatable("codex.summon"), rx, y, rw);
		y = paragraph(g, font, Component.translatable("codex.boss." + id + ".summon"), rx, y, rw, BODY) + 3;
		JsonObject bs = CodexModel.bossStats(id);
		if (bs != null) {
			double hp = bs.get("hp").getAsDouble() * UsmConfig.get().bossHealthMultiplier;
			g.text(font, "Health", rx, y, BODY, false);
			String v = CodexModel.fmt(hp);
			g.text(font, v, rx + rw - font.width(v), y, INK, false);
			y += 10;
			g.text(font, "Armor", rx, y, BODY, false);
			String a = CodexModel.fmt(bs.get("armor").getAsDouble());
			g.text(font, a, rx + rw - font.width(a), y, INK, false);
			y += 13;
		}
		int n = CodexModel.unlockCount(r.gate);
		if (n > 0) {
			y = heading(g, font, Component.literal("Unlocks " + n + " item" + (n == 1 ? "" : "s")), rx, y, rw);
			int ix = rx;
			for (var it : CodexModel.unlockPreview(r.gate, 7)) {
				ItemStack st = new ItemStack(it);
				slot(g, ix, y);
				g.item(st, ix + 1, y + 1);
				hoverTip(g, st, ix + 1, y + 1);
				ix += 20;
			}
			y += 22;
		}
		MobDef md = Mobs.DEFS.get(id);
		if (md != null && !md.bag.isEmpty()) {
			y = heading(g, font, Component.translatable("codex.rewards"), rx, y, rw);
			if (r.defeated()) {
				int ix = rx;
				for (MobDef.Drop d : md.bag) {
					ItemStack st = Mobs.stack(d.item(), d.max());
					if (ix + 18 > rx + rw) {
						ix = rx;
						y += 20;
					}
					slot(g, ix, y);
					g.item(st, ix + 1, y + 1);
					g.itemDecorations(font, st, ix + 1, y + 1);
					hoverTip(g, st, ix + 1, y + 1);
					ix += 20;
				}
				y += 24;
			} else {
				y = paragraph(g, font, Component.translatable("codex.rewards.locked"), rx, y, rw, MUTED) + 3;
			}
		}
		y = heading(g, font, Component.translatable("codex.tips"), rx, y, rw);
		if (r.defeated()) {
			for (int i = 0; lang.has("codex.boss." + id + ".tip." + i); i++) {
				g.text(font, "-", rx, y, GOLD, false);
				y = paragraph(g, font, Component.translatable("codex.boss." + id + ".tip." + i), rx + 7, y, rw - 7, BODY) + 2;
			}
		} else {
			y = paragraph(g, font, Component.translatable("codex.tips.locked"), rx, y, rw, MUTED);
		}
		return y;
	}

	private int drawMobDetail(GuiGraphicsExtractor g, int rx, int y, int rw) {
		Row r = selectedRow();
		if (r == null) {
			return paragraph(g, font, Component.translatable("codex.empty"), rx, y, rw, MUTED);
		}
		if (!r.known()) {
			return unknownDetail(g, rx, y, rw);
		}
		Language lang = Language.getInstance();
		g.text(font, r.name(), rx, y, INK, false);
		y += 11;
		g.text(font, Component.translatable("codex.tier", r.tier), rx, y, argb(255, tierRgb(r.tier)), false);
		y += 12;
		if (MobPreview.has(r.id)) {
			y = previewBox(g, r.id, rx, y, rw, 96);
		}
		String key = "entity." + UltimateSurvival.MOD_ID + "." + r.id + ".lore";
		if (lang.has(key)) {
			y = paragraph(g, font, Component.translatable(key), rx, y, rw, MUTED) + 3;
		}
		MobDef md = Mobs.DEFS.get(r.id);
		if (md == null) {
			return y;
		}
		y = heading(g, font, Component.translatable("codex.stats"), rx, y, rw);
		y = statLine(g, rx, y, rw, "Health", CodexModel.fmt(md.hp));
		y = statLine(g, rx, y, rw, "Damage", CodexModel.fmt(md.dmg));
		if (md.armor > 0) {
			y = statLine(g, rx, y, rw, "Armor", CodexModel.fmt(md.armor));
		}
		y = statLine(g, rx, y, rw, "Speed", CodexModel.fmt(md.speed * 43.17) + " m/s");
		String traits = (md.fly ? "Flies" : "Walks") + (md.fire ? ", fireproof" : "");
		y = statLine(g, rx, y, rw, "Traits", traits);
		if (md.minionOf != null) {
			y = paragraph(g, font, Component.translatable("codex.mob.minion_of", Component.translatable("entity." + UltimateSurvival.MOD_ID + "." + md.minionOf)), rx, y + 2, rw, BODY);
		}
		y += 3;
		var seen = new java.util.LinkedHashSet<String>();
		for (MobDef.Phase ph : md.phases) {
			for (MobDef.AttackSpec s : ph.attacks()) {
				seen.add(s.type());
			}
		}
		if (!seen.isEmpty()) {
			y = heading(g, font, Component.translatable("codex.attacks"), rx, y, rw);
			for (String t : seen) {
				if (lang.has("codex.attack." + t + ".name")) {
					g.text(font, Component.translatable("codex.attack." + t + ".name"), rx, y, GOLD, false);
					y += 10;
					y = paragraph(g, font, Component.translatable("codex.attack." + t + ".text"), rx + 3, y, rw - 3, BODY) + 2;
				}
			}
		}
		if (!md.drops.isEmpty()) {
			y = heading(g, font, Component.translatable("codex.drops"), rx, y, rw);
			int ix = rx;
			for (MobDef.Drop d : md.drops) {
				ItemStack st = Mobs.stack(d.item(), d.max());
				slot(g, ix, y);
				g.item(st, ix + 1, y + 1);
				hoverTip(g, st, ix + 1, y + 1);
				ix += 20;
			}
			y += 22;
		}
		return y;
	}

	private int statLine(GuiGraphicsExtractor g, int rx, int y, int rw, String label, String value) {
		g.text(font, label, rx, y, BODY, false);
		g.text(font, value, rx + rw - font.width(value), y, INK, false);
		return y + 10;
	}

	/** Dark inset showing the live model of a mob; skipped during the book-opening animation (the model ignores GUI scaling). */
	private int previewBox(GuiGraphicsExtractor g, String id, int rx, int y, int rw, int h) {
		g.fill(rx - 1, y - 1, rx + rw + 1, y + h + 1, 0xFF3A2C20);
		g.fill(rx, y, rx + rw, y + h, 0xFF16110D);
		g.fill(rx, y + h - 14, rx + rw, y + h, 0xFF1F1812);
		if (openTicks >= 10) {
			MobPreview.draw(g, id, rx, y, rx + rw, y + h, mouseX, mouseY);
		}
		return y + h + 5;
	}

	// ---- progression -----------------------------------------------------------------------------
	private int drawProgressLeft(GuiGraphicsExtractor g, int lx, int y, int lw) {
		int tier = Gates.currentTier();
		g.pose().pushMatrix();
		g.pose().translate(lx, y);
		g.pose().scale(2f, 2f);
		g.text(font, Component.translatable("codex.progress.tier", tier), 0, 0, argb(255, tierRgb(tier)), false);
		g.pose().popMatrix();
		y += 22;
		Gate next = Gates.nextGate();
		y = paragraph(g, font, next == null ? Component.translatable("codex.progress.done") : Component.translatable("codex.progress.next", next.name()),
				lx, y, lw, next == null ? GREEN : BODY) + 4;
		int bosses = 0;
		int done = 0;
		for (Gate gt : Content.GATES) {
			if (gt.boss() != null) {
				bosses++;
				done += Gates.isCleared(gt) ? 1 : 0;
			}
		}
		g.text(font, Component.translatable("codex.progress.gates"), lx, y, GOLD, false);
		y += 11;
		bar(g, lx, y, lw, 6, done / (float) Math.max(1, bosses), 0xFFC8A030);
		String bs = done + " / " + bosses;
		g.text(font, bs, lx + lw - font.width(bs), y - 11, INK, false);
		y += 12;
		int found = CodexModel.itemsFound();
		int total = CodexModel.totalEntries();
		g.text(font, Component.translatable("codex.progress.found", found, total), lx, y, BODY, false);
		y += 11;
		bar(g, lx, y, lw, 6, found / (float) Math.max(1, total), 0xFF3A6EB8);
		y += 14;
		rule(g, lx, y - 3, lw, 0x55A0782C);
		return paragraph(g, font, Component.translatable("codex.prologue"), lx, y, lw, MUTED);
	}

	private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int color) {
		g.fill(x, y, x + w, y + h, 0xFF3A2C20);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1C1612);
		g.fill(x + 1, y + 1, x + 1 + (int) ((w - 2) * Mth.clamp(frac, 0f, 1f)), y + h - 1, color);
	}

	private int drawProgressRight(GuiGraphicsExtractor g, int rx, int y, int rw) {
		y = heading(g, font, Component.translatable("codex.progress.gates"), rx, y, rw);
		for (Gate gt : Content.GATES) {
			boolean cleared = Gates.isCleared(gt);
			boolean known = gt.isStart() || cleared || CodexModel.bossRevealed(gt) || UsmState.progress().isDiscovered("boss:" + gt.id());
			if (gt.isStart()) {
				g.fill(rx, y, rx + 16, y + 16, 0xFF3F8F3F);
				g.text(font, "T1", rx + 3, y + 4, 0xFFFFFFFF, false);
			} else if (known) {
				g.blit(RenderPipelines.GUI_TEXTURED, UltimateSurvival.id("textures/gui/emblem/" + gt.id() + ".png"), rx, y, 0f, 0f, 16, 16, 32, 32, 32, 32);
			} else {
				icon(g, ICON_QUESTION, rx, y, 16, 0xFFFFFFFF);
			}
			Component name = known ? Component.literal(gt.name()) : Component.translatable("codex.unknown");
			int lines = 0;
			int ty = y + 1;
			for (FormattedCharSequence l : font.split(name, rw - 40)) {
				g.text(font, l, rx + 20, ty, cleared ? GREEN : known ? INK : MUTED, false);
				ty += 8;
				lines++;
			}
			g.text(font, Component.translatable("codex.tier", gt.tier()), rx + 20, y + (lines > 1 ? 17 : 9), argb(255, tierRgb(gt.tier())), false);
			icon(g, cleared ? ICON_CHECK : ICON_LOCK, rx + rw - 16, y + 1, 14, cleared ? 0xFFFFFFFF : 0xBBFFFFFF);
			y += lines > 1 ? 26 : 20;
		}
		return y;
	}

	// ---- creative cheats ----------------------------------------------------------------------------
	private static final int[] GROUP_COL = {GREEN, RED, BLUE};

	private void sendCheat(String id, int value) {
		ClientPlayNetworking.send(new Payloads.CheatAction(id, value));
	}

	private void drawCheatList(GuiGraphicsExtractor g, int lx, int top, int lw) {
		int bottom = py + 204;
		int n = Cheats.ALL.size();
		int visible = Math.max(1, (bottom - top) / ROW_H);
		scroll[TAB_CHEATS] = Mth.clamp(scroll[TAB_CHEATS], 0, Math.max(0, n - visible));
		g.enableScissor(lx - 2, top - 1, lx + lw + 1, bottom);
		for (int i = scroll[TAB_CHEATS]; i < Math.min(n, scroll[TAB_CHEATS] + visible); i++) {
			Cheats.Cheat c = Cheats.ALL.get(i);
			int y = top + (i - scroll[TAB_CHEATS]) * ROW_H;
			boolean sel = i == selected[TAB_CHEATS];
			boolean hover = inRect(mouseX, mouseY, lx - 2, y, lw - 2, ROW_H);
			if (sel) {
				g.fill(lx - 2, y, lx + lw - 4, y + ROW_H - 1, 0x66C8A040);
				g.fill(lx - 2, y, lx - 1, y + ROW_H - 1, 0xFF8A5A10);
			} else if (hover) {
				g.fill(lx - 2, y, lx + lw - 4, y + ROW_H - 1, 0x22000000);
			}
			g.fill(lx + 1, y + 3, lx + 4, y + ROW_H - 4, GROUP_COL[c.group()]);
			g.text(font, Component.translatable("codex.cheat." + c.id()), lx + 8, y + 4, INK, false);
			String v = Cheats.valueText(c);
			if (!v.isEmpty()) {
				boolean on = c.kind() == Cheats.Kind.TOGGLE && c.get().getAsDouble() > 0.5;
				g.text(font, v, lx + lw - 10 - font.width(v), y + 4, on ? GREEN : c.kind() == Cheats.Kind.TOGGLE ? MUTED : GOLD, false);
			} else {
				icon(g, ICON_TIP, lx + lw - 24, y + 1, 14, 0x99FFFFFF);                  // actions carry a small mark instead of a value
			}
		}
		g.disableScissor();
		scrollbar(g, BAR_LIST, lx + lw - 1, top, bottom, n, visible, () -> scroll[TAB_CHEATS], v -> scroll[TAB_CHEATS] = v);
	}

	/** A flat button: drawn here, handled in mouseClicked through {@link #btns}. */
	private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component text, int color, Runnable run) {
		boolean hover = inRect(mouseX, mouseY, x, y, w, h) && mouseY >= py + 17 && mouseY < py + 205;
		g.fill(x, y, x + w, y + h, 0xFF2A1A0A);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? darker(color, 1.0f) : darker(color, 0.8f));
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x44FFFFFF);
		g.text(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF, false);
		btns.add(new Btn(x, y, w, h, run));
	}

	private int drawCheatDetail(GuiGraphicsExtractor g, int rx, int y, int rw) {
		Cheats.Cheat c = Cheats.ALL.get(Mth.clamp(selected[TAB_CHEATS], 0, Cheats.ALL.size() - 1));
		g.text(font, Component.translatable("codex.cheat.group." + c.group()), rx, y, GROUP_COL[c.group()], false);
		y += 11;
		for (FormattedCharSequence line : font.split(Component.translatable("codex.cheat." + c.id()), rw)) {
			g.text(font, line, rx, y, INK, false);
			y += 10;
		}
		rule(g, rx, y, rw, 0x66A0782C);
		y += 5;
		y = paragraph(g, font, Component.translatable("codex.cheat." + c.id() + ".desc"), rx, y, rw, BODY) + 8;
		String id = c.id();
		switch (c.kind()) {
			case TOGGLE -> {
				boolean on = c.get().getAsDouble() > 0.5;
				button(g, rx, y, rw, 16, Component.translatable(on ? "codex.cheat.on" : "codex.cheat.off"), on ? 0xFF3F8F3F : 0xFF7A3030, () -> sendCheat(id, on ? 0 : 1));
				y += 22;
			}
			case NUMBER -> {
				int idx = c.valueIndex();
				int n = c.values().length;
				int big = n > 12 ? 5 : n;
				button(g, rx, y, 20, 16, Component.literal("<<"), 0xFF6A5030, () -> sendCheat(id, Math.max(0, idx - big)));
				button(g, rx + 22, y, 16, 16, Component.literal("<"), 0xFF6A5030, () -> sendCheat(id, Math.max(0, idx - 1)));
				button(g, rx + rw - 38, y, 16, 16, Component.literal(">"), 0xFF6A5030, () -> sendCheat(id, Math.min(n - 1, idx + 1)));
				button(g, rx + rw - 20, y, 20, 16, Component.literal(">>"), 0xFF6A5030, () -> sendCheat(id, Math.min(n - 1, idx + big)));
				String v = Cheats.valueText(c);
				g.fill(rx + 40, y, rx + rw - 40, y + 16, 0xFF16110D);
				g.text(font, v, rx + rw / 2 - font.width(v) / 2, y + 4, 0xFFFFE9A8, false);
				y += 22;
			}
			case ACTION -> {
				int ci = 0;
				if (!c.choices().isEmpty()) {
					int n = c.choices().size();
					ci = Mth.clamp(choice.getOrDefault(id, 0), 0, n - 1);
					int cur = ci;
					button(g, rx, y, 16, 16, Component.literal("<"), 0xFF6A5030, () -> choice.put(id, (cur + n - 1) % n));
					button(g, rx + rw - 16, y, 16, 16, Component.literal(">"), 0xFF6A5030, () -> choice.put(id, (cur + 1) % n));
					String label = Cheats.label(id, ci);
					g.fill(rx + 18, y, rx + rw - 18, y + 16, 0xFF16110D);
					g.text(font, label, rx + rw / 2 - font.width(label) / 2, y + 4, 0xFFFFE9A8, false);
					y += 22;
				}
				int pick = ci;
				button(g, rx, y, rw, 16, Component.translatable("codex.cheat.apply"), 0xFFB07A18, () -> sendCheat(id, pick));
				y += 22;
			}
		}
		return y;
	}

	// ---- tips ---------------------------------------------------------------------------------------
	private static final int TIP_LINES = 19;

	private record TipLine(FormattedCharSequence text, int color, boolean bullet) {
	}

	private List<TipLine> tipLines(int width) {
		List<TipLine> out = new ArrayList<>();
		int tier = Gates.currentTier();
		for (int n = 0; n < CodexModel.tipCount(); n++) {
			boolean open = tier >= CodexModel.tipTier(n);
			Component text = open ? Component.translatable("codex.tip." + n)
					: Component.literal("??? (reach tier " + CodexModel.tipTier(n) + ")");
			boolean first = true;
			for (FormattedCharSequence l : font.split(text, width - 8)) {
				out.add(new TipLine(l, open ? BODY : MUTED, first));
				first = false;
			}
			out.add(new TipLine(FormattedCharSequence.EMPTY, 0, false));
		}
		return out;
	}

	/** Tips tab: all tips flow through one shared line list; page 0 shows the first block, page 1 the next. */
	private int drawTipsPage(GuiGraphicsExtractor g, int x, int y, int w, int page) {
		List<TipLine> lines = tipLines(TIP_WRAP);
		int perPage = TIP_LINES;
		int start = scroll[4] + page * perPage;
		int yy = y;
		for (int i = start; i < Math.min(lines.size(), start + perPage); i++) {
			TipLine t = lines.get(i);
			if (t.bullet()) {
				g.text(font, "*", x, yy, GOLD, false);
			}
			g.text(font, t.text(), x + 8, yy, t.color(), false);
			yy += 9;
		}
		return page == 1 ? top(y) + 1 : yy;
	}

	private static int top(int y) {
		return y;
	}

	// ====================================================================================================
	// input
	// ====================================================================================================
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (openTicks < 6) {
			return true;
		}
		int mx = (int) event.x();
		int my = (int) event.y();
		if (event.button() == 0) {
			for (Bar bar : bars) {
				if (bar.grabs(mx, my)) {
					int thumb = bar.thumbTop();
					if (my >= thumb && my < thumb + bar.thumbHeight()) {
						grab = my - thumb;                                  // pick the thumb up where it was touched
					} else {
						grab = bar.thumbHeight() / 2;                       // the bare track: the thumb jumps there and follows
						drag(bar, my);
					}
					dragging = bar.id();
					return true;
				}
			}
		}
		if (event.button() == 0 && tab == TAB_CHEATS) {
			for (Btn b : btns) {
				if (inRect(mx, my, b.x(), b.y(), b.w(), b.h()) && my >= py + 17 && my < py + 205) {
					b.run().run();
					click(1.3f);
					return true;
				}
			}
		}
		for (int i = 0; i < tabCount(); i++) {
			int x = px - 20 - (i == tab ? 3 : 0);
			int y = py + 14 + i * 31;
			if (inRect(mx, my, x, y, 26, 26)) {
				setTab(i);
				return true;
			}
		}
		int lx = px + 20;
		int ly = py + 17;
		if (tab == 1) {
			for (int i = 0; i < 7; i++) {
				if (inRect(mx, my, lx + i * 22, ly + 16, 20, 11)) {
					tierFilter = i;
					scroll[1] = 0;
					selected[1] = -1;
					rebuildRows();
					click(1.2f);
					return true;
				}
			}
		}
		if ((tab <= 2 || tab == TAB_CHEATS) && listSize() > 0) {
			int top = ly + (tab == 1 ? 14 : 0) + 16;
			if (mx >= lx - 2 && mx < lx + 148 && my >= top && my < py + 204) {
				int idx = scroll[tab] + (my - top) / ROW_H;
				if (idx >= 0 && idx < listSize()) {
					selected[tab] = idx;
					detailScroll = 0;
					click(1.0f);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Moves the scroll of {@code bar} so its thumb follows the mouse at height {@code my}. */
	private void drag(Bar bar, int my) {
		double along = (my - grab - bar.top()) / (double) bar.travel();
		bar.setter().accept((int) Math.round(Mth.clamp(along, 0.0, 1.0) * bar.max()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging >= 0 && event.button() == 0) {
			for (Bar bar : bars) {
				if (bar.id() == dragging) {
					drag(bar, (int) event.y());
					return true;
				}
			}
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging >= 0 && event.button() == 0) {
			dragging = -1;
			return true;
		}
		return super.mouseReleased(event);
	}

	private void click(float pitch) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int step = scrollY > 0 ? -1 : 1;
		if (tab == 4) {
			scroll[4] = Math.max(0, scroll[4] + step * 3);
			int maxLines = Math.max(0, tipLines(TIP_WRAP).size() - 2 * TIP_LINES);
			scroll[4] = Math.min(scroll[4], maxLines);
			return true;
		}
		if (x < px + 182 && tab == 3) {
			scroll[3] = Mth.clamp(scroll[3] + step * 12, 0, Math.max(0, leftHeight - 170));
		} else if (x < px + 182) {
			scroll[tab] = Math.max(0, scroll[tab] + step);
		} else {
			int view = 188;
			detailScroll = Mth.clamp(detailScroll + step * 14, 0, Math.max(0, detailHeight - view));
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int k = event.key();
		if (com.ultimatesurvival.client.UsmKeys.codex != null && com.ultimatesurvival.client.UsmKeys.codex.matches(event)) {
			onClose();
			return true;
		}
		if (k == 69) {                            // E closes, like a vanilla book
			onClose();
			return true;
		}
		if (k == 263) {                // left
			setTab((tab + tabCount() - 1) % tabCount());
			return true;
		}
		if (k == 262) {                // right
			setTab((tab + 1) % tabCount());
			return true;
		}
		if ((k == 265 || k == 264) && (tab <= 2 || tab == TAB_CHEATS) && listSize() > 0) {
			int s = Mth.clamp(selected[tab] + (k == 264 ? 1 : -1), 0, listSize() - 1);
			selected[tab] = s;
			detailScroll = 0;
			int visible = Math.max(1, (py + 204 - (py + 17 + (tab == 1 ? 14 : 0) + 16)) / ROW_H);
			if (s < scroll[tab]) {
				scroll[tab] = s;
			} else if (s >= scroll[tab] + visible) {
				scroll[tab] = s - visible + 1;
			}
			click(1.1f);
			return true;
		}
		return super.keyPressed(event);
	}

	/** Test hook: select an entry by id on the current tab. */
	public void select(String id) {
		if (tab == TAB_CHEATS) {
			for (int i = 0; i < Cheats.ALL.size(); i++) {
				if (Cheats.ALL.get(i).id().equals(id)) {
					selected[tab] = i;
					detailScroll = 0;
					scroll[tab] = Math.max(0, i - 4);
				}
			}
			return;
		}
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).id.equals(id)) {
				selected[tab] = i;
				detailScroll = 0;
				int visible = Math.max(1, (188 - 31) / ROW_H);
				scroll[tab] = Math.max(0, i - visible / 2);
			}
		}
	}

	/** Test hook: the Creative page's buttons as drawn last frame, as {x, y, w, h} in GUI coordinates. */
	public List<int[]> buttons() {
		List<int[]> out = new ArrayList<>();
		for (Btn b : btns) {
			out.add(new int[] {b.x(), b.y(), b.w(), b.h()});
		}
		return out;
	}

	public int tabIndex() {
		return tab;
	}

	/** Test hook: how the Arsenal shows every item and block right now: "collected", "faded" (open but not obtained yet) or "hidden" ("???"). */
	public static java.util.Map<String, String> entryStates() {
		java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
		for (Row r : CodexModel.items(0)) {
			out.put(r.id, r.collected() ? "collected" : r.known() ? "faded" : "hidden");
		}
		return out;
	}

	/** Test hook: the scrollbar with this id as drawn last frame - {x, top, bottom, thumbTop, thumbHeight, value, max} - or null when there is none. */
	public int[] scrollbarInfo(int id) {
		for (Bar bar : bars) {
			if (bar.id() == id) {
				return new int[] {bar.x(), bar.top(), bar.bottom(), bar.thumbTop(), bar.thumbHeight(), bar.value().getAsInt(), bar.max()};
			}
		}
		return null;
	}

	public void setTabIndex(int t) {
		tab = t;
		dragging = -1;
		detailScroll = 0;
		rebuildRows();
	}

	public void setTier(int tier) {
		tierFilter = tier;
		rebuildRows();
	}
}
