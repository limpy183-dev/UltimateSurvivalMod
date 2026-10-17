package com.ultimatesurvival.client;

import com.ultimatesurvival.client.codex.CodexScreen;
import com.ultimatesurvival.client.gui.AccessoryScreen;
import com.ultimatesurvival.codex.CodexItem;
import com.ultimatesurvival.entity.UsmEntities;
import com.ultimatesurvival.item.Accessories;
import com.ultimatesurvival.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class UltimateSurvivalClient implements ClientModInitializer {
	private static final SystemToast.SystemToastId CODEX_TOAST = new SystemToast.SystemToastId();

	@Override
	public void onInitializeClient() {
		ClientTooltips.register();
		UsmKeys.register();
		AbilityHud.register();
		MenuScreens.register(Accessories.MENU, AccessoryScreen::new);
		EntityRenderers.register(UsmEntities.BOLT, ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
		com.ultimatesurvival.client.rig.Rigs.register();
		ParticleSetup.register();
		BossBarStyle.register();
		ClientFlash.register();
		ClientPlayNetworking.registerGlobalReceiver(Payloads.ScreenFlash.TYPE, (payload, ctx) -> ClientFlash.start(payload.rgb(), payload.ticks(), payload.strength()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Shake.TYPE, (payload, ctx) -> ClientShake.start(payload.strength(), payload.ticks()));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ClientShake.tick();
			ClientFlash.tick();
		});

		CodexItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new CodexScreen());
		ClientPlayNetworking.registerGlobalReceiver(Payloads.CodexNotice.TYPE, (payload, ctx) -> {
			Minecraft mc = ctx.client();
			var keys = payload.keys();
			Component msg = keys.size() == 1 ? Component.translatable("codex.notice.one", entryName(keys.get(0)))
					: Component.translatable("codex.notice.many", keys.size());
			SystemToast.addOrUpdate(mc.gui.toastManager(), CODEX_TOAST, Component.translatable("codex.title"), msg);
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 0.5f));
		});
	}

	/** "item:verdite_ingot" -> its display name. */
	private static Component entryName(String key) {
		int c = key.indexOf(':');
		String kind = key.substring(0, c);
		String id = key.substring(c + 1);
		return switch (kind) {
			case "item" -> {
				var item = com.ultimatesurvival.item.UsmItems.BY_ID.get(id);
				yield item != null ? Component.translatable(item.getDescriptionId())
						: Component.translatable("block." + com.ultimatesurvival.UltimateSurvival.MOD_ID + "." + id);
			}
			case "boss" -> {
				var g = com.ultimatesurvival.content.Content.gate(id);
				yield Component.literal(g != null ? g.name() : id);
			}
			default -> Component.translatable("entity." + com.ultimatesurvival.UltimateSurvival.MOD_ID + "." + id);
		};
	}
}
