package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SettingsTabControl<C extends AbstractContainerScreen<?>, T extends SettingsTabBase<C>> extends CompositeWidgetBase<Tab> {
	private static final int VERTICAL_SPACE = 1;
	@Nullable
	private T openTab = null;

	protected SettingsTabControl(Position position) {
		super(position, new Dimension(0, 0));
	}

	protected <U extends T> U addSettingsTab(Runnable onTabOpenContainerAction, Runnable onTabCloseContainerAction, U tab) {
		U settingsTab = addChild(tab);
		settingsTab.setHandlers(() -> {
					if (openTab != null && differentTabIsOpen(settingsTab)) {
						openTab.close();
					}
					settingsTab.setZOffset(200);
					openTab = settingsTab;
					onTabOpenContainerAction.run();
				},
				() -> {
					if (openTab != null) {
						openTab.setZOffset(0);
						openTab = null;
						onTabCloseContainerAction.run();
					}
				},
				() -> openTab == null || !differentTabIsOpen(settingsTab) || isNotCovered(openTab, settingsTab, true),
				() -> openTab == null || isNotCovered(openTab, settingsTab, false)
		);
		return settingsTab;
	}

	private boolean isNotCovered(T open, Tab t, boolean checkFullyCovered) {
		if (checkFullyCovered) {
			return open.getBottomY() < t.getBottomY() || open.getTopY() > t.getTopY();
		} else {
			return open.getBottomY() < t.getTopY() || open.getTopY() > t.getTopY();
		}
	}

	private boolean differentTabIsOpen(Tab tab) {
		return openTab != tab;
	}

	public Optional<T> getOpenTab() {
		return Optional.ofNullable(openTab);
	}

	@Override
	protected void renderBg(PoseStack matrixStack, Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	public void renderTooltip(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
		children.forEach(tab -> tab.renderTooltip(screen, poseStack, mouseX, mouseY));
	}

	protected int getTopY() {
		return y + children.size() * (Tab.DEFAULT_HEIGHT + VERTICAL_SPACE);
	}

	public int getHeight() {
		MutableInt maxY = new MutableInt(0);

		children.forEach(tab -> {
			int bottomY = tab.getBottomY();
			if (bottomY > maxY.getValue()) {
				maxY.setValue(bottomY);
			}
		});
		return maxY.getValue() - y;
	}

	@Override
	public int getWidth() {
		MutableInt maxWidth = new MutableInt(0);

		children.forEach(tab -> {
			int width = tab.getWidth();
			if (width > maxWidth.getValue()) {
				maxWidth.setValue(width);
			}
		});
		return maxWidth.getValue();
	}

	public List<Rect2i> getTabRectangles() {
		List<Rect2i> ret = new ArrayList<>();
		children.forEach(child -> child.getTabRectangle().ifPresent(ret::add));
		return ret;
	}

	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		//noop
	}
}
