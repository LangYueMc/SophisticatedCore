/*
package net.p3pp3rf1y.sophisticatedcore.compat.inventorysorter;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;

public class InventorySorterCompat implements ICompat {
	private static final String SLOTBLACKLIST = "slotblacklist";

	public InventorySorterCompat() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::sendImc);
	}

	private void sendImc(InterModEnqueueEvent evt) {
		evt.enqueueWork(() -> {
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, StorageContainerMenuBase.StorageUpgradeSlot.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, FilterSlotItemHandler.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, FilterLogicContainer.FilterLogicSlot.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, SlotSuppliedHandler.class::getName);
			InterModComms.sendTo(CompatModIds.INVENTORY_SORTER, SLOTBLACKLIST, StorageInventorySlot.class::getName);
		});
	}

	@Override
	public void setup() {
		//noop
	}
}
*/
