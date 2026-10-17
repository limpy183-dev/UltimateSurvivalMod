package com.ultimatesurvival.test;

import static com.ultimatesurvival.test.T.check;
import static com.ultimatesurvival.test.T.near;

import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.item.UsmItems;
import com.ultimatesurvival.progress.Gates;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Raised enchant limits, the anvil, recipe locking, the dragon gate + loot, consumables, ore worldgen. */
final class RulesTests {
	private RulesTests() {
	}

	static void run(ClientGameTestContext ctx, TestSingleplayerContext w) {
		var server = w.getServer();
		server.runCommand("gamemode survival @a");
		server.runCommand("clear @a");
		server.runCommand("usm reset");
		ctx.waitTicks(2);

		// ---- raised enchantment limits -----------------------------------------------------------------------------
		var enchants = T.onServer(w, () -> T.level(w).registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
		int sharp = T.onServer(w, () -> enchants.getOrThrow(Enchantments.SHARPNESS).value().getMaxLevel());
		int mending = T.onServer(w, () -> enchants.getOrThrow(Enchantments.MENDING).value().getMaxLevel());
		check(sharp == 10, "Sharpness max level raised to 10 (is " + sharp + ")");
		check(mending == 1, "single-level enchantments stay single-level (Mending " + mending + ")");
		server.runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
		server.runCommand("enchant @a minecraft:sharpness 10");
		ItemStack enchanted = T.onServer(w, () -> T.player(w).getMainHandItem());
		int lvl = T.onServer(w, () -> EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SHARPNESS), enchanted));
		check(lvl == 10, "/enchant accepts Sharpness 10 (got " + lvl + ")");

