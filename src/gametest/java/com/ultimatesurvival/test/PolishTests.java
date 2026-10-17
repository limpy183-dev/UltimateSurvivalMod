package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.BossTests.immortal;
import static com.ultimatesurvival.test.BossTests.mortal;
import static com.ultimatesurvival.test.BossTests.spawn;
import static com.ultimatesurvival.test.T.check;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.block.UsmBlocks;
import com.ultimatesurvival.client.ClientFlash;
import com.ultimatesurvival.client.ClientShake;
import com.ultimatesurvival.config.UsmConfig;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.mob.BossMusic;
import com.ultimatesurvival.mob.BossRush;
import com.ultimatesurvival.mob.LootBeacon;
import com.ultimatesurvival.mob.MobDef;
import com.ultimatesurvival.mob.Mobs;
import com.ultimatesurvival.mob.UsmBoss;
import com.ultimatesurvival.mob.attack.Styles;
import com.ultimatesurvival.particle.UsmParticles;
import com.ultimatesurvival.progress.Advancements;
import com.ultimatesurvival.progress.Gates;
import com.ultimatesurvival.sound.UsmSounds;
import com.ultimatesurvival.util.ScreenFlash;
import com.ultimatesurvival.util.Shake;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Phase 7 polish: mod sounds + boss music, custom particles, screen shake, armor ambient effects, tomes + trader, advancements, trophies, Boss Rush, shrines. */
final class PolishTests {
	private PolishTests() {
	}

	private static int particles(ClientGameTestContext ctx) {
		String s = ctx.computeOnClient(mc -> mc.particleEngine.countParticles());
		return Integer.parseInt(s.substring(s.lastIndexOf("T ") + 2).trim());
	}

