package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class CraftingContainerRecipeTransferHandlerServer {
	private CraftingContainerRecipeTransferHandlerServer() {}

	/**
	 * Called server-side to actually put the items in place.
	 */
	public static void setItems(Player player, Map<Integer, Integer> slotIdMap, List<Integer> craftingSlots, List<Integer> inventorySlots, boolean maxTransfer) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> container)) {
			return;
		}

		// grab items from slots
		Map<Integer, ItemStack> slotMap = new HashMap<>(slotIdMap.size());
		for (Map.Entry<Integer, Integer> entry : slotIdMap.entrySet()) {
			Slot slot = container.getSlot(entry.getValue());
			final ItemStack slotStack = slot.getItem();
			if (slotStack.isEmpty()) {
				return;
			}
			ItemStack stack = slotStack.copy();
			stack.setCount(1);
			slotMap.put(entry.getKey(), stack);
		}

		Map<Integer, ItemStack> toTransfer = removeItemsFromInventory(player, container, slotMap, craftingSlots, inventorySlots, maxTransfer);

		if (toTransfer.isEmpty()) {
			return;
		}

		// clear the crafting grid
		List<ItemStack> clearedCraftingItems = clearAndPutItemsIntoGrid(player, craftingSlots, container, toTransfer);

		putIntoInventory(player, inventorySlots, container, clearedCraftingItems);

		container.sendSlotUpdates();
		container.broadcastChanges();
	}

	private static void putIntoInventory(Player player, List<Integer> inventorySlots, StorageContainerMenuBase<?> container, List<ItemStack> clearedCraftingItems) {
		for (ItemStack oldCraftingItem : clearedCraftingItems) {
			int added = addStack(container, inventorySlots, oldCraftingItem);
			if (added < oldCraftingItem.getCount() && !player.getInventory().add(oldCraftingItem)) {
				player.drop(oldCraftingItem, false);
			}
		}
	}

	private static List<ItemStack> clearAndPutItemsIntoGrid(Player player, List<Integer> craftingSlots, AbstractContainerMenu container, Map<Integer, ItemStack> toTransfer) {
		List<ItemStack> clearedCraftingItems = new ArrayList<>();
		int minSlotStackLimit = Integer.MAX_VALUE;
		for (int craftingSlotNumberIndex = 0; craftingSlotNumberIndex < craftingSlots.size(); craftingSlotNumberIndex++) {
			int craftingSlotNumber = craftingSlots.get(craftingSlotNumberIndex);
			Slot craftingSlot = container.getSlot(craftingSlotNumber);
			if (!craftingSlot.mayPickup(player)) {
				continue;
			}
			if (craftingSlot.hasItem()) {
				ItemStack craftingItem = craftingSlot.remove(Integer.MAX_VALUE);
				clearedCraftingItems.add(craftingItem);
			}
			ItemStack transferItem = toTransfer.get(craftingSlotNumberIndex);
			if (transferItem != null) {
				int slotStackLimit = craftingSlot.getMaxStackSize(transferItem);
				minSlotStackLimit = Math.min(slotStackLimit, minSlotStackLimit);
			}
		}

		// put items into the crafting grid
		putItemIntoGrid(container, toTransfer, clearedCraftingItems, minSlotStackLimit);
		return clearedCraftingItems;
	}

	private static void putItemIntoGrid(AbstractContainerMenu container, Map<Integer, ItemStack> toTransfer, List<ItemStack> clearedCraftingItems, int minSlotStackLimit) {
		for (Map.Entry<Integer, ItemStack> entry : toTransfer.entrySet()) {
			Integer craftingSlotIndex = entry.getKey();
			Slot slot = container.getSlot(craftingSlotIndex);

			ItemStack stack = entry.getValue();
			if (slot.mayPlace(stack)) {
				if (stack.getCount() > minSlotStackLimit) {
					ItemStack remainder = stack.split(stack.getCount() - minSlotStackLimit);
					clearedCraftingItems.add(remainder);
				}
				slot.set(stack);
			} else {
				clearedCraftingItems.add(stack);
			}
		}
	}

	private static Map<Integer, ItemStack> removeItemsFromInventory(
			Player player,
			StorageContainerMenuBase<?> container,
			Map<Integer, ItemStack> required,
			List<Integer> craftingSlots,
			List<Integer> inventorySlots,
			boolean maxTransfer
	) {

		// This map becomes populated with the resulting items to transfer and is returned by this method.
		final Map<Integer, ItemStack> result = new HashMap<>(required.size());

		loopSets:
		while (true) { // for each set

			// This map holds the original contents of a slot we have removed items from. This is used if we don't
			// have enough items to complete a whole set, we can roll back the items that were removed.
			Map<Slot, ItemStack> originalSlotContents = new HashMap<>();

			// This map holds items found for each set iteration. Its contents are added to the result map
			// after each complete set iteration. If we are transferring as complete sets, this allows
			// us to simply ignore the map's contents when a complete set isn't found.
			final Map<Integer, ItemStack> foundItemsInSet = new HashMap<>(required.size());

			// This flag is set to false if at least one item is found during the set iteration. It is used
			// to determine if iteration should continue and prevents an infinite loop if not transferring
			// as complete sets.
			boolean noItemsFound = true;

			for (Map.Entry<Integer, ItemStack> entry : required.entrySet()) { // for each item in set
				final ItemStack requiredStack = entry.getValue().copy();

				// Locate a slot that has what we need.
				final Slot slot = getSlotWithStack(container, requiredStack, craftingSlots, inventorySlots);

				boolean itemFound = (slot != null) && !slot.getItem().isEmpty() && slot.mayPickup(player);
				ItemStack resultItemStack = result.get(entry.getKey());
				boolean resultItemStackLimitReached = (resultItemStack != null) && (resultItemStack.getCount() == resultItemStack.getMaxStackSize());

				if (!itemFound || resultItemStackLimitReached) {
					// We can't find any more items to fulfill the requirements or the maximum stack size for this item
					// has been reached.

					// Since the full set requirement wasn't satisfied, we need to roll back any
					// slot changes we've made during this set iteration.
					for (Map.Entry<Slot, ItemStack> slotEntry : originalSlotContents.entrySet()) {
						ItemStack stack = slotEntry.getValue();
						slotEntry.getKey().set(stack);
					}
					break loopSets;

				} else { // the item was found and the stack limit has not been reached

					// Keep a copy of the slot's original contents in case we need to roll back.
					if (!originalSlotContents.containsKey(slot)) {
						originalSlotContents.put(slot, slot.getItem().copy());
					}

					// Reduce the size of the found slot.
					ItemStack removedItemStack = slot.remove(1);
					foundItemsInSet.put(entry.getKey(), removedItemStack);

					noItemsFound = false;
				}
			}

			// Merge the contents of the temporary map with the result map.
			for (Map.Entry<Integer, ItemStack> entry : foundItemsInSet.entrySet()) {
				ItemStack resultItemStack = result.get(entry.getKey());

				if (resultItemStack == null) {
					result.put(entry.getKey(), entry.getValue());

				} else {
					resultItemStack.grow(1);
				}
			}

			if (!maxTransfer || noItemsFound) {
				// If max transfer is not requested by the player this will exit the loop after trying one set.
				// If no items were found during this iteration, we're done.
				break;
			}
		}

		return result;
	}

	@Nullable
	private static Slot getSlotWithStack(StorageContainerMenuBase<?> container, ItemStack stack, List<Integer> craftingSlots, List<Integer> inventorySlots) {
		Slot slot = getSlotWithStack(container, craftingSlots, stack);
		if (slot == null) {
			slot = getSlotWithStack(container, inventorySlots, stack);
		}

		return slot;
	}

	private static int addStack(StorageContainerMenuBase<?> container, Collection<Integer> slotIndexes, ItemStack stack) {
		int added = 0;
		// Add to existing stacks first
		for (final Integer slotIndex : slotIndexes) {
			if (slotIndex >= 0 && slotIndex < getTotalSlotsSize(container)) {
				final Slot slot = container.getSlot(slotIndex);
				final ItemStack inventoryStack = slot.getItem();
				// Check that the slot's contents are stackable with this stack
				if (!inventoryStack.isEmpty() &&
						inventoryStack.isStackable() &&
						ItemStack.isSameItemSameTags(inventoryStack, stack)) {
					final int remain = stack.getCount() - added;
					final int maxStackSize = slot.getMaxStackSize(inventoryStack);
					final int space = maxStackSize - inventoryStack.getCount();
					if (space > 0) {

						// Enough space
						if (space >= remain) {
							inventoryStack.grow(remain);
							return stack.getCount();
						}

						// Not enough space
						inventoryStack.setCount(maxStackSize);

						added += space;
					}
				}
			}
		}

		if (added >= stack.getCount()) {
			return added;
		}

		for (final Integer slotIndex : slotIndexes) {
			if (slotIndex >= 0 && slotIndex < getTotalSlotsSize(container)) {
				final Slot slot = container.getSlot(slotIndex);
				final ItemStack inventoryStack = slot.getItem();
				if (inventoryStack.isEmpty()) {
					ItemStack stackToAdd = stack.copy();
					stackToAdd.setCount(stack.getCount() - added);
					slot.set(stackToAdd);
					return stack.getCount();
				}
			}
		}

		return added;
	}

	/**
	 * Get the slot which contains a specific itemStack.
	 *
	 * @param container   the container to search
	 * @param slotNumbers the slots in the container to search
	 * @param itemStack   the itemStack to find
	 * @return the slot that contains the itemStack. returns null if no slot contains the itemStack.
	 */
	@Nullable
	private static Slot getSlotWithStack(StorageContainerMenuBase<?> container, Iterable<Integer> slotNumbers, ItemStack itemStack) {
		for (Integer slotNumber : slotNumbers) {
			if (slotNumber >= 0 && slotNumber < getTotalSlotsSize(container)) {
				Slot slot = container.getSlot(slotNumber);
				ItemStack slotStack = slot.getItem();
				if (ItemStack.isSameItemSameTags(itemStack, slotStack)) {
					return slot;
				}
			}
		}
		return null;
	}

	private static int getTotalSlotsSize(StorageContainerMenuBase<?> container) {
		return container.upgradeSlots.size() + container.realInventorySlots.size();
	}
}
