package com.ultimatesurvival.client.rig;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Everything the rig animation needs, copied from the entity once per frame. */
public class RigRenderState extends LivingEntityRenderState {
	public String anim = "";
	/** 0..1 through the playing animation, -1 when idle. */
	public float animProgress = -1;
	public int flags;
	public int phase;
}