		// ---- anvil: no 'Too Expensive!' ---------------------------------------------------------------------------------
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.REPAIR_COST, 100000);
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		T.onServer(w, () -> {
			ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
			m.set(enchants.getOrThrow(Enchantments.SHARPNESS), 6);
			book.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
		});
		ItemStack result = T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			AnvilMenu menu = new AnvilMenu(77, p.getInventory(), ContainerLevelAccess.create(T.level(w), BlockPos.ZERO));
			menu.getSlot(0).set(sword);
			menu.getSlot(1).set(book);
			menu.slotsChanged(p.getInventory());
			return menu.getSlot(2).getItem().copy();
		});
		check(!result.isEmpty(), "anvil produces a result even at a repair cost far above 40");

		// ---- recipe locking in the real crafting grid ------------------------------------------------------------------
		String[] pattern = {"azurite_ingot", "azurite_ingot", "azurite_ingot", "", "drakescale", "", "", "minecraft:stick", ""};
		check(craft(w, pattern).isEmpty(), "locked tier-2 recipe produces nothing in the crafting grid");
		server.runCommand("usm unlock ender_dragon");
		ctx.waitTicks(2);
		ItemStack made = craft(w, pattern);
		check(made.getItem() == UsmItems.get("azurite_pickaxe"), "after the dragon gate the same grid produces the azurite pickaxe");
		check(craft(w, new String[] {"verdite_ingot", "", "", "verdite_ingot", "", "", "minecraft:stick", "", ""}).getItem() == UsmItems.get("verdite_sword"),
				"tier-1 recipes always work");

		// ---- recipe book follows the gates ---------------------------------------------------------------------------------
		ResourceKey<Recipe<?>> t2 = ResourceKey.create(Registries.RECIPE, UltimateSurvival.id("azurite_pickaxe"));
		ResourceKey<Recipe<?>> t3 = ResourceKey.create(Registries.RECIPE, UltimateSurvival.id("verdite_pickaxe"));
		check(T.onServer(w, () -> T.player(w).getRecipeBook().contains(t2)), "tier-2 recipe is in the recipe book once unlocked");
		check(T.onServer(w, () -> T.player(w).getRecipeBook().contains(t3)), "tier-1 recipe is in the recipe book");
		server.runCommand("usm reset");
		ctx.waitTicks(2);
		check(!T.onServer(w, () -> T.player(w).getRecipeBook().contains(t2)), "reset hides the tier-2 recipe again");

		// ---- the Ender Dragon is the tier-2 gate; it also drops drakescale ---------------------------------------------------
		BlockPos at0 = T.onServer(w, () -> T.player(w).blockPosition());
		EnderDragon dragon = T.onServer(w, () -> {
			EnderDragon d = new EnderDragon(EntityTypes.ENDER_DRAGON, T.level(w));
			d.snapTo(at0.getX() + 20, at0.getY() + 10, at0.getZ(), 0, 0);
			T.level(w).addFreshEntity(d);
			return d;
		});
		check(!Gates.isCleared("ender_dragon"), "gate closed before the dragon dies");
		T.onServer(w, () -> dragon.setHealth(0f));          // what the dying phase does at the portal
		ctx.waitTicks(10);
		check(Gates.isCleared("ender_dragon"), "the dragon's death animation opens tier 2");
		check(Gates.currentTier() == 2, "the current tier is now 2");
		ctx.waitTicks(205);
		int scales = T.onServer(w, () -> T.level(w).getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(at0).inflate(60)).stream().filter(e -> e.getItem().is(UsmItems.get("drakescale"))).mapToInt(e -> e.getItem().getCount()).sum());
		check(scales >= 4 && scales <= 8, "the dying dragon dropped 4-8 drakescale (" + scales + ")");
		check(T.onServer(w, () -> dragon.isRemoved()), "and the dragon was removed after its death animation");
		T.takeShot(ctx, "phase2-dragon-loot");

		// ---- consumables --------------------------------------------------------------------------------------------------
		server.runCommand("effect clear @a");
		T.onServer(w, () -> T.player(w).setHealth(4f));
		ItemStack salve = new ItemStack(UsmItems.get("healing_salve"));
		T.onServer(w, () -> salve.finishUsingItem(T.level(w), T.player(w)));
		check(T.onServer(w, () -> T.player(w).getHealth()) >= 12f, "Healing Salve restores 8 health");

		ItemStack crystal = new ItemStack(UsmItems.get("heart_crystal"), 8);
		for (int i = 0; i < 5; i++) {
			T.onServer(w, () -> crystal.finishUsingItem(T.level(w), T.player(w)));
		}
		near(T.onServer(w, () -> T.player(w).getMaxHealth()), 30.0, 1e-6, "five Heart Crystals add five hearts");
		T.onServer(w, () -> T.player(w).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crystal));
		boolean sixth = T.onServer(w, () -> crystal.use(T.level(w), T.player(w), net.minecraft.world.InteractionHand.MAIN_HAND) instanceof net.minecraft.world.InteractionResult.Success);
		check(!sixth, "the sixth Heart Crystal is refused");
		near(T.onServer(w, () -> T.player(w).getMaxHealth()), 30.0, 1e-6, "health stays capped at +5 hearts");

		// ---- world generation: ores are wired into the overworld biomes ---------------------------------------------------------
		boolean wired = T.onServer(w, () -> {
			var biomes = T.level(w).registryAccess().lookupOrThrow(Registries.BIOME);
			var key = ResourceKey.create(Registries.PLACED_FEATURE, UltimateSurvival.id("ore_verdite"));
			return biomes.listElements().anyMatch(b -> b.value().getGenerationSettings().features().stream()
					.anyMatch(step -> step.stream().anyMatch(f -> f.is(key))));
		});
		check(wired, "verdite ore generates in the overworld (placed feature attached to biomes)");
	}

	/** Puts the 9 ingredient ids into a crafting table menu and returns what the result slot shows. */
	private static ItemStack craft(TestSingleplayerContext w, String[] ids) {
		return T.onServer(w, () -> {
			ServerPlayer p = T.player(w);
			CraftingMenu menu = new CraftingMenu(88, p.getInventory(), ContainerLevelAccess.create(T.level(w), BlockPos.ZERO));
			for (int i = 0; i < 9; i++) {
				if (!ids[i].isEmpty()) {
					var item = ids[i].contains(":") ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(ids[i])) : UsmItems.get(ids[i]);
					menu.getSlot(1 + i).set(new ItemStack(item));
				}
			}
			menu.slotsChanged(p.getInventory());
			ItemStack out = menu.getSlot(0).getItem().copy();
			return out;
		});
	}
}
