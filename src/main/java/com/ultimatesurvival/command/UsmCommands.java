package com.ultimatesurvival.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.progress.Gates;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * /usm status | unlock &lt;tier|boss|all&gt; | lock &lt;boss&gt; | reset | codex | discover &lt;all|key&gt;
 * unlock clears the named gate and every earlier one; lock re-locks the named gate and every later one.
 */
public final class UsmCommands {
	private UsmCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("usm")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("status").executes(UsmCommands::status))
				.then(Commands.literal("unlock").then(Commands.argument("target", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(targets(), b))
						.executes(c -> unlock(c, StringArgumentType.getString(c, "target")))))
				.then(Commands.literal("lock").then(Commands.argument("target", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(targets(), b))
						.executes(c -> lock(c, StringArgumentType.getString(c, "target")))))
				.then(Commands.literal("reset").executes(UsmCommands::reset))
				.then(Commands.literal("bossrush")
						.then(Commands.literal("start").executes(c -> {
							var p = c.getSource().getPlayerOrException();
							return com.ultimatesurvival.mob.BossRush.start(p) ? 1 : 0;
						}))
						.then(Commands.literal("stop").executes(c -> {
							var p = c.getSource().getPlayerOrException();
							com.ultimatesurvival.mob.BossRush.stop(c.getSource().getServer(), p.getUUID(), "ultimatesurvival.rush.stopped");
							return 1;
						})))
				.then(Commands.literal("codex").executes(c -> {
					var src = c.getSource();
					if (src.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
						com.ultimatesurvival.codex.Codex.give(p);
						return 1;
					}
					for (var p : src.getServer().getPlayerList().getPlayers()) {
						com.ultimatesurvival.codex.Codex.give(p);
					}
					return 1;
				}))
				.then(Commands.literal("discover").then(Commands.argument("key", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(com.ultimatesurvival.codex.Discovery.allKeys(), b))
						.executes(c -> {
							String key = StringArgumentType.getString(c, "key");
							var pr = com.ultimatesurvival.progress.UsmState.progress();
							int n = 0;
							for (String k : key.equals("all") ? com.ultimatesurvival.codex.Discovery.allKeys() : java.util.List.of(key)) {
								n += pr.discover(k) ? 1 : 0;
							}
							int count = n;
							c.getSource().sendSuccess(() -> Component.literal("Discovered " + count + " entries."), true);
							return count;
						}))));
	}

	public static List<String> targets() {
		List<String> t = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "all"));
		for (Gate g : Content.GATES) {
			if (!g.isStart()) {
				t.add(g.id());
			}
		}
		return t;
	}

	/** tier number -> last gate order of that tier; gate id -> its order; "all" -> last; else -1. */
	public static int resolve(String target) {
		if (target.equals("all")) {
			return Content.GATES.size() - 1;
		}
		if (target.matches("[1-6]")) {
			int tier = Integer.parseInt(target);
			int order = -1;
			for (Gate g : Content.GATES) {
				if (g.tier() <= tier) {
					order = g.order();
				}
			}
			return order;
		}
		Gate g = Content.gate(target);
		return g == null ? -1 : g.order();
	}

	private static int status(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		src.sendSuccess(() -> Component.translatable("ultimatesurvival.cmd.status.header").withStyle(ChatFormatting.GOLD), false);
		src.sendSuccess(() -> Component.translatable("ultimatesurvival.cmd.status.tier", Gates.currentTier()), false);
		for (Gate g : Content.GATES) {
			boolean done = Gates.isCleared(g);
			src.sendSuccess(() -> Component.literal((done ? "[x] " : "[ ] ") + "T" + g.tier() + "  " + g.name())
					.withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
		}
		Gate next = Gates.nextGate();
		src.sendSuccess(() -> next == null
				? Component.translatable("ultimatesurvival.cmd.status.done")
				: Component.translatable("ultimatesurvival.cmd.status.next", next.name()), false);
		return 1;
	}

	private static int unlock(CommandContext<CommandSourceStack> c, String target) {
		int order = resolve(target);
		if (order < 0) {
			c.getSource().sendFailure(Component.translatable("ultimatesurvival.cmd.unknown", target));
			return 0;
		}
		Gates.unlockUpTo(c.getSource().getServer(), order);
		Gate g = Content.GATES.get(order);
		c.getSource().sendSuccess(() -> Component.translatable("ultimatesurvival.cmd.unlocked", g.name()), true);
		return 1;
	}

	/** The first gate order that "lock &lt;target&gt;" re-locks ("lock 3" means: from the first gate of tier 3 on); -1 for an unknown target. */
	public static int lockOrder(String target) {
		int order = resolve(target);
		if (order < 0) {
			return -1;
		}
		if (target.matches("[1-6]")) {
			int tier = Integer.parseInt(target);
			for (Gate g : Content.GATES) {
				if (g.tier() >= tier) {
					order = g.order();
					break;
				}
			}
		}
		return Math.max(order, 1);
	}

	private static int lock(CommandContext<CommandSourceStack> c, String target) {
		int order = lockOrder(target);
		if (order < 0) {
			c.getSource().sendFailure(Component.translatable("ultimatesurvival.cmd.unknown", target));
			return 0;
		}
		Gates.lockFrom(c.getSource().getServer(), order);
		Gate g = Content.GATES.get(order);
		c.getSource().sendSuccess(() -> Component.translatable("ultimatesurvival.cmd.locked", g.name()), true);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> c) {
		Gates.reset(c.getSource().getServer());
		c.getSource().sendSuccess(() -> Component.translatable("ultimatesurvival.cmd.reset"), true);
		return 1;
	}
}