	/** A broken file is ignored, a half-written one is completed, and absurd numbers are clamped to what the game can represent. */
	private static void config() {
		java.nio.file.Path cfg = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(UltimateSurvival.MOD_ID + ".json");
		try {
			String keep = java.nio.file.Files.readString(cfg);
			try {
				java.nio.file.Files.writeString(cfg, "{ this is not json");
				UsmConfig before = UsmConfig.get();
				UsmConfig.load();
				check(UsmConfig.get() == before, "a broken config file leaves the running settings alone instead of crashing");
				java.nio.file.Files.writeString(cfg, "{\"bossHealthMultiplier\": 2.5, \"stackSizeCap\": 5000, \"invasionIntervalSeconds\": -4, \"somethingUnknown\": true}");
				UsmConfig.load();
				UsmConfig c = UsmConfig.get();
				check(c.bossHealthMultiplier == 2.5, "a hand-edited value is read");
				check(c.stackSizeCap == 99 && c.invasionIntervalSeconds == 30, "absurd values are clamped to what the game can hold (" + c.stackSizeCap + ", " + c.invasionIntervalSeconds + ")");
				check(c.bossDamageMultiplier == 1.0 && c.enchantmentMaxLevel == 10, "keys missing from the file keep their defaults");
				String rewritten = java.nio.file.Files.readString(cfg);
				check(rewritten.contains("\"bossDamageMultiplier\"") && !rewritten.contains("somethingUnknown"), "the file is written back complete, without the unknown key");
				java.nio.file.Files.writeString(cfg, "{\"maxParticlesPerEffect\": 96, \"veinMineMaxBlocks\": 48, \"fellMaxLogs\": 192, \"effectIntensity\": 1.0}");
				UsmConfig.load();
				c = UsmConfig.get();
				check(c.maxParticlesPerEffect == 192 && c.veinMineMaxBlocks == 256 && c.fellMaxLogs == 384 && c.configVersion >= 2,
						"a config file from before the power pass gets its new caps once (" + c.maxParticlesPerEffect + ", " + c.veinMineMaxBlocks + ", " + c.fellMaxLogs + ")");
				check(c.effectIntensity == 1.0, "while a value the player chose for a new key is kept");
				java.nio.file.Files.writeString(cfg, "{\"maxParticlesPerEffect\": 64, \"configVersion\": 2}");
				UsmConfig.load();
				check(UsmConfig.get().maxParticlesPerEffect == 64, "and a file already at the current version is never migrated again");
			} finally {
				java.nio.file.Files.writeString(cfg, keep);
				UsmConfig.load();
				UsmConfig.get().vanillaMobScalingPerTier = 0;           // the two switches UsmClientGameTest flips for every suite
				UsmConfig.get().invasionsEnabled = false;
			}
		} catch (java.io.IOException e) {
			throw new AssertionError(e);
		}
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("difficulty normal");
		server.runCommand("usm reset");
		server.runCommand("clear @a");
		server.runCommand("kill @e[type=minecraft:husk]");
		BlockPos me = T.onServer(w, () -> T.player(w).blockPosition());
		double x = me.getX() + 0.5 + 110;
		double y = me.getY();
		double z = me.getZ() + 0.5 - 110;
		T.stand(w, x - 14, y, z, x, y, z);
		ctx.waitTicks(40);
		ServerPlayer p = T.onServer(w, () -> T.player(w));
		String menu = T.onServer(w, () -> p.containerMenu.getClass().getSimpleName());
		UltimateSurvival.LOGGER.info("[gametest] container open at the start of the polish suite: {}", menu);
		T.onServer(w, () -> p.closeContainer());                           // earlier suites drive menus from the server side; an open menu keeps armor changes from reaching the client

		// ---- config: hand-edited nonsense never breaks the game --------------------------------------------------------------------------------------------------------------------------
		config();

		// ---- raised limits that nothing else checks: stack sizes and maximum health ------------------------------------------------------------------------------------------------
		check(new ItemStack(net.minecraft.world.item.Items.DIRT).getMaxStackSize() == 99, "ordinary stackable items hold 99");
		check(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL).getMaxStackSize() == 99, "even those vanilla stops at 16 do");
		check(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD).getMaxStackSize() == 1, "while tools and weapons still do not stack");
		double hugeHealth = T.onServer(w, () -> {
			var attr = T.player(w).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
			double old = attr.getBaseValue();
			attr.setBaseValue(5000);
			double now = T.player(w).getMaxHealth();
			attr.setBaseValue(old);
			return now;
		});
		check(hugeHealth == 5000, "maximum health goes far past vanilla's 1024 (" + hugeHealth + ")");

		// ---- sounds: every mod event exists on both sides; creatures and themes speak through them ------------------------------------------------------------------------------------------
		List<String> ids = new ArrayList<>();
		UsmSounds.ids().forEach(ids::add);
		check(ids.size() >= 70, "the mod registers its own sound events (" + ids.size() + ")");
		check(ids.stream().allMatch(id -> BuiltInRegistries.SOUND_EVENT.containsKey(UltimateSurvival.id(id))), "all of them are in the sound registry");
		check(ctx.computeOnClient(mc -> ids.stream().allMatch(id -> mc.getSoundManager().getSoundEvent(UltimateSurvival.id(id)) != null)), "and defined in sounds.json on the client");
		String silent = ctx.computeOnClient(mc -> {                                                     // an event whose vanilla sound is missing resolves to the empty sound: audible nothing
			var random = net.minecraft.util.RandomSource.create(7);
			StringBuilder b = new StringBuilder();
			for (String id : ids) {
				var event = mc.getSoundManager().getSoundEvent(UltimateSurvival.id(id));
				if (event == null || event.getWeight() <= 0 || event.getSound(random) == net.minecraft.client.sounds.SoundManager.EMPTY_SOUND) {
					b.append(id).append(' ');
				}
			}
			return b.toString();
		});
		check(silent.isEmpty(), "every mod sound event plays a real sound under it (silent: " + silent + ")");
		MobDef nihilus = Mobs.DEFS.get("nihilus");
		check(Mobs.sound(nihilus, "hurt", null) == UsmSounds.get("entity.nihilus.hurt", null), "bosses speak through their own sound events");
		check(Styles.warn("void") == UsmSounds.get("style.void.warn", null) && Styles.impact("radiant") == UsmSounds.get("style.radiant.impact", null), "attack themes have their own telegraph and impact sounds");

		// ---- boss music: the theme plays near a live boss, stops when it dies, and obeys the config --------------------------------------------------------------------------------------------
		UsmBoss ig = spawn(w, "ignarok", x, y, z);
		ctx.waitTicks(40);
		check("music.boss.ember".equals(T.onServer(w, () -> BossMusic.trackOf(p))), "Ignarok fights to its own ember theme");
		T.onServer(w, () -> ig.kill(T.level(w)));
		ctx.waitTicks(10);
		check(T.onServer(w, () -> BossMusic.trackOf(p)) == null, "the music stops when the boss dies");
		server.runCommand("kill @e[type=ultimatesurvival:ignarok]");
		ctx.waitTicks(130);
		check(LootBeacon.live() >= 1, "the fallen Sovereign's treasure bag burns a beacon (" + LootBeacon.live() + ")");
		server.runCommand("kill @e[type=minecraft:item]");
		ctx.waitTicks(15);
		check(LootBeacon.live() == 0, "and the beacon goes out when the bag is gone");
		UsmConfig.get().bossMusic = false;
		UsmBoss quiet = spawn(w, "voltaris", x, y, z);
		ctx.waitTicks(40);
		check(T.onServer(w, () -> BossMusic.trackOf(p)) == null, "bossMusic = false keeps the fight quiet");
		UsmConfig.get().bossMusic = true;
		T.onServer(w, () -> quiet.discard());

		// ---- particles: eleven custom types, each with sprites and a working provider -----------------------------------------------------------------------------------------------------
		check(UsmParticles.TYPES.size() == 11, "eleven signature particles are registered");
		for (String id : UsmParticles.TYPES.keySet()) {
			ctx.waitTicks(30);
			int before = particles(ctx);
			server.runCommand(String.format(java.util.Locale.ROOT, "particle ultimatesurvival:%s %f %f %f 0.5 0.5 0.5 0 24 force @a", id, x - 8, y + 1.5, z));
			ctx.waitTicks(2);
			check(particles(ctx) >= before + 10, id + " particles spawn on the client (" + before + " -> " + particles(ctx) + ")");
		}
		T.stand(w, x - 14, y, z, x, y, z);
		ctx.waitTicks(2);
		server.runCommand(String.format(java.util.Locale.ROOT, "particle ultimatesurvival:void_mote %f %f %f 0.4 0.4 0.4 0 12 force @a", x - 12, y + 1.5, z));
		server.runCommand(String.format(java.util.Locale.ROOT, "particle ultimatesurvival:radiant_spark %f %f %f 0.4 0.4 0.4 0 12 force @a", x - 12.5, y + 1.8, z + 0.5));
		server.runCommand(String.format(java.util.Locale.ROOT, "particle ultimatesurvival:ember_spark %f %f %f 0.4 0.4 0.4 0 12 force @a", x - 12, y + 1.2, z - 0.5));
		ctx.waitTicks(3);
		T.takeShot(ctx, "polish-1-particles");

		// ---- screen shake ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		check(!ctx.computeOnClient(mc -> ClientShake.active()), "the camera is still before an impact");
		T.onServer(w, () -> Shake.at(T.level(w), p.position(), 40, 0.9f, 24));
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> ClientShake.active()), "an impact rattles the client");
		float off = 0;
		for (int i = 0; i < 8; i++) {
			off = Math.max(off, ctx.computeOnClient(mc -> Math.abs(mc.gameRenderer.mainCamera().yRot() - mc.player.getYRot()) + Math.abs(mc.gameRenderer.mainCamera().xRot() - mc.player.getXRot())));
			ctx.waitTicks(1);
		}
		check(off > 0.05f, "and the camera really leaves the player's aim (" + off + " degrees)");
		ctx.waitTicks(30);
		check(!ctx.computeOnClient(mc -> ClientShake.active()), "the shake dies down");
		UsmConfig.get().reducedEffects = true;
		T.onServer(w, () -> Shake.at(T.level(w), p.position(), 40, 0.9f, 24));
		ctx.waitTicks(3);
		check(!ctx.computeOnClient(mc -> ClientShake.active()), "reduced effects switch the shake off");
		UsmConfig.get().reducedEffects = false;

		// ---- screen flash: a brief, never-opaque wash for the biggest moments; off with reduced effects or strength 0 ---------------------------------------------------------------------------
		check(!ctx.computeOnClient(mc -> ClientFlash.active()), "the screen is clear before a big moment");
		T.onServer(w, () -> ScreenFlash.to(p, 0xFFFFFF, 0.9f, 40));
		ctx.waitTicks(4);
		check(ctx.computeOnClient(mc -> ClientFlash.active()), "a big moment washes the screen");
		float wash = ctx.computeOnClient(mc -> ClientFlash.alpha(0f));
		check(wash > 0.15f && wash <= ScreenFlash.MAX_ALPHA + 1e-4f, "the wash is plain to see but never fully opaque (" + wash + ")");
		ctx.takeScreenshot("polish-flash");
		ctx.waitTicks(50);
		check(!ctx.computeOnClient(mc -> ClientFlash.active()), "and it fades away again");
		UsmConfig.get().reducedEffects = true;
		T.onServer(w, () -> ScreenFlash.to(p, 0xFFFFFF, 0.9f, 20));
		ctx.waitTicks(3);
		check(!ctx.computeOnClient(mc -> ClientFlash.active()), "reduced effects switch the flash off");
		UsmConfig.get().reducedEffects = false;
		UsmConfig.get().screenFlashStrength = 0;
		T.onServer(w, () -> ScreenFlash.to(p, 0xFFFFFF, 0.9f, 20));
		ctx.waitTicks(3);
		check(!ctx.computeOnClient(mc -> ClientFlash.active()), "screenFlashStrength = 0 does too");
		UsmConfig.get().screenFlashStrength = 1.0;
		T.onServer(w, () -> ScreenFlash.to(p, 0xFFFFFF, 0.9f, 8));
		ctx.waitTicks(1);
		T.onServer(w, () -> ScreenFlash.to(p, 0xFF0000, 0.9f, 8));                                      // a second one inside the same second is swallowed: never a strobe
		ctx.waitTicks(3);
		check(ctx.computeOnClient(mc -> ClientFlash.active()), "flashes arrive one at a time");
		ctx.waitTicks(20);

		// ---- swing animations: thin blades thrust, heavy ones sweep -------------------------------------------------------------------------------------------------------------------------------------
		var gsSwing = new ItemStack(UsmItems.get("epoch_greatsword")).get(DataComponents.SWING_ANIMATION);
		var daggerSwing = new ItemStack(UsmItems.get("vampire_dagger")).get(DataComponents.SWING_ANIMATION);
		var spearSwing = new ItemStack(UsmItems.get("seraph_spear")).get(DataComponents.SWING_ANIMATION);
		check(gsSwing != null && gsSwing.type() == SwingAnimationType.WHACK && gsSwing.duration() >= 10, "a greatsword takes a long sweeping swing (" + gsSwing + ")");
		check(daggerSwing != null && daggerSwing.type() == SwingAnimationType.STAB && daggerSwing.duration() <= 5, "a dagger stabs quickly (" + daggerSwing + ")");
		check(spearSwing != null && spearSwing.type() == SwingAnimationType.STAB, "a spear thrusts (" + spearSwing + ")");

		// ---- armor ambient effects ---------------------------------------------------------------------------------------------------------------------------------------------------------------
		server.runCommand("usm unlock omnarch");
		ctx.waitTicks(3);
		T.stripArmor(w);
		ctx.waitTicks(40);
		int bare = 0;
		for (int i = 0; i < 10; i++) {
			bare = Math.max(bare, particles(ctx));
			ctx.waitTicks(2);
		}
		T.wear(w, "apex");
		ctx.waitTicks(30);
		int dressed = 0;
		for (int i = 0; i < 10; i++) {
			dressed = Math.max(dressed, particles(ctx));
			ctx.waitTicks(2);
		}
		check(dressed >= bare + 6, "a worn Apex set sheds its own particles (" + bare + " -> " + dressed + ")");
		T.takeShot(ctx, "polish-2-apex-ambient");

		// ---- the eyes and runes of a worn set are a full-bright layer of their own ----------------------------------------------------------------------------------------------------------
		int litBefore = com.ultimatesurvival.client.ArmorGlow.lit;
		server.runCommand("time set midnight");
		ctx.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		boolean glowing;
		try {
			ctx.waitFor(mc -> com.ultimatesurvival.client.ArmorGlow.lit > litBefore, 200);        // frames are only drawn when the window is being presented: give it time
			glowing = true;
		} catch (RuntimeException | AssertionError e) {
			glowing = false;
		}
		String view = ctx.computeOnClient(mc -> "camera " + mc.options.getCameraType() + ", invisible " + mc.player.isInvisible() + ", head " + mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem()
				+ ", screen " + mc.gui.screen() + ", " + mc.getFps() + " fps, minimized " + mc.getWindow().isMinimized() + ", focused " + mc.getWindow().isFocused());
		check(glowing, "a worn set's glow layers are drawn at full brightness (" + (com.ultimatesurvival.client.ArmorGlow.lit - litBefore) + " layers; " + view + ")");
		T.takeShot(ctx, "polish-armor-glow");
		ctx.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		server.runCommand("time set noon");
		T.stripArmor(w);
		for (String set : new String[] {"seraph", "voidwalker"}) {
			T.wear(w, set);
			ctx.waitTicks(20);
			T.stripArmor(w);
		}
		server.runCommand("usm reset");

		// ---- tomes: levels vanilla never gives, from loot and from the wandering trader ------------------------------------------------------------------------------------------------------
		ItemStack book = Mobs.stack("book:sharpness:8", 1);
		var stored = book.get(DataComponents.STORED_ENCHANTMENTS);
		check(stored != null && stored.entrySet().iterator().next().getIntValue() == 8 && stored.entrySet().iterator().next().getKey().is(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),
				"a book:sharpness:8 row makes an enchanted book of Sharpness VIII");
		server.runCommand("usm unlock hemorath");
		ctx.waitTicks(3);
		server.runCommand("clear @a");
		T.onServer(w, () -> p.getInventory().add(new ItemStack(UsmItems.get("omnarch_bag"), 8)));
		for (int i = 0; i < 8; i++) {
			T.hold(w, "omnarch_bag");
			AbilityTests.use(w);
			ctx.waitTicks(1);
		}
		int tomes = T.onServer(w, () -> {
			int n = 0;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				var st = p.getInventory().getItem(i).get(DataComponents.STORED_ENCHANTMENTS);
				if (st != null && st.entrySet().iterator().next().getIntValue() == 10) {
					n++;
				}
			}
			return n;
		});
		check(tomes >= 2, "the Omnarch's hoards hold level-ten tomes (" + tomes + " found in eight bags)");
		ctx.waitTicks(30);
		check(T.onServer(w, () -> Advancements.has(p, "tome")), "holding one earns the Beyond the Table advancement");
		check(T.count(w, "trophy_omnarch") == 8, "every hoard also holds its trophy (" + T.count(w, "trophy_omnarch") + ")");
		boolean traded = T.onServer(w, () -> {
			var reg = T.level(w).registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
			var key = ResourceKey.create(Registries.VILLAGER_TRADE, UltimateSurvival.id("tome_sharpness_7"));
			return reg.get(key).isPresent() && reg.get(VillagerTradeTags.WANDERING_TRADER_UNCOMMON).map(set -> set.stream().anyMatch(h -> h.is(key))).orElse(false);
		});
		check(traded, "the wandering trader's uncommon stock includes the Sharpness VII tome");
		int tomeOffers = T.onServer(w, () -> {                                                       // and traders really do offer them: roll the stock of 120 of them
			int n = 0;
			for (int i = 0; i < 120; i++) {
				var trader = EntityTypes.WANDERING_TRADER.create(T.level(w), EntitySpawnReason.COMMAND);
				trader.snapTo(x, y, z, 0f, 0f);
				for (var offer : trader.getOffers()) {
					if (offer.getResult().get(DataComponents.STORED_ENCHANTMENTS) != null) {
						n++;
					}
				}
			}
			return n;
		});
		check(tomeOffers >= 1, "wandering traders really offer the tomes (" + tomeOffers + " enchanted-book offers across 120 traders)");
		server.runCommand("clear @a");

		// ---- advancements --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		for (String id : Content.root.getAsJsonArray("advancements").asList().stream().map(e -> e.getAsString()).toList()) {
			check(T.onServer(w, () -> T.level(w).getServer().getAdvancements().get(UltimateSurvival.id(id))) != null, "advancement " + id + " is loaded");
		}
		ctx.waitTicks(5);
		check(T.onServer(w, () -> Advancements.has(p, "root")), "the root advancement is granted on the first tick");
		server.runCommand("advancement revoke @a only ultimatesurvival:voltaris");           // earlier suites really killed Voltaris
		ctx.waitTicks(3);
		server.runCommand("usm unlock voltaris");
		ctx.waitTicks(3);
		check(!T.onServer(w, () -> Advancements.has(p, "voltaris")), "cheating a gate open with /usm does not grant the boss advancement");
		server.runCommand("usm reset");
		ctx.waitTicks(3);
		T.onServer(w, () -> Gates.defeat(T.level(w).getServer(), Content.gate("voltaris")));
		ctx.waitTicks(3);
		check(T.onServer(w, () -> Advancements.has(p, "voltaris")), "defeating Voltaris grants Ride the Lightning");
		server.runCommand("usm reset");

		// ---- trophies ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		for (String gate : new String[] {"ender_dragon", "ignarok", "voltaris", "hemorath", "chronos", "nihilus", "solareon", "omnarch", "boreus", "morbane", "astraeus", "eternus"}) {
			check(UsmBlocks.BY_ID.containsKey("trophy_" + gate), "a trophy block exists for " + gate);
		}
		BlockPos tp = BlockPos.containing(x - 20, y, z + 6);
		T.onServer(w, () -> T.level(w).setBlockAndUpdate(tp, UsmBlocks.BY_ID.get("trophy_solareon").defaultBlockState()));
		var drops = T.onServer(w, () -> Block.getDrops(T.level(w).getBlockState(tp), T.level(w), tp, null));
		check(drops.size() == 1 && drops.get(0).getItem() == UsmBlocks.ITEMS.get("trophy_solareon"), "a trophy drops itself when broken");
		check(T.onServer(w, () -> T.level(w).getBlockState(tp).getLightEmission()) >= 6, "the Solareon trophy glows");
		T.stand(w, x - 24, y, z + 6, x - 20, y - 0.4, z + 6);
		ctx.waitTicks(10);
		T.takeShot(ctx, "polish-3-trophy");
		T.onServer(w, () -> T.level(w).removeBlock(tp, false));

		// ---- a sovereign shrine --------------------------------------------------------------------------------------------------------------------------------------------------------------------
		boolean wiredOverworld = T.onServer(w, () -> {
			var biomes = T.level(w).registryAccess().lookupOrThrow(Registries.BIOME);
			var key = ResourceKey.create(Registries.PLACED_FEATURE, UltimateSurvival.id("sovereign_shrine"));
			return biomes.listElements().anyMatch(b -> b.key().identifier().getPath().equals("plains") && b.value().getGenerationSettings().features().stream().anyMatch(s -> s.stream().anyMatch(f -> f.is(key))))
					&& biomes.listElements().anyMatch(b -> b.key().identifier().getPath().equals("end_highlands") && b.value().getGenerationSettings().features().stream().anyMatch(s -> s.stream().anyMatch(f -> f.is(key))));
		});
		check(wiredOverworld, "shrines are wired into the Overworld and the outer End islands");
		BlockPos spot = BlockPos.containing(x + 30, y, z + 30);
		T.stand(w, spot.getX() + 0.5, y, spot.getZ() + 7.5, spot.getX() + 0.5, y + 1, spot.getZ() + 0.5);
		ctx.waitTicks(40);
		boolean placed = T.onServer(w, () -> {
			var lvl = T.level(w);
			var cf = lvl.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, UltimateSurvival.id("sovereign_shrine"))).value();
			return cf.place(lvl, lvl.getChunkSource().getGenerator(), lvl.getRandom(), spot);
		});
		check(placed, "a shrine can be built on flat ground");
		check(T.onServer(w, () -> T.level(w).getBlockState(spot).getBlock() == UsmBlocks.BY_ID.get("ritual_altar")), "with a Ritual Altar at its heart");
		ChestBlockEntity chest = T.onServer(w, () -> {
			for (BlockPos c : new BlockPos[] {spot.offset(0, 0, 3), spot.offset(0, 0, -3)}) {
				if (T.level(w).getBlockEntity(c) instanceof ChestBlockEntity ce) {
					return ce;
				}
			}
			return null;
		});
		check(chest != null, "and a chest beside it");
		int loot = T.onServer(w, () -> {
			chest.unpackLootTable(p);
			int n = 0;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				n += chest.getItem(i).isEmpty() ? 0 : 1;
			}
			return n;
		});
		check(loot >= 4, "whose loot table fills at least four slots (" + loot + ")");
		T.stand(w, spot.getX() + 0.5, y, spot.getZ() + 10.5, spot.getX() + 0.5, y + 1.5, spot.getZ() + 0.5);
		ctx.waitTicks(12);
		T.takeShot(ctx, "polish-4-shrine");

		// ---- Boss Rush ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
		T.stand(w, x - 14, y, z, x, y, z);
		immortal(w);
		server.runCommand("usm reset");
		ctx.waitTicks(10);
		check(T.onServer(w, () -> BossRush.start(p)), "a Boss Rush can be started");
		check(!T.onServer(w, () -> BossRush.start(p)) && T.onServer(w, () -> BossRush.active(p)), "but only one at a time");
		T.onServer(w, () -> BossRush.hurry(p));
		ctx.waitTicks(4);
		UsmBoss first = T.onServer(w, () -> BossRush.current(p));
		check(first != null && first.def().id.equals("ignarok"), "the first Sovereign is Ignarok");
		T.takeShot(ctx, "polish-5-rush");
		T.onServer(w, () -> BossRush.stop(T.level(w).getServer(), p.getUUID(), null));
		ctx.waitTicks(3);
		check(!T.onServer(w, () -> BossRush.active(p)) && !T.onServer(w, () -> first.isAlive()), "stopping the rush removes the Sovereign");
		check(T.onServer(w, () -> BossRush.start(p)), "and a new rush can begin");
		for (int i = 0; i < BossRush.ORDER.size(); i++) {
			final int stage = i;
			T.onServer(w, () -> BossRush.hurry(p));
			ctx.waitTicks(4);
			UsmBoss b = T.onServer(w, () -> BossRush.current(p));
			check(b != null && b.def().id.equals(BossRush.ORDER.get(stage)), "stage " + (i + 1) + " calls " + BossRush.ORDER.get(i));
			T.onServer(w, () -> b.kill(T.level(w)));
			ctx.waitTicks(6);
		}
		T.onServer(w, () -> BossRush.hurry(p));
		ctx.waitTicks(6);
		check(!T.onServer(w, () -> BossRush.active(p)), "the rush is over after the eleventh Sovereign");
		check(T.onServer(w, () -> Advancements.has(p, "boss_rush")), "Gauntlet is earned");
		check(T.count(w, "omni_core") >= 2, "and the reward is paid (" + T.count(w, "omni_core") + " Omni Cores)");
		check(Gates.isCleared("eternus") && Gates.currentTier() == 6, "the kills count as real progression");
		ctx.waitTicks(180);
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("kill @e[type=minecraft:experience_orb]");
		mortal(w);
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		Vec3 done = T.onServer(w, () -> p.position());
		check(done != null, "polish suite finished");
	}
}
