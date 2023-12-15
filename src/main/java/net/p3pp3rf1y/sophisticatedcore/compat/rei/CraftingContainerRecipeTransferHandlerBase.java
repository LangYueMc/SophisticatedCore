package net.p3pp3rf1y.sophisticatedcore.compat.rei;/*
package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import mezz.jei.common.util.StringUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public abstract class CraftingContainerRecipeTransferHandlerBase implements TransferHandler {
	protected CraftingContainerRecipeTransferHandlerBase() {
	}

	@Override
	public Result handle(Context context) {
		if (!(context.getMenu() instanceof StorageContainerMenuBase<?> container)) {
			return Result.createNotApplicable();
		}

		Optional<? extends UpgradeContainerBase<?, ?>> potentialCraftingContainer = container.getOpenOrFirstCraftingContainer();
		if (potentialCraftingContainer.isEmpty()) {
			return Result.createFailed(Component.literal("Internal error"));
		}

		UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = potentialCraftingContainer.get();
		Display display = context.getDisplay();
		if (!(display instanceof DefaultCraftingDisplay<?> craftingDisplay)) {
			return Result.createNotApplicable();
		}

		List<Slot> craftingSlots = Collections.unmodifiableList(openOrFirstCraftingContainer instanceof ICraftingContainer cc ? cc.getRecipeSlots() : Collections.emptyList());
		List<Slot> inventorySlots = container.realInventorySlots.stream().filter(s -> s.mayPickup(context.getMinecraft().player)).toList();
		if (!validateTransferInfo(container, craftingSlots, inventorySlots)) {
			return Result.createFailed(Component.literal("Internal error"));
		}

		List<EntryIngredient> inputItemSlotViews = craftingDisplay.getInputEntries();
		if (!validateRecipeView(container, craftingSlots, inputItemSlotViews)) {
			return Result.createFailed(Component.literal("Internal error"));
		}

		InventoryState inventoryState = getInventoryState(craftingSlots, inventorySlots, context.getMinecraft().player, container);
		if (inventoryState == null) {
			return Result.createFailed(Component.literal("Internal error"));
		}

		// check if we have enough inventory space to shuffle items around to their final locations
		int inputCount = inputItemSlotViews.size();
		if (!inventoryState.hasRoom(inputCount)) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.inventory.full");
			return Result.createFailed(message);
		}

		if (!context.isActuallyCrafting()) {
			return Result.createSuccessful();
		}

		RecipeTransferOperationsResult transferOperations = RecipeTransferUtil.getRecipeTransferOperations(
				stackHelper,
				inventoryState.availableItemStacks,
				inputItemSlotViews,
				craftingSlots
		);

		if (transferOperations.missingItems.size() > 0) {
			Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
			return handlerHelper.createUserErrorForMissingSlots(message, transferOperations.missingItems);
		}

		if (!RecipeTransferUtil.validateSlots(player, transferOperations.results, craftingSlots, inventorySlots)) {
			return Result.createFailed(Component.literal("Internal error"));
		}

		List<Integer> craftingSlotIndexes = craftingSlots.stream().map(s -> s.index).sorted().toList();
		List<Integer> inventorySlotIndexes = inventorySlots.stream().map(s -> s.index).sorted().toList();

		if (!openOrFirstCraftingContainer.isOpen()) {
			container.getOpenContainer().ifPresent(c -> {
				c.setIsOpen(false);
				container.setOpenTabId(-1);
			});
			openOrFirstCraftingContainer.setIsOpen(true);
			container.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
		}

		TransferRecipeMessage message = new TransferRecipeMessage(
				toMap(transferOperations.results),
				craftingSlotIndexes,
				inventorySlotIndexes,
				context.isStackedCrafting());
		PacketHandler.sendToServer(message);

		return Result.createSuccessful();
	}

	private Map<Integer, Integer> toMap(List<TransferOperation> transferOperations) {
		Map<Integer, Integer> ret = new HashMap<>();
		transferOperations.forEach(to -> ret.put(to.craftingSlot().index, to.inventorySlot().index));
		return ret;
	}

	private boolean validateTransferInfo(
			StorageContainerMenuBase<?> container,
			List<Slot> craftingSlots,
			List<Slot> inventorySlots
	) {
		Collection<Integer> craftingSlotIndexes = slotIndexes(craftingSlots);
		Collection<Integer> inventorySlotIndexes = slotIndexes(inventorySlots);
		ArrayList<Slot> allSlots = new ArrayList<>(container.realInventorySlots);
		allSlots.addAll(container.upgradeSlots);
		Collection<Integer> containerSlotIndexes = slotIndexes(allSlots);

		if (!containerSlotIndexes.containsAll(craftingSlotIndexes)) {
			SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
							"The Recipes Transfer Helper references crafting slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(craftingSlotIndexes), StringUtil.intsToString(containerSlotIndexes)
			);
			return false;
		}

		if (!containerSlotIndexes.containsAll(inventorySlotIndexes)) {
			SophisticatedCore.LOGGER.error("Recipe Transfer helper {} does not work for container {}. " +
							"The Recipes Transfer Helper references inventory slot indexes [{}] that are not found in the inventory container slots [{}]",
					getClass(), container.getClass(), StringUtil.intsToString(inventorySlotIndexes), StringUtil.intsToString(containerSlotIndexes)
			);
			return false;
		}

		return true;
	}

	private boolean validateRecipeView(
			StorageContainerMenuBase container,
			List<Slot> craftingSlots,
			List<EntryIngredient> inputSlots
	) {
		if (inputSlots.size() > craftingSlots.size()) {
			SophisticatedCore.LOGGER.error("Recipe View {} does not work for container {}. " +
							"The Recipe View has more input slots ({}) than the number of inventory crafting slots ({})",
					getClass(), container.getClass(), inputSlots.size(), craftingSlots.size()
			);
			return false;
		}

		return true;
	}

	@Nullable
	private InventoryState getInventoryState(
			Collection<Slot> craftingSlots,
			Collection<Slot> inventorySlots,
			Player player,
			StorageContainerMenuBase container
	) {
		Map<Slot, ItemStack> availableItemStacks = new HashMap<>();
		int filledCraftSlotCount = 0;
		int emptySlotCount = 0;

		for (Slot slot : craftingSlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.mayPickup(player)) {
					SophisticatedCore.LOGGER.error(
							"Recipe Transfer helper {} does not work for container {}. " +
									"The Player is not able to move items out of Crafting Slot number {}",
							getClass(), container.getClass(), slot.index
					);
					return null;
				}
				filledCraftSlotCount++;
				availableItemStacks.put(slot, stack.copy());
			}
		}

		for (Slot slot : inventorySlots) {
			final ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				if (!slot.mayPickup(player)) {
					SophisticatedCore.LOGGER.error(
							"Recipe Transfer helper {} does not work for container {}. " +
									"The Player is not able to move items out of Inventory Slot number {}",
							getClass(), container.getClass(), slot.index
					);
					return null;
				}
				availableItemStacks.put(slot, stack.copy());
			} else {
				emptySlotCount++;
			}
		}

		return new InventoryState(availableItemStacks, filledCraftSlotCount, emptySlotCount);
	}

	private Set<Integer> slotIndexes(Collection<Slot> slots) {
		return slots.stream()
				.map(s -> s.index)
				.collect(Collectors.toSet());
	}

	private int getEmptySlotCount(Map<Integer, Slot> inventorySlots, Map<Integer, ItemStack> availableItemStacks) {
		int emptySlotCount = 0;
		for (Slot slot : inventorySlots.values()) {
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				availableItemStacks.put(slot.index, stack.copy());
			} else {
				++emptySlotCount;
			}
		}
		return emptySlotCount;
	}

	public record InventoryState(
			Map<Slot, ItemStack> availableItemStacks,
			int filledCraftSlotCount,
			int emptySlotCount
	) {
		*/
/**
		 * check if we have enough inventory space to shuffle items around to their final locations
		 *//*

		public boolean hasRoom(int inputCount) {
			return filledCraftSlotCount - inputCount <= emptySlotCount;
		}
	}

}
*/
