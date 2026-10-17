package com.ultimatesurvival.client;

import com.ultimatesurvival.armor.ArmorSets;
import com.ultimatesurvival.content.AttrSpec;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.content.ItemDef;
import com.ultimatesurvival.item.Accessories;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.UsmState;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Tooltip layout for every Ultimate Survival item:
 * name / tier / LOCKED warning / lore / abilities (generated from the real parameters) / armor-set progress.
 */
public final class ClientTooltips {
	private ClientTooltips() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Item item = stack.getItem();
			Gate gate = Gates.gateOf(item);
			if (gate == null) {
				return;
			}
			Language lang = Language.getInstance();
			String base = item.getDescriptionId();
			int at = Math.min(1, lines.size());
			lines.add(at++, Component.translatable("ultimatesurvival.tooltip.tier", gate.tier(), tierName(gate.tier())).withStyle(tierColor(gate.tier())));
			if (!Gates.isCleared(gate)) {
				lines.add(at++, Component.translatable("ultimatesurvival.tooltip.locked", gate.name()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			}
			ItemDef acc = Accessories.isAccessory(stack) ? UsmItems.defOf(item) : null;
			if (acc != null) {
				if (UsmState.progress() == null || !UsmState.progress().isDiscovered(Accessories.revealKey(acc.id))) {
					lines.add(at, Component.translatable("ultimatesurvival.tooltip.acc_hidden").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
					return;
				}
				for (AttrSpec a : AttrSpec.of(acc)) {
					lines.add(at++, statLine(a));
				}
			}
			if (lang.has(base + ".lore")) {
				lines.add(at++, Component.translatable(base + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			}
			for (int n = 0; lang.has(base + ".ab." + n); n++) {
				// "kind|title|text (lines separated by \n)|cooldown seconds"
				String[] part = lang.getOrDefault(base + ".ab." + n).split("\\|", -1);
				if (part.length < 3) {
					continue;
				}
				lines.add(at++, Component.literal(kindLabel(part[0])).withStyle(kindColor(part[0]))
						.append(Component.literal(part[1]).withStyle(ChatFormatting.WHITE)));
				for (String l : part[2].split("\n")) {
					lines.add(at++, Component.literal("  " + l).withStyle(ChatFormatting.GRAY));
				}
				if (part.length > 3 && !part[3].isEmpty()) {
					lines.add(at++, Component.translatable("ultimatesurvival.tooltip.cooldown", part[3]).withStyle(ChatFormatting.DARK_GRAY));
				}
			}
			ArmorSets.SetDef set = ArmorSets.of(item);
			if (set != null) {
				Player p = Minecraft.getInstance().player;
				int worn = p == null ? 0 : ArmorSets.worn(p, set);
				int total = set.pieces().size();
				lines.add(at++, Component.literal(" "));
				lines.add(at++, Component.translatable("ultimatesurvival.tooltip.set", lang.getOrDefault(set.langKey()), worn, total)
						.withStyle(worn >= total ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
				for (int n = 0; lang.has(set.langKey() + ".bonus." + n); n++) {
					String[] part = lang.getOrDefault(set.langKey() + ".bonus." + n).split("\\|", -1);
					boolean on = n < set.bonuses().size() && worn >= set.bonuses().get(n).need();
					ChatFormatting c = on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
					MutableComponent head = Component.literal("  (" + set.bonuses().get(n).need() + ") " + part[0]).withStyle(c);
					lines.add(at++, head);
					if (part.length > 1) {
						for (String l : part[1].split("\n")) {
							lines.add(at++, Component.literal("    " + l).withStyle(on ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
						}
					}
				}
			}
		});
	}

	/** "+2 Armor", "+10% Movement Speed" (negative amounts in red). */
	private static Component statLine(AttrSpec a) {
		boolean pct = a.op() != AttributeModifier.Operation.ADD_VALUE;
		double v = pct ? a.amount() * 100 : a.amount();
		String num = (v >= 0 ? "+" : "") + (v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v)) + (pct ? "%" : "");
		return Component.literal(num + " ").append(Component.translatable(a.attr().value().getDescriptionId()))
				.withStyle(v >= 0 ? ChatFormatting.BLUE : ChatFormatting.RED);
	}

	private static String kindLabel(String kind) {
		return Component.translatable("ultimatesurvival.tooltip.kind." + kind).getString();
	}

	private static ChatFormatting kindColor(String kind) {
		return switch (kind) {
			case "active" -> ChatFormatting.GOLD;
			case "hit" -> ChatFormatting.AQUA;
			case "kill" -> ChatFormatting.RED;
			case "wear" -> ChatFormatting.GREEN;
			case "use" -> ChatFormatting.LIGHT_PURPLE;
			default -> ChatFormatting.DARK_GREEN;
		};
	}

	private static String tierName(int tier) {
		return switch (tier) {
			case 1 -> "Early";
			case 2 -> "Mid";
			case 3 -> "High";
			case 4 -> "Late";
			case 5 -> "Endgame";
			default -> "Transcendent";
		};
	}

	private static ChatFormatting tierColor(int tier) {
		return switch (tier) {
			case 1 -> ChatFormatting.GREEN;
			case 2 -> ChatFormatting.AQUA;
			case 3 -> ChatFormatting.GOLD;
			case 4 -> ChatFormatting.RED;
			case 5 -> ChatFormatting.LIGHT_PURPLE;
			default -> ChatFormatting.AQUA;
		};
	}
}
