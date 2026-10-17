package com.ultimatesurvival.client.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ultimatesurvival.mob.UsmMob;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/** Renders any rig-based mob: the diffuse texture, an emissive glow pass, and the animation state from the entity. */
public class RigRenderer extends MobRenderer<UsmMob, RigRenderState, RigModel> {
	private final Identifier texture;

	public RigRenderer(EntityRendererProvider.Context ctx, RigDef def, ModelLayerLocation layer, Identifier texture, Identifier glow, float shadow) {
		super(ctx, new RigModel(ctx.bakeLayer(layer), def), shadow);
		this.texture = texture;
		addLayer(new GlowLayer(this, glow));
	}

	@Override
	public Identifier getTextureLocation(RigRenderState state) {
		return texture;
	}

	@Override
	public RigRenderState createRenderState() {
		return new RigRenderState();
	}

	@Override
	public void extractRenderState(UsmMob entity, RigRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.anim = entity.anim();
		state.animProgress = entity.animProgress(partialTicks);
		state.flags = entity.flagBits();
		state.phase = entity.phaseIndex();
	}

	@Override
	protected float getFlipDegrees() {
		return 0.0f;                                  // bosses crumble in place instead of toppling over
	}

	/** Emissive texture pass: lava cracks and eyes stay bright in the dark, and pulse harder when the mob is hot or enraged. */
	static final class GlowLayer extends RenderLayer<RigRenderState, RigModel> {
		private final Identifier glow;

		GlowLayer(RenderLayerParent<RigRenderState, RigModel> parent, Identifier glow) {
			super(parent);
			this.glow = glow;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, RigRenderState state, float yRot, float xRot) {
			float pulse = 0.78f + 0.22f * Mth.sin(state.ageInTicks * 0.13f);
			if ((state.flags & (UsmMob.F_HOT | UsmMob.F_ENRAGED)) != 0) {
				pulse = 1.0f + 0.35f * Mth.sin(state.ageInTicks * 0.5f);
			} else if ((state.flags & UsmMob.F_VULN) != 0) {
				pulse = 0.45f + 0.2f * Mth.sin(state.ageInTicks * 0.3f);
			}
			pulse = Mth.clamp(pulse, 0f, 1f);
			int color = ARGB.colorFromFloat(1.0f, pulse, pulse, pulse);
			collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(glow), LightCoordsUtil.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
		}
	}
}
