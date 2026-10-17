package com.ultimatesurvival.progress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ultimatesurvival.UltimateSurvival;
import com.ultimatesurvival.content.Content;
import com.ultimatesurvival.content.ItemDef;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Keeps the recipe book honest: recipes of cleared gates are unlocked, locked ones are hidden
 * (the crafting grid itself refuses locked results, see CraftingMenuMixin). Block recipes are never gated.
 */
public final class RecipeSync {
	private RecipeSync() {
	}

	public static void sync(ServerPlayer player) {
		var manager = player.level().getServer().getRecipeManager();
		List<RecipeHolder<?>> award = new ArrayList<>();
		List<RecipeHolder<?>> revoke = new ArrayList<>();
		for (ItemDef def : Content.ITEMS) {
			if (def.json.has("recipe_ids")) {
				collect(manager, def.json, Gates.isCleared(def.gate) ? award : revoke);
			}
		}
		if (Content.root.has("blocks")) {
			for (JsonElement b : Content.root.getAsJsonArray("blocks")) {
				if (b.getAsJsonObject().has("recipe_ids")) {
					collect(manager, b.getAsJsonObject(), award);
				}
			}
		}
		if (!award.isEmpty()) {
			player.awardRecipes(award);
		}
		if (!revoke.isEmpty()) {
			player.resetRecipes(revoke);
		}
	}

	private static void collect(net.minecraft.world.item.crafting.RecipeManager manager, JsonObject o, List<RecipeHolder<?>> into) {
		for (JsonElement id : o.getAsJsonArray("recipe_ids")) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, UltimateSurvival.id(id.getAsString()));
			manager.byKey(key).ifPresent(into::add);
		}
	}
}
