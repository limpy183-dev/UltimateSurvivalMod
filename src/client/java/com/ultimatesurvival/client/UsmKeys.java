package com.ultimatesurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.net.Payloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Keybinds and the client half of double jump. */
public final class UsmKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(UltimateSurvival.id("main"));
	public static KeyMapping accessories;
	public static KeyMapping codex;
	private static boolean jumpWasDown;

	private UsmKeys() {
	}

	public static void register() {
		accessories = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ultimatesurvival.accessories", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
		codex = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ultimatesurvival.codex", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(UsmKeys::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null) {
			return;
		}
		while (codex.consumeClick()) {
			if (mc.gui.screen() == null) {
				if (com.ultimatesurvival.codex.CodexItem.carries(mc.player)) {
					mc.gui.setScreen(new com.ultimatesurvival.client.codex.CodexScreen());
				} else {
					mc.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("codex.no_book"));
				}
			}
		}
		while (accessories.consumeClick()) {
			if (mc.gui.screen() == null) {
				ClientPlayNetworking.send(new Payloads.OpenAccessories());
			}
		}
		// rising edge of the jump key while airborne = a double-jump request (the server validates it)
		boolean down = mc.options.keyJump.isDown();
		if (down && !jumpWasDown && mc.gui.screen() == null && !mc.player.onGround() && !mc.player.isInWater()
				&& !mc.player.getAbilities().flying && !mc.player.isFallFlying() && !mc.player.onClimbable()) {
			ClientPlayNetworking.send(new Payloads.DoubleJump());
		}
		jumpWasDown = down;
	}
}
