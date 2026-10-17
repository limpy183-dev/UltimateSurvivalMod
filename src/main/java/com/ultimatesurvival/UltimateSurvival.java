package com.ultimatesurvival;

import com.ultimatesurvival.ability.Abilities;
import com.ultimatesurvival.ability.ConsumableAbilitiesAccess;
import com.ultimatesurvival.armor.ArmorSets;
import com.ultimatesurvival.armor.Wearer;
import com.ultimatesurvival.armor.WearerAbilities;
import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.codex.Codex;
import com.ultimatesurvival.codex.Discovery;
import com.ultimatesurvival.command.UsmCommands;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.Gate;
import com.ultimatesurvival.entity.UsmEntities;
import com.ultimatesurvival.item.Accessories;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.item.UsmWeaponItem;
import com.ultimatesurvival.mob.Invasions;
import com.ultimatesurvival.mob.MobScaling;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.Ritual;
import com.ultimatesurvival.net.Payloads;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.progress.RecipeSync;
import com.ultimatesurvival.progress.UsmState;
import com.ultimatesurvival.util.PosHistory;
import com.ultimatesurvival.util.Tasks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimateSurvival implements ModInitializer {
	public static final String MOD_ID = "ultimatesurvival";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UsmConfig.load();
		if (UsmConfig.get().maxHealthCap > 1024) {
			((com.ultimatesurvival.mixin.RangedAttributeAccessor) (net.minecraft.world.entity.ai.attributes.RangedAttribute)
					net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()).usm$setMaxValue(UsmConfig.get().maxHealthCap);
		}
		// vanilla clamps armor at 30 and toughness at 20; the late-game sets exceed both
		for (var attr : java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) {
			((com.ultimatesurvival.mixin.RangedAttributeAccessor) (net.minecraft.world.entity.ai.attributes.RangedAttribute) attr.value()).usm$setMaxValue(1024);
		}
		Content.load();
		com.ultimatesurvival.sound.UsmSounds.register();
		com.ultimatesurvival.particle.UsmParticles.register();
		Abilities.bootstrap();
		UsmEntities.init();
		Mobs.register();
		UsmBlocks.register();
		net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.FEATURE, id("sovereign_shrine"), new com.ultimatesurvival.world.ShrineFeature());
		var shrine = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, id("sovereign_shrine"));
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES, shrine);
		net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInTheEnd(),
				net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES, shrine);
		UsmItems.register();
		Mobs.validate();
		Invasions.load();
		ArmorSets.load();
		Accessories.registerMenu();
		Payloads.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> UsmCommands.register(dispatcher));
		ServerLifecycleEvents.SERVER_STARTED.register(UsmState::attach);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(MobScaling::apply);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			UsmState.detach();
			Tasks.clear();
			Ritual.clear();
			PosHistory.clear();
			com.ultimatesurvival.util.LightZones.clear();
			com.ultimatesurvival.util.Shake.clear();
			com.ultimatesurvival.util.ScreenFlash.clear();
			com.ultimatesurvival.mob.LootBeacon.clear();
			com.ultimatesurvival.mob.BossMusic.clear();
			com.ultimatesurvival.mob.BossRush.clear();
			Invasions.clear();
			Wearer.clearAll();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Tasks.tick();
			Invasions.tick(server);
			com.ultimatesurvival.mob.BossRush.tick(server);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				Wearer.tick(p);
				PosHistory.record(p);
				Discovery.tick(p);
				if (p.tickCount % 10 == 0) {
					Accessories.tickAttributes(p);
				}
				if (p.tickCount % 100 == 1) {
					ConsumableAbilitiesAccess.applyHearts(p);
				}
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ConsumableAbilitiesAccess.applyHearts(handler.getPlayer());
			RecipeSync.sync(handler.getPlayer());
			Codex.giveOnFirstJoin(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Wearer.forget(handler.getPlayer().getUUID());
			PosHistory.forget(handler.getPlayer().getUUID());
			Accessories.forget(handler.getPlayer().getUUID());
			WearerAbilities.forget(handler.getPlayer().getUUID());
		});
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, entity, killed, source) -> {
			if (entity instanceof ServerPlayer p && p.getMainHandItem().getItem() instanceof UsmWeaponItem w) {
				w.killed(p, p.getMainHandItem(), killed);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) {
				Gate gate = Gates.gateForBoss(entity);
				if (gate != null) {
					Gates.defeat(level.getServer(), gate);
				}
			}
		});
		LOGGER.info("Ultimate Survival ready: {} gates, {} items, {} blocks", Content.GATES.size(), UsmItems.BY_ID.size(), UsmBlocks.BY_ID.size());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
