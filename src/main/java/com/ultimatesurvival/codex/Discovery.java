package com.ultimatesurvival.codex;

import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.net.Payloads;
import com.ultimatesurvival.progress.Progress;
import com.ultimatesurvival.progress.UsmState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Fills the Codex as the player progresses. Keys: item:&lt;id&gt; (obtained), mob:&lt;id&gt; (met), boss:&lt;id&gt; (met),
 * bossdown:&lt;id&gt; is implied by a cleared gate. Scans once a second; new entries trigger one notice.
 */
public final class Discovery {
	private Discovery() {
	}

	public static String itemKey(String id) {
		return "item:" + id;
	}

	public static String mobKey(String id) {
		return "mob:" + id;
	}

	public static String bossKey(String id) {
		return "boss:" + id;
	}

	/** Server tick for one player; cheap early-out except once per second. */
	public static void tick(ServerPlayer p) {
		if (p.tickCount % 20 != 7) {
			return;
		}
		Progress pr = UsmState.progress();
		List<String> fresh = new ArrayList<>();
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			scanStack(pr, p.getInventory().getItem(i), fresh);
		}
		for (EquipmentSlot s : EquipmentSlot.VALUES) {
			scanStack(pr, p.getItemBySlot(s), fresh);
		}
		scanStack(pr, p.containerMenu.getCarried(), fresh);
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			var stored = p.getInventory().getItem(i).get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
			if (stored != null) {
				for (var en : stored.entrySet()) {
					if (en.getIntValue() >= 6) {                                                                // a tome beyond anything the table gives
						com.ultimatesurvival.progress.Advancements.award(p, "tome");
					}
				}
			}
		}
		// Sovereigns and mobs from this mod that are close enough to be "met"
		AABB box = p.getBoundingBox().inflate(40);
		for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box)) {
			var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
			if (!key.getNamespace().equals("ultimatesurvival") && !key.equals(net.minecraft.resources.Identifier.withDefaultNamespace("ender_dragon"))) {
				continue;
			}
			String id = key.getPath();
			boolean boss = Content.gate(id) != null;
			String k = boss ? bossKey(id) : mobKey(id);
			if (e.distanceToSqr(p) < 32 * 32 && pr.discover(k)) {
				fresh.add(k);
			}
		}
		if (!fresh.isEmpty()) {
			notify(p, fresh);
			if (itemsKnown() >= allKeys().size()) {
				com.ultimatesurvival.progress.Advancements.award(p, "codex_complete");
			}
		}
	}

	private static void scanStack(Progress pr, ItemStack s, List<String> fresh) {
		if (s.isEmpty()) {
			return;
		}
		Item it = s.getItem();
		var key = BuiltInRegistries.ITEM.getKey(it);
		if (key.getNamespace().equals("ultimatesurvival") && !isHidden(key.getPath())) {
			String k = itemKey(key.getPath());
			if (pr.discover(k)) {
				fresh.add(k);
			}
		}
	}

	private static boolean isHidden(String id) {
		var def = Content.ITEMS.stream().filter(d -> d.id.equals(id)).findFirst();
		return def.isPresent() && def.get().json.has("hidden");
	}

	/** Discover directly (commands, tests, boss kills). Returns true if it was new. */
	public static boolean discover(ServerPlayer p, String key) {
		if (UsmState.progress().discover(key)) {
			notify(p, List.of(key));
			return true;
		}
		return false;
	}

	public static void notify(ServerPlayer p, List<String> keys) {
		if (ServerPlayNetworking.canSend(p, Payloads.CodexNotice.TYPE)) {
			ServerPlayNetworking.send(p, new Payloads.CodexNotice(new ArrayList<>(keys)));
		}
	}

	/** Every entry key that exists right now (for /usm discover all and for the Codex totals). */
	public static List<String> allKeys() {
		List<String> keys = new ArrayList<>();
		for (var def : Content.ITEMS) {
			if (!def.json.has("hidden")) {
				keys.add(itemKey(def.id));
			}
		}
		UsmBlocks.ITEMS.keySet().forEach(id -> keys.add(itemKey(id)));
		for (Gate g : Content.GATES) {
			if (g.boss() != null) {
				keys.add(bossKey(g.id()));
			}
		}
		if (Content.root.has("mobs")) {
			Content.root.getAsJsonArray("mobs").forEach(m -> {
				String id = m.getAsJsonObject().get("id").getAsString();
				if (Content.gate(id) == null) {                                      // bosses are met as boss: entries
					keys.add(mobKey(id));
				}
			});
		}
		return keys;
	}

	public static int itemsKnown() {
		Progress pr = UsmState.progress();
		return (int) allKeys().stream().filter(pr::isDiscovered).count();
	}
}
