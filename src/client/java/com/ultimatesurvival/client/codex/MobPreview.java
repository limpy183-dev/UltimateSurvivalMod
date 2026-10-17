package com.ultimatesurvival.client.codex;

import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.UsmMob;
import com.ultimatesurvival.mob.attack.Attacks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Live 3D preview of a mob in the Codex: a client-only dummy entity (never added to the world) that follows the mouse
 * and replays the mob's own attack animations, so the book shows exactly what the fight will look like.
 */
final class MobPreview {
	private static final Map<String, UsmMob> CACHE = new HashMap<>();
	private static final Map<String, List<Object[]>> ANIMS = new HashMap<>();
	private static int clock;

	private MobPreview() {
	}

	static boolean has(String id) {
		return Mobs.TYPES.containsKey(id);
	}

	static UsmMob get(String id) {
		UsmMob m = CACHE.get(id);
		var level = Minecraft.getInstance().level;
		if (m == null && level != null && has(id)) {
			m = Mobs.TYPES.get(id).create(level, EntitySpawnReason.LOAD);
			if (m != null) {
				m.setId(0x7FFF0000 + CACHE.size());                 // renderers read the id; the dummy never joins the world
				CACHE.put(id, m);
			}
		}
		return m;
	}

	/** One Object[]{animId, lengthTicks} per distinct attack of the mob, plus its melee swing. */
	private static List<Object[]> animsOf(MobDef d) {
		return ANIMS.computeIfAbsent(d.id, k -> {
			List<Object[]> out = new ArrayList<>();
			java.util.Set<String> seen = new java.util.HashSet<>();
			for (MobDef.Phase ph : d.phases) {
				for (MobDef.AttackSpec s : ph.attacks()) {
					String anim = s.p().s("anim", s.type());
					if (seen.add(anim)) {
						out.add(new Object[] {anim, Attacks.create(s).estimateLength()});
					}
				}
			}
			if (d.melee && seen.add("melee")) {
				out.add(new Object[] {"melee", 14});
			}
			return out;
		});
	}

	static void tick() {
		clock++;
		for (Map.Entry<String, UsmMob> e : CACHE.entrySet()) {
			UsmMob m = e.getValue();
			m.tickCount++;
			List<Object[]> anims = animsOf(m.def());
			int period = 110;
			if (!anims.isEmpty() && clock % period == 40) {
				Object[] a = anims.get((clock / period + Math.abs(e.getKey().hashCode())) % anims.size());
				m.startAnim((String) a[0], (int) a[1]);
			}
		}
	}

	static void clear() {
		CACHE.clear();
	}

	static void draw(GuiGraphicsExtractor g, String id, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
		UsmMob m = get(id);
		if (m == null) {
			return;
		}
		float h = Math.max(0.6f, m.getBbHeight() / Math.max(0.1f, m.getScale()));
		float w = Math.max(0.4f, m.getBbWidth() / Math.max(0.1f, m.getScale()));
		int size = (int) Math.max(6, 0.76f * (y1 - y0) / Math.max(h, w * 0.9f));
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, size, 0.0625f, mouseX, mouseY, m);
	}
}
