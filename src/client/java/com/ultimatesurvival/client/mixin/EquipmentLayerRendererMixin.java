package com.ultimatesurvival.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ultimatesurvival.client.ArmorGlow;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Worn armor may carry a second layer called "{name}_glow" (the eyes, runes and sigils of the set): it is drawn at full brightness, so it shines in the dark.
 * Two redirects in the layer loop: one notes the texture of the layer about to be drawn, the other lights it.
 */
@Mixin(net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
	private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;"
			+ "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

	@Redirect(method = RENDER_LAYERS, at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object usm$noteTexture(Function<Object, Object> lookup, Object key) {
		Object texture = lookup.apply(key);
		ArmorGlow.note(texture);
		return texture;
	}

	@Redirect(method = RENDER_LAYERS, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel("
			+ "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
			+ "ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
	private void usm$lightGlow(OrderedSubmitNodeCollector collector, Model<Object> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
			TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		collector.submitModel(model, state, pose, type, ArmorGlow.take() ? LightCoordsUtil.FULL_BRIGHT : light, overlay, color, sprite, outline, crumbling);
	}
}
