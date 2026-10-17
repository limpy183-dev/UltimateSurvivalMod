package com.ultimatesurvival.mixin;

import com.ultimatesurvival.progress.Gates;
import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A recipe whose result belongs to an uncleared gate produces nothing in the crafting grid (2x2 and 3x3). */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
	private static void usm$lockedRecipes(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
			ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> hint, CallbackInfo ci) {
		CraftingInput input = container.asCraftInput();
		Optional<RecipeHolder<CraftingRecipe>> found = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, hint);
		if (found.isPresent() && Gates.isLocked(found.get().value().assemble(input))) {
			resultSlots.setItem(0, ItemStack.EMPTY);
			menu.setRemoteSlot(0, ItemStack.EMPTY);
			((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
			ci.cancel();
		}
	}
}
