package net.p3pp3rf1y.sophisticatedcore.settings;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntConsumer;

public abstract class SettingsTab<C extends SettingsContainerBase<?>> extends SettingsTabBase<SettingsScreen> {
	private final C settingsContainer;

	protected SettingsTab(C settingsContainer, Position position, SettingsScreen screen, Component tabLabel, List<Component> tooltip,
			List<Component> openTooltip, Function<IntConsumer, ButtonBase> getTabButton) {
		super(position, screen, tabLabel, tooltip, openTooltip, getTabButton);
		this.settingsContainer = settingsContainer;
	}

	protected C getSettingsContainer() {
		return settingsContainer;
	}

	public abstract Optional<Integer> getSlotOverlayColor(int slotNumber);

	public abstract void handleSlotClick(Slot slot, int mouseButton);

	@SuppressWarnings("unused") // parameter used in override
	public int getItemRotation(int slotIndex) {
		return 0;
	}

	@SuppressWarnings("unused") //parameters used in overrides
	public void renderExtra(PoseStack poseStack, Slot slot) {
		//noop by default
	}

	@SuppressWarnings("unused") // parameter used in override
	public ItemStack getItemDisplayOverride(int slotNumber) {
		return ItemStack.EMPTY;
	}

	@SuppressWarnings("unused") //parameters used in overrides
	public void drawSlotStackOverlay(PoseStack poseStack, Slot slot) {
		//noop by default
	}
}
