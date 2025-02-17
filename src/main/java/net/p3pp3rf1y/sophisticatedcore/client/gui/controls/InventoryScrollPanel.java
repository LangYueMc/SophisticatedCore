package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import io.github.fabricators_of_create.porting_lib.util.client.ScrollPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public class InventoryScrollPanel extends ScrollPanel {
	private static final int TOP_Y_OFFSET = 1;
	private final IInventoryScreen screen;
	private final int firstSlotIndex;
	private final int numberOfSlots;
	private final int slotsInARow;
	public InventoryScrollPanel(Minecraft client, IInventoryScreen screen, int firstSlotIndex, int numberOfSlots, int slotsInARow, int height, int top, int left) {
		super(client, slotsInARow * 18 + 6, height, top, left, 0);
		this.screen = screen;
		this.firstSlotIndex = firstSlotIndex;
		this.numberOfSlots = numberOfSlots;
		this.slotsInARow = slotsInARow;
	}

	@Override
	protected int getScrollAmount() {
		return 18;
	}

	@Override
	protected int getContentHeight() {
		int rows = numberOfSlots / slotsInARow + (numberOfSlots % slotsInARow > 0 ? 1 : 0);
		return rows * 18;
	}

	@Override
	protected void drawBackground(GuiGraphics guiGraphics, Tesselator tess, float partialTick) {
		screen.drawSlotBg(guiGraphics);
	}

	@Override
	protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(screen.getLeftX(), screen.getTopY(), 0.0D);

		screen.renderInventorySlots(guiGraphics, mouseX, mouseY, isMouseOver(mouseX, mouseY));

		poseStack.popPose();
	}

	public Optional<Slot> findSlot(double mouseX, double mouseY) {
		if (!isMouseOver(mouseX, mouseY)) {
			return Optional.empty();
		}
		for (int slotIndex = firstSlotIndex; slotIndex < firstSlotIndex + numberOfSlots; slotIndex++) {
			Slot slot = screen.getSlot(slotIndex);
			if (screen.isMouseOverSlot(slot, mouseX, mouseY) && slot.isActive()) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	public interface IInventoryScreen {
		void renderInventorySlots(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean canShowHover);

		boolean isMouseOverSlot(Slot pSlot, double pMouseX, double pMouseY);
		void drawSlotBg(GuiGraphics guiGraphics);
		int getTopY();

		int getLeftX();

		Slot getSlot(int slotIndex);
	}

	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		//noop
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
		boolean ret = super.mouseScrolled(mouseX, mouseY, scroll);
		updateSlotsYPosition();
		return ret;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		boolean ret = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		updateSlotsYPosition();
		return ret;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isMouseOver(mouseX, mouseY)) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		return false;
	}

	public void updateSlotsYPosition() {
		for (int i = firstSlotIndex, row = 0; i < firstSlotIndex + numberOfSlots; i++, row = i / slotsInARow) {
			int newY = top - screen.getTopY() - (int) scrollDistance / 18 * 18 + row * 18 + TOP_Y_OFFSET;
			if (newY < -17 || newY > height) {
				newY = -100;
			}
			screen.getSlot(i).y = newY;
		}
	}
}
