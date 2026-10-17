package com.ultimatesurvival.sound;

import com.google.gson.JsonElement;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.Content;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own sound events. The ids come from the content manifest ("sounds"); each one is a redirect onto a vanilla sound with its own
 * pitch and volume (assets/ultimatesurvival/sounds.json), so every theme, boss and creature has a voice of its own.
 */
public final class UsmSounds {
	private static final Map<String, Holder.Reference<SoundEvent>> BY_ID = new LinkedHashMap<>();

	private UsmSounds() {
	}

	public static void register() {
		if (!Content.root.has("sounds")) {
			return;
		}
		for (JsonElement e : Content.root.getAsJsonArray("sounds")) {
			String id = e.getAsString();
			Identifier key = UltimateSurvival.id(id);
			BY_ID.put(id, Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key)));
		}
		UltimateSurvival.LOGGER.info("Registered {} mod sound events", BY_ID.size());
	}

	/** The mod event with this id ("style.void.warn"), or {@code fallback} when the manifest does not define it. */
	public static SoundEvent get(String id, SoundEvent fallback) {
		Holder.Reference<SoundEvent> h = BY_ID.get(id);
		return h == null ? fallback : h.value();
	}

	public static Holder<SoundEvent> holder(String id) {
		return BY_ID.get(id);
	}

	public static boolean has(String id) {
		return BY_ID.containsKey(id);
	}

	public static Iterable<String> ids() {
		return BY_ID.keySet();
	}
}
