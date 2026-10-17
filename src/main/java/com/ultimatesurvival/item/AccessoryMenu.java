package com.ultimatesurvival.item;

import com.ultimatesurvival.progress.Gates;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Six accessory slots (more with the Creative cheat) above the normal inventory. Only items of kind "accessory" fit. */
public class AccessoryMenu extends AbstractContainerMenu {
	/** Slots in the original panel: two rows of three above the inventory. */
	public static final int BASE = com.ultimatesurvival.progress.Progress.ACCESSORY_SLOTS;
	/** Where the extra slots (the Creative cheat) start, and how many fit in a row. */
	public static final int EXTRA_Y = 81;
	public static final int PER_ROW = 9;
	private final Container accessories;
	public final int slotCount;

	/** Height added to the panel by the extra slot rows (0 with the normal six). */
	public static int extraHeight(int count) {
		return count <= BASE ? 0 : ((count - BASE + PER_ROW - 1) / PER_ROW) * 18 + 12;      // 12: room for the "Inventory" label under the extra rows
	}

	public AccessoryMenu(int id, Inventory inv, Container accessories, int count) {
		super(Accessories.MENU, id);
		this.accessories = accessories;
		this.slotCount = count;
		int extra = extraHeight(count);
		for (int i = 0; i < count; i++) {
			int sx = i < BASE ? 62 + (i % 3) * 18 : 8 + ((i - BASE) % PER_ROW) * 18;
			int sy = i < BASE ? 26 + (i / 3) * 18 : EXTRA_Y + 1 + ((i - BASE) / PER_ROW) * 18;
			addSlot(new Slot(accessories, i, sx, sy) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return Accessories.isAccessory(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + extra + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inv, col, 8 + col * 18, 142 + extra));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			if (index < slotCount) {
				if (!moveItemStackTo(stack, slotCount, slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (Accessories.isAccessory(stack)) {
				if (!moveItemStackTo(stack, 0, slotCount, false)) {
					return ItemStack.EMPTY;
				}
			} else if (index < slotCount + 27) {
				if (!moveItemStackTo(stack, slotCount + 27, slots.size(), false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(stack, slotCount, slotCount + 27, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	static boolean unlocked(ItemStack s) {
		return !Gates.isLocked(s);
	}
}
