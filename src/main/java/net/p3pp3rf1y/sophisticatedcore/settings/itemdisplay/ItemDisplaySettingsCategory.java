package net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.ISlotColorCategory;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemDisplaySettingsCategory implements ISettingsCategory, ISlotColorCategory {
	public static final String NAME = "item_display";
	private static final String SLOT_TAG = "slot";
	private static final String ROTATION_TAG = "rotation";
	private static final String SLOTS_TAG = "slots";
	private static final String ROTATIONS_TAG = "rotations";
	private static final String COLOR_TAG = "color";
	private final Supplier<InventoryHandler> inventoryHandlerSupplier;
	private final Supplier<RenderInfo> renderInfoSupplier;
	private CompoundTag categoryNbt;
	private final Consumer<CompoundTag> saveNbt;
	private final int itemNumberLimit;
	private DyeColor color = DyeColor.RED;
	private final List<Integer> slotIndexes = new LinkedList<>();
	private Map<Integer, Integer> slotRotations = new HashMap<>();

	public ItemDisplaySettingsCategory(Supplier<InventoryHandler> inventoryHandlerSupplier, Supplier<RenderInfo> renderInfoSupplier, CompoundTag categoryNbt, Consumer<CompoundTag> saveNbt, int itemNumberLimit) {
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		this.renderInfoSupplier = renderInfoSupplier;
		this.categoryNbt = categoryNbt;
		this.saveNbt = saveNbt;
		this.itemNumberLimit = itemNumberLimit;

		deserialize();
	}

	public void unselectSlot(int slotIndex) {
		int orderIndex = slotIndexes.indexOf(slotIndex);

		//noinspection RedundantCollectionOperation
		slotIndexes.remove(orderIndex);
		slotRotations.remove(slotIndex);
		if (slotIndexes.isEmpty()) {
			categoryNbt.remove(SLOTS_TAG);
			categoryNbt.remove(ROTATIONS_TAG);
		}
		saveNbt.accept(categoryNbt);
		serializeSlotIndexes();

		updateFullRenderInfo();
	}

	private void updateFullRenderInfo() {
		RenderInfo renderInfo = renderInfoSupplier.get();
		List<RenderInfo.DisplayItem> displayItems = new ArrayList<>();

		for (int slotIndex : slotIndexes) {
			getSlotItemCopy(slotIndex).ifPresent(stackCopy ->
					displayItems.add(new RenderInfo.DisplayItem(stackCopy, slotRotations.getOrDefault(slotIndex, 0))));
		}

		renderInfo.refreshItemDisplayRenderInfo(displayItems);
	}

	private Optional<ItemStack> getSlotItemCopy(int slotIndex) {
		ItemStack slotStack = inventoryHandlerSupplier.get().getStackInSlot(slotIndex);
		if (slotStack.isEmpty()) {
			return Optional.empty();
		}
		ItemStack stackCopy = slotStack.copy();
		stackCopy.setCount(1);
		return Optional.of(stackCopy);
	}

	public void selectSlot(int slotIndex) {
		if (slotIndexes.size() + 1 > itemNumberLimit) {
			return;
		}
		slotIndexes.add(slotIndex);
		serializeSlotIndexes();

		updateFullRenderInfo();
	}

	private void serializeSlotIndexes() {
		categoryNbt.putIntArray(SLOTS_TAG, slotIndexes);
		saveNbt.accept(categoryNbt);
	}

	public List<Integer> getSlots() {
		return slotIndexes;
	}

	public int getRotation(int slotIndex) {
		return slotRotations.getOrDefault(slotIndex, 0);
	}

	public void rotate(int slotIndex, boolean clockwise) {
		if (!slotIndexes.contains(slotIndex)) {
			return;
		}

		int rotation = getRotation(slotIndex);
		rotation = (rotation + ((clockwise ? 1 : -1) * 45) + 360) % 360;
		slotRotations.put(slotIndex, rotation);
		serializeRotations();
		updateFullRenderInfo();
	}

	private void serializeRotations() {
		NBTHelper.putMap(categoryNbt, ROTATIONS_TAG, slotRotations, String::valueOf, IntTag::valueOf);
		saveNbt.accept(categoryNbt);
	}

	public void setColor(DyeColor color) {
		this.color = color;
		categoryNbt.putInt(COLOR_TAG, color.getId());
		saveNbt.accept(categoryNbt);
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	public void reloadFrom(CompoundTag categoryNbt) {
		this.categoryNbt = categoryNbt;
		deserialize();
	}

	private void deserialize() {
		slotIndexes.clear();
		NBTHelper.getIntArray(categoryNbt, SLOTS_TAG).ifPresent(slots -> {
			for (int slot : slots) {
				slotIndexes.add(slot);
			}
		});
		slotRotations = NBTHelper.getMap(categoryNbt, ROTATIONS_TAG, Integer::valueOf, (k, v) -> Optional.of(((IntTag) v).getAsInt())).orElseGet(HashMap::new);
		color = NBTHelper.getInt(categoryNbt, COLOR_TAG).map(DyeColor::byId).orElse(DyeColor.RED);

		//legacy nbt support to be removed in the future
		NBTHelper.getInt(categoryNbt, SLOT_TAG).ifPresent(slotIndexes::add);
		NBTHelper.getInt(categoryNbt, ROTATION_TAG).ifPresent(r -> {
			if (!slotIndexes.isEmpty()) {
				slotRotations.put(slotIndexes.iterator().next(), r);
			}
		});
	}

	public void itemChanged(int changedSlotIndex) {
		if (!slotIndexes.contains(changedSlotIndex)) {
			return;
		}
		updateFullRenderInfo();
	}

	@Override
	public Optional<Integer> getSlotColor(int slotNumber) {
		return slotIndexes.contains(slotNumber) ? Optional.of(ColorHelper.getColor(color.getTextureDiffuseColors())) : Optional.empty();
	}
}
