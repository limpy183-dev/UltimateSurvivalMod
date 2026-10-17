package com.ultimatesurvival.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.entity.BoltSpec;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.UsmMob;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Dresses the vanilla boss bar of a Sovereign: its emblem beside the bar, a themed rule under it, a tick where every phase begins,
 * and a pip for each phase (lit as the fight advances). Drawn over the vanilla bar, which stays in charge of the name and the fill.
 */
public final class BossBarStyle implements HudElement {
	private static final int BAR_W = 182;

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, UltimateSurvival.id("boss_bar_style"), new BossBarStyle());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		UsmBoss boss = null;
		double best = 160 * 160;
		for (var e : mc.level.entitiesForRendering()) {
			if (e instanceof UsmBoss b && b.isAlive() && !b.hasFlag(UsmMob.F_SPAWNING)) {
				double d = b.distanceToSqr(mc.player);
				if (d < best) {
					best = d;
					boss = b;
				}
			}
		}
		if (boss == null) {
			return;
		}
		MobDef def = boss.def();
		int color = 0xFF000000 | BoltSpec.colorOf(def.style);
		int x = g.guiWidth() / 2 - BAR_W / 2;
		int y = 12;
		Identifier emblem = UltimateSurvival.id("textures/gui/emblem/" + def.id + ".png");
		RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
		g.blit(pipeline, emblem, x - 26, y - 8, 0f, 0f, 22, 22, 32, 32, 32, 32);
		g.blit(pipeline, emblem, x + BAR_W + 4, y - 8, 0f, 0f, 22, 22, 32, 32, 32, 32);
		g.fill(x, y + 7, x + BAR_W, y + 8, (color & 0x00FFFFFF) | 0xAA000000);
		int phases = def.phases.size();
		int now = boss.phaseIndex();
		for (int i = 1; i < phases; i++) {                                         // where the next phase begins on the bar
			int tx = x + (int) Math.round(BAR_W * def.phases.get(i).at());
			g.fill(tx, y - 1, tx + 1, y + 7, i <= now ? 0x66FFFFFF : 0xDDFFFFFF);
		}
		int pipX = g.guiWidth() / 2 - (phases * 8) / 2;
		for (int i = 0; i < phases; i++) {
			int px = pipX + i * 8;
			boolean lit = i <= now;
			g.fill(px + 2, y + 10, px + 5, y + 13, lit ? color : 0xFF2A2A34);
			g.fill(px + 3, y + 9, px + 4, y + 10, lit ? color : 0xFF2A2A34);
			g.fill(px + 3, y + 13, px + 4, y + 14, lit ? color : 0xFF2A2A34);
		}
		if (boss.hasFlag(UsmMob.F_ENRAGED)) {
			long t = mc.level.getGameTime();
			g.fill(x, y + 7, x + BAR_W, y + 8, ((t / 4) % 2 == 0) ? 0xFFFF2020 : 0xFF801010);
		}
	}
}
