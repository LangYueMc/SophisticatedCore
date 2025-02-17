package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.List;

public class SyncContainerStacksMessage extends SimplePacketBase {
	private final int windowId;
	private final int stateId;
	private final List<ItemStack> itemStacks;
	private final ItemStack carriedStack;

	public SyncContainerStacksMessage(int windowId, int stateId, List<ItemStack> itemStacks, ItemStack carriedStack) {
		this.windowId = windowId;
		this.stateId = stateId;
		this.itemStacks = itemStacks;
		this.carriedStack = carriedStack;
	}

	public SyncContainerStacksMessage(FriendlyByteBuf buffer) {
		this.windowId = buffer.readByte();
		this.stateId = buffer.readVarInt();
		int slots = buffer.readShort();
		this.itemStacks = NonNullList.withSize(slots, ItemStack.EMPTY);

		for (int j = 0; j < slots; ++j) {
			this.itemStacks.set(j, PacketHelper.readItemStack(buffer));
		}

		this.carriedStack = buffer.readItem();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByte(windowId);
		buffer.writeVarInt(stateId);
		buffer.writeShort(itemStacks.size());

		for (ItemStack itemstack : itemStacks) {
			PacketHelper.writeItemStack(itemstack, buffer);
		}
		buffer.writeItem(carriedStack);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Player player = context.getClientPlayer();
			if (player == null || !(player.containerMenu instanceof StorageContainerMenuBase) || player.containerMenu.containerId != windowId) {
				return;
			}
			player.containerMenu.initializeContents(stateId, itemStacks, carriedStack);
		});
		return true;
	}
}
