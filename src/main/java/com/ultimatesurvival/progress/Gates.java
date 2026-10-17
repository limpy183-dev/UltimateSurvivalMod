package com.ultimatesurvival.progress;

import com.ultimatesurvival.ability.ConsumableAbilitiesAccess;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.mixin.LivingEntityAccessor;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lock / unlock rules.
 *
 * <p>Design: every mod item belongs to one gate (a boss). While that gate is not cleared the item is
 * "locked": it still exists, can be picked up and carried, but contributes no attack damage, armor,
 * mining speed or ability (see {@code ItemStackMixin} and the item classes), and its tooltip says
 * which boss unlocks it. Locked recipes never produce a result (see {@code CraftingMenuMixin}).
 */
public final class Gates {
	private static final Map<Item, Gate> ITEM_GATE = new IdentityHashMap<>();

	private Gates() {
	}

	public static void bind(Item item, Gate gate) {
		ITEM_GATE.put(item, gate);
	}

	public static Gate gateOf(Item item) {
		return ITEM_GATE.get(item);
	}

	public static boolean isCleared(Gate gate) {
		return gate.isStart() || UsmState.progress().isDefeated(gate.id());
	}

	public static boolean isCleared(String gateId) {
		Gate g = Content.gate(gateId);
		return g != null && isCleared(g);
	}

	public static boolean isLocked(Item item) {
		Gate g = ITEM_GATE.get(item);
		return g != null && !isCleared(g);
	}

	public static boolean isLocked(ItemStack stack) {
		return !stack.isEmpty() && isLocked(stack.getItem());
	}

	/** Highest tier among cleared gates (1 when nothing was beaten). */
	public static int currentTier() {
		int tier = 1;
		for (Gate g : Content.GATES) {
			if (isCleared(g)) {
				tier = Math.max(tier, g.tier());
			}
		}
		return tier;
	}

	/** First gate (in order) that is not cleared, or null when everything is beaten. */
	public static Gate nextGate() {
		for (Gate g : Content.GATES) {
			if (!isCleared(g)) {
				return g;
			}
		}
		return null;
	}

	public static Gate gateForBoss(Entity entity) {
		String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		for (Gate g : Content.GATES) {
			if (id.equals(g.boss())) {
				return g;
			}
		}
		return null;
	}

	// ---- mutations --------------------------------------------------------------------------

	/** A boss really died: clear its gate and tell the world. */
	public static void defeat(MinecraftServer server, Gate gate) {
		if (isCleared(gate)) {
			return;
		}
		mutate(server, () -> UsmState.progress().markDefeated(gate.id()));
		server.getPlayerList().broadcastSystemMessage(
				Component.translatable("ultimatesurvival.msg.gate_cleared", gate.name(), gate.tier())
						.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.level().playSound(null, p.blockPosition(), com.ultimatesurvival.sound.UsmSounds.get("gate.cleared", net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
					net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);
		}
		Advancements.awardAll(server, gate.id());
	}

	/** Command helper: clear this gate and every earlier one. */
	public static void unlockUpTo(MinecraftServer server, int order) {
		mutate(server, () -> {
			for (Gate g : Content.GATES) {
				if (g.order() <= order && !g.isStart()) {
					UsmState.progress().markDefeated(g.id());
				}
			}
		});
	}

	/** Command helper: re-lock this gate and every later one. */
	public static void lockFrom(MinecraftServer server, int order) {
		mutate(server, () -> {
			for (Gate g : Content.GATES) {
				if (g.order() >= order && !g.isStart()) {
					UsmState.progress().unmarkDefeated(g.id());
				}
			}
		});
	}

	public static void reset(MinecraftServer server) {
		mutate(server, () -> {
			Progress p = UsmState.progress();
			p.clearGates();
			p.clearDiscoveries();
			p.clearCounters();
			p.clearCodexGiven();
		});
	}

	/**
	 * Runs a state change and makes every online player's equipment re-evaluate: vanilla only
	 * recomputes item attribute modifiers when the equipped stack changes, so we strip the modifiers
	 * that were applied under the old state and clear the "last seen equipment" cache to force the
	 * new state to be applied on the next tick.
	 */
	private static void mutate(MinecraftServer server, Runnable change) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer p : players) {
			// strip what vanilla last applied (its "last seen equipment" cache), not just what is held right now:
			// gear swapped in the same tick would otherwise keep its old modifiers forever
			for (var entry : ((LivingEntityAccessor) p).usm$lastEquipmentItems().entrySet()) {
				EquipmentSlot slot = entry.getKey();
				ItemStack applied = entry.getValue();
				if (!applied.isEmpty()) {
					applied.forEachModifier(slot, (attribute, modifier) -> {
						AttributeInstance inst = p.getAttributes().getInstance(attribute);
						if (inst != null) {
							inst.removeModifier(modifier.id());
						}
					});
				}
			}
		}
		change.run();
		for (ServerPlayer p : players) {
			((LivingEntityAccessor) p).usm$lastEquipmentItems().replaceAll((slot, stack) -> ItemStack.EMPTY);
			ConsumableAbilitiesAccess.applyHearts(p);                                                // a reset clears the heart counter: the hearts go with it now, not at the next 5-second sweep
			RecipeSync.sync(p);
		}
	}
}
