package com.ultimatesurvival.ability;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.progress.Progress;
import com.ultimatesurvival.progress.UsmState;
import com.ultimatesurvival.util.Effects;
import com.ultimatesurvival.util.Fx;
import com.ultimatesurvival.util.Tasks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Abilities used by drinks, food and utility gadgets. */
final class ConsumableAbilities {
	private ConsumableAbilities() {
	}

	static void register() {
		Abilities.active("heal", 0, c -> {
			c.player().heal((float) c.p().d("amount", 10));
			Fx.ring(c.level(), c.player().position(), 0.8, 16, ParticleTypes.HEART, 1.0);
			return true;
		});

		// up to three effects per item: effect/ticks/amp, effect2/ticks2/amp2, effect3/...
		Abilities.active("give_effect", 0, c -> {
			for (String suffix : new String[] {"", "2", "3"}) {
				if (c.p().params().has("effect" + suffix)) {
					Effects.give(c.player(), c.p().s("effect" + suffix, "speed"), c.p().i("ticks" + suffix, 600), c.p().i("amp" + suffix, 0));
				}
			}
			c.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, c.player().getX(), c.player().getY() + 1, c.player().getZ(),
					Fx.n(10), 0.4, 0.6, 0.4, 0);
			return true;
		});

		Abilities.active("cleanse", 0, c -> {
			c.player().removeAllEffects();
			Fx.ring(c.level(), c.player().position(), 0.9, 16, ParticleTypes.END_ROD, 1.0);
			return true;
		});

		// permanent max-health upgrade, counted per player, capped by "max"
		Ability heart = Abilities.active("perm_health", 0, c -> {
			Progress pr = UsmState.progress();
			String key = heartKey(c.player());
			if (pr.counter(key) >= c.p().i("max", 10)) {
				return false;
			}
			pr.addCounter(key, 1);
			applyHearts(c.player());
			c.player().heal(100);
			Fx.burst(c.level(), c.player().position().add(0, 1, 0), ParticleTypes.TOTEM_OF_UNDYING, 40, 0.5, 0.4);
			Fx.sound(c.level(), c.player().position(), SoundEvents.TOTEM_USE, 0.5f, 1.6f);
			return true;
		});
		heart.canUse = c -> {
			boolean ok = UsmState.progress().counter(heartKey(c.player())) < c.p().i("max", 10);
			if (!ok) {
				c.player().sendSystemMessage(Component.translatable("ultimatesurvival.msg.hearts_maxed").withStyle(ChatFormatting.GOLD), true);
			}
			return ok;
		};

		// hold still for three seconds, then return to your spawn point (bed or world spawn)
		Abilities.active("recall", 900, c -> {
			ServerPlayer p = c.player();
			Vec3 start = p.position();
			ServerLevel level = c.level();
			int delay = c.p().i("delay", 60);
			Fx.sound(level, start, SoundEvents.PORTAL_TRIGGER, 0.4f, 1.8f);
			for (int i = 1; i <= delay; i += 3) {
				final int t = i;
				Tasks.later(i, () -> {
					if (p.isAlive() && p.position().distanceToSqr(start) < 4) {
						Fx.ring(level, p.position(), 1.4 - 1.2 * t / delay, 12, ParticleTypes.REVERSE_PORTAL, 0.2 + 1.8 * t / delay);
					}
				});
			}
			Tasks.later(delay, () -> {
				if (!p.isAlive() || p.position().distanceToSqr(start) > 4) {
					p.sendSystemMessage(Component.translatable("ultimatesurvival.msg.recall_cancelled").withStyle(ChatFormatting.RED), true);
					return;
				}
				Fx.burst(level, p.position().add(0, 1, 0), ParticleTypes.PORTAL, 60, 0.5, 0.6);
				p.teleport(p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
				Fx.burst(p.level(), p.position().add(0, 1, 0), ParticleTypes.PORTAL, 60, 0.5, 0.6);
				Fx.sound(p.level(), p.position(), SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f);
			});
			p.sendSystemMessage(Component.translatable("ultimatesurvival.msg.recall_start").withStyle(ChatFormatting.AQUA), true);
			return true;
		});
	}

	public static String heartKey(ServerPlayer p) {
		return "hearts_" + p.getUUID();
	}

	/** Re-applies the permanent heart-crystal bonus (idempotent; call on join and now and then). */
	public static void applyHearts(ServerPlayer p) {
		int n = UsmState.progress().counter(heartKey(p));
		AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
		if (inst == null) {
			return;
		}
		var id = UltimateSurvival.id("heart_crystals");
		double want = n * 2.0;
		AttributeModifier cur = inst.getModifier(id);
		if (want <= 0) {
			if (cur != null) {
				inst.removeModifier(id);
			}
		} else if (cur == null || cur.amount() != want) {
			inst.addOrReplacePermanentModifier(new AttributeModifier(id, want, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
