package com.ultimatesurvival.client;

import com.ultimatesurvival.UltimateSurvival;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The full-screen colour wash driven by {@code Payloads.ScreenFlash}: a quick rise, then a long fade, drawn under the hotbar so the HUD stays readable. */
public final class ClientFlash implements HudElement {
	private static int rgb;
	private static int total;
	private static int left;
	private static float strength;

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, UltimateSurvival.id("screen_flash"), new ClientFlash());
	}

	public static void start(int color, int ticks, float peak) {
		if (left <= 0 || peak >= alpha(0)) {
			rgb = color;
			total = Math.max(1, ticks);
			left = total;
			strength = peak;
		}
	}

	public static void tick() {
		if (left > 0) {
			left--;
		}
	}

	public static boolean active() {
		return left > 0;
	}

	public static void reset() {
		left = 0;
	}

	/** Current opacity 0..1: 12% of the time to flare up, the rest to fade with an ease-out. */
	public static float alpha(float partial) {
		if (left <= 0) {
			return 0f;
		}
		float t = 1f - Math.max(0f, left - partial) / total;
		float shape = t < 0.12f ? t / 0.12f : (float) Math.pow(1.0 - (t - 0.12f) / 0.88f, 1.8);
		return strength * Math.max(0f, Math.min(1f, shape));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		float a = alpha(delta.getGameTimeDeltaPartialTick(false));
		if (a < 0.01f) {
			return;
		}
		g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (a * 255) << 24) | (rgb & 0xFFFFFF));
	}
}
