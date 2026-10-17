package com.ultimatesurvival.item;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.AbilityRef;
import com.ultimatesurvival.content.AttrSpec;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.Progress;
import com.ultimatesurvival.progress.UsmState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Accessory slots: six slots per player (the Creative cheat can raise it), stored in world progress data (they stay on death).
 * Attribute bonuses of all equipped accessories add up; ability bonuses go through {@link com.ultimatesurvival.armor.Wearer}.
 */
public final class Accessories {
	public static ExtendedMenuType<AccessoryMenu, Integer> MENU;
	private static final Map<UUID, Set<Identifier>> APPLIED = new HashMap<>();

	private Accessories() {
	}

	public static void registerMenu() {
		MENU = Registry.register(BuiltInRegistries.MENU, UltimateSurvival.id("accessories"),
				new ExtendedMenuType<AccessoryMenu, Integer>((id, inv, data) -> new AccessoryMenu(id, inv, new SimpleContainer(data), data),
						ByteBufCodecs.VAR_INT));
	}

	public static boolean isAccessory(ItemStack s) {
		ItemDef d = s.isEmpty() ? null : UsmItems.defOf(s.getItem());
		return d != null && d.kind.equals("accessory");
	}

	/** Progress key set the first time this accessory sits in a slot; the tooltip shows its details only after that. */
	public static String revealKey(String itemId) {
		return "acc:" + itemId;
	}

	/** Container view over the player's stored accessory list; every change is written back. */
	private static final class Backed extends SimpleContainer {
		private final List<ItemStack> store;
		private final Progress progress;
		private boolean loading = true;

		Backed(ServerPlayer p) {
			super(Progress.slots());
			this.progress = UsmState.progress();
			this.store = progress.accessories(p.getUUID());
			for (int i = 0; i < Progress.slots(); i++) {
				super.setItem(i, store.get(i).copy());
			}
			loading = false;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if (loading) {
				return; // setItem() above calls this while later slots are still empty; writing back would wipe the store
			}
			for (int i = 0; i < Progress.slots(); i++) {
				store.set(i, getItem(i).copy());
			}
			progress.setDirty();
		}
	}

	public static void open(ServerPlayer p) {
		p.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer player) {
				return Progress.slots();
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("container.ultimatesurvival.accessories");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
				return new AccessoryMenu(id, inv, new Backed((ServerPlayer) player), Progress.slots());
			}
		});
	}

	/** Equipped, unlocked accessory stacks. */
	public static List<ItemStack> equipped(Player p) {
		return UsmState.progress().accessories(p.getUUID()).subList(0, Progress.slots());
	}

	/** Adds the ability bonuses of the player's accessories to {@code out} (keyed by ability id). */
	public static void collect(ServerPlayer p, Map<String, AbilityRef> out) {
		for (ItemStack s : equipped(p)) {
			ItemDef d = s.isEmpty() ? null : UsmItems.defOf(s.getItem());
			if (d == null || !d.kind.equals("accessory") || Gates.isLocked(s)) {
				continue;
			}
			for (AbilityRef r : d.abilities) {
				out.putIfAbsent(r.id(), r);
			}
		}
	}

	/** Re-applies the summed attribute bonuses of all equipped accessories (call every few ticks). */
	public static void tickAttributes(ServerPlayer p) {
		Map<Identifier, Double> sums = new HashMap<>();
		Map<Identifier, AttrSpec> proto = new HashMap<>();
		for (ItemStack s : equipped(p)) {
			ItemDef d = s.isEmpty() ? null : UsmItems.defOf(s.getItem());
			if (d == null || !d.kind.equals("accessory")) {
				continue;
			}
			UsmState.progress().discover(revealKey(d.id));                       // once placed, its tooltip stays revealed for good
			if (Gates.isLocked(s)) {
				continue;
			}
			for (AttrSpec a : AttrSpec.of(d)) {
				Identifier id = a.id("acc");
				sums.merge(id, a.amount(), Double::sum);
				proto.putIfAbsent(id, a);
			}
		}
		Set<Identifier> prev = APPLIED.getOrDefault(p.getUUID(), Set.of());
		for (Map.Entry<Identifier, Double> e : sums.entrySet()) {
			AttrSpec a = proto.get(e.getKey());
			AttributeInstance inst = p.getAttributes().getInstance(a.attr());
			if (inst != null) {
				inst.addOrUpdateTransientModifier(new AttributeModifier(e.getKey(), e.getValue(), a.op()));
			}
		}
		for (Identifier old : prev) {
			if (!sums.containsKey(old)) {
				removeById(p, old);
			}
		}
		if (sums.isEmpty()) {
			APPLIED.remove(p.getUUID());
		} else {
			APPLIED.put(p.getUUID(), new HashSet<>(sums.keySet()));
		}
	}

	private static void removeById(ServerPlayer p, Identifier id) {
		for (Holder.Reference<Attribute> h : BuiltInRegistries.ATTRIBUTE.listElements().toList()) {
			AttributeInstance inst = p.getAttributes().getInstance(h);
			if (inst != null && inst.hasModifier(id)) {
				inst.removeModifier(id);
			}
		}
	}

	public static void forget(UUID id) {
		APPLIED.remove(id);
	}
}
