package com.ultimatesurvival.client;

import com.ultimatesurvival.UltimateSurvival;
import net.minecraft.resources.Identifier;

/** Book-keeping for the emissive layer of worn armor (see EquipmentLayerRendererMixin): the "{set}_glow" sheets are drawn at full brightness. */
public final class ArmorGlow {
	/** How many glow layers have been submitted so far (tests read this). */
	public static int lit;
	private static boolean next;

	private ArmorGlow() {
	}

	/** Called with the texture the renderer is about to use for a layer. */
	public static void note(Object texture) {
		next = texture instanceof Identifier id && id.getNamespace().equals(UltimateSurvival.MOD_ID) && id.getPath().endsWith("_glow.png");
	}

	/** True once for the layer that follows a glow texture. */
	public static boolean take() {
		boolean glow = next;
		next = false;
		if (glow) {
			lit++;
		}
		return glow;
	}
}
