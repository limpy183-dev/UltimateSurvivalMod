package com.ultimatesurvival.net;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.armor.WearerAbilities;
import com.ultimatesurvival.item.Accessories;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Network payloads. Singleplayer still talks through the integrated server's local connection. */
public final class Payloads {
	private Payloads() {
	}

	/** Client asks the server to open the accessory menu. */
	public record OpenAccessories() implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<OpenAccessories> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("open_accessories"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccessories> CODEC = StreamCodec.unit(new OpenAccessories());

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client pressed jump while airborne. */
	public record DoubleJump() implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<DoubleJump> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("double_jump"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJump> CODEC = StreamCodec.unit(new DoubleJump());

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server tells the client which Codex entries were just discovered (client shows a toast). */
	public record CodexNotice(java.util.List<String> keys) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<CodexNotice> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("codex_notice"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CodexNotice> CODEC = StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()),
				CodexNotice::keys, CodexNotice::new);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server tells a nearby client to rattle its camera: strength 0..1, duration in ticks. */
	public record Shake(float strength, int ticks) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<Shake> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("shake"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Shake> CODEC = StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.FLOAT, Shake::strength, net.minecraft.network.codec.ByteBufCodecs.VAR_INT, Shake::ticks, Shake::new);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server tells a client to wash the screen in a colour: 0xRRGGBB, a duration in ticks and the peak opacity 0..1. */
	public record ScreenFlash(int rgb, int ticks, float strength) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<ScreenFlash> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("screen_flash"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFlash> CODEC = StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.INT, ScreenFlash::rgb, net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ScreenFlash::ticks,
				net.minecraft.network.codec.ByteBufCodecs.FLOAT, ScreenFlash::strength, ScreenFlash::new);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client sets or triggers a Creative-page cheat (see {@link com.ultimatesurvival.cheat.Cheats}); the server ignores it outside creative mode. */
	public record CheatAction(String id, int value) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<CheatAction> TYPE = new CustomPacketPayload.Type<>(UltimateSurvival.id("cheat"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CheatAction> CODEC = StreamCodec.composite(
				net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, CheatAction::id, net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CheatAction::value, CheatAction::new);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(CheatAction.TYPE, CheatAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CheatAction.TYPE, (payload, ctx) -> com.ultimatesurvival.cheat.Cheats.apply(ctx.player(), payload.id(), payload.value()));
		PayloadTypeRegistry.clientboundPlay().register(Shake.TYPE, Shake.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ScreenFlash.TYPE, ScreenFlash.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CodexNotice.TYPE, CodexNotice.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenAccessories.TYPE, OpenAccessories.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DoubleJump.TYPE, DoubleJump.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(OpenAccessories.TYPE, (payload, ctx) -> Accessories.open(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(DoubleJump.TYPE, (payload, ctx) -> WearerAbilities.doubleJump(ctx.player()));
	}
}
