package net.p3pp3rf1y.sophisticatedcore.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.client.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.Collections;
import java.util.Optional;

@SuppressWarnings("unused")
public class ClientEventHandler implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModParticles.registerFactories();
        ModFluids.registerFluids();

        ServerWorldEvents.UNLOAD.register(StorageSoundHandler::onWorldUnload);
		ClientTickEvents.END_WORLD_TICK.register(StorageSoundHandler::tick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> RecipeHelper.setWorld(client.level));

        ScreenEvents.BEFORE_INIT.register((client, screen, windowWidth, windowHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerGui) || screen instanceof CreativeModeInventoryScreen || client.player == null) {
                return;
            }

            ScreenEvents.afterRender(screen).register(ClientEventHandler::onDrawScreen);
        });

        PacketHandler.getChannel().initClientListener();
    }

    private static void onDrawScreen(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        Screen gui = mc.screen;
        if (!(gui instanceof AbstractContainerScreen<?> containerGui) || gui instanceof CreativeModeInventoryScreen || mc.player == null) {
            return;
        }
        AbstractContainerMenu menu = containerGui.getMenu();
        ItemStack held = menu.getCarried();
        if (!held.isEmpty()) {
            Slot under = GuiHelper.getSlotUnderMouse(containerGui).orElse(null);

            for (Slot s : menu.slots) {
                ItemStack stack = s.getItem();
                if (!s.mayPickup(mc.player) || stack.isEmpty()) {
                    continue;
                }
				Optional<StashResultAndTooltip> stashResultAndTooltip = getStashResultAndTooltip(stack, held);
				if (stashResultAndTooltip.isEmpty()) {
					continue;
				}

                if (s == under) {
                    renderSpecialTooltip(mc, containerGui, guiGraphics, mouseX, mouseY, stashResultAndTooltip.get());
                } else {
                    renderStashSign(mc, containerGui, guiGraphics, s, stack, stashResultAndTooltip.get().stashResult());
                }
            }
        }
    }

    private static void renderStashSign(Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, Slot s, ItemStack stack, IStashStorageItem.StashResult stashResult) {
        int x = containerGui.getGuiLeft() + s.x;
        int y = containerGui.getGuiTop() + s.y;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
		poseStack.translate(0, 0, 300);

		int color = stashResult == IStashStorageItem.StashResult.MATCH_AND_SPACE ? ChatFormatting.GREEN.getColor() : 0xFFFF00;
        if (stack.getItem() instanceof IStashStorageItem) {
            guiGraphics.drawString(mc.font, "+", x + 10, y + 8, color);
        } else {
            guiGraphics.drawString(mc.font, "-", x + 1, y, color);
        }
        poseStack.popPose();
    }

    private static void renderSpecialTooltip(Minecraft mc, AbstractContainerScreen<?> containerGui, GuiGraphics guiGraphics, int x, int y, StashResultAndTooltip stashResultAndTooltip) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 100);
        guiGraphics.renderTooltip(containerGui.font, Collections.singletonList(Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".right_click_to_add_to_storage")), stashResultAndTooltip.tooltip(), x, y);
        poseStack.popPose();
    }

	private static Optional<StashResultAndTooltip> getStashResultAndTooltip(ItemStack inInventory, ItemStack held) {
		if (inInventory.getCount() == 1 && inInventory.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResultAndTooltip(inInventory, held, stashStorageItem);
		}

		if (held.getItem() instanceof IStashStorageItem stashStorageItem) {
			return getStashResultAndTooltip(held, inInventory, stashStorageItem);
		}
		return Optional.empty();
	}

    private static Optional<StashResultAndTooltip> getStashResultAndTooltip(ItemStack potentialStashStorage, ItemStack potentiallyStashable, IStashStorageItem stashStorageItem) {
		IStashStorageItem.StashResult stashResult = stashStorageItem.getItemStashable(potentialStashStorage, potentiallyStashable);
		if (stashResult == IStashStorageItem.StashResult.NO_SPACE) {
			return Optional.empty();
		}
		return Optional.of(new StashResultAndTooltip(stashResult, stashStorageItem.getInventoryTooltip(potentialStashStorage)));
    }

	private record StashResultAndTooltip(IStashStorageItem.StashResult stashResult, Optional<TooltipComponent> tooltip) {}
}
