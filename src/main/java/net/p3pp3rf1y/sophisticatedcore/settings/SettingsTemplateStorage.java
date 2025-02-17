package net.p3pp3rf1y.sophisticatedcore.settings;

import io.github.fabricators_of_create.porting_lib.util.LogicalSidedProvider;
import io.github.fabricators_of_create.porting_lib.util.ServerLifecycleHooks;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public class SettingsTemplateStorage extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedCore.ID + "_settings_templates";
	private Map<UUID, Map<Integer, CompoundTag>> playerTemplates = new HashMap<>();
	private Map<UUID, Map<String, CompoundTag>> playerNamedTemplates = new HashMap<>();
	private static final SettingsTemplateStorage clientStorageCopy = new SettingsTemplateStorage();

	private SettingsTemplateStorage() {}

	private SettingsTemplateStorage(Map<UUID, Map<Integer, CompoundTag>> playerTemplates, Map<UUID, Map<String, CompoundTag>> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public void putPlayerTemplate(Player player, int slot, CompoundTag settingsTag) {
		playerTemplates.computeIfAbsent(player.getUUID(), u -> new HashMap<>()).put(slot, settingsTag);
		setDirty();
	}

	public void putPlayerNamedTemplate(Player player, String name, CompoundTag settingsTag) {
		playerNamedTemplates.computeIfAbsent(player.getUUID(), u -> new TreeMap<>()).put(name, settingsTag);
		setDirty();
	}

	public Map<Integer, CompoundTag> getPlayerTemplates(Player player) {
		return playerTemplates.getOrDefault(player.getUUID(), new HashMap<>());
	}

	public Map<String, CompoundTag> getPlayerNamedTemplates(Player player) {
		return playerNamedTemplates.getOrDefault(player.getUUID(), new TreeMap<>());
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		NBTHelper.putMap(tag, "playerTemplates", playerTemplates, UUID::toString, slotTemplates -> NBTHelper.putMap(new CompoundTag(), "slotTemplates", slotTemplates, String::valueOf, settingsTag -> settingsTag));
		NBTHelper.putMap(tag, "playerNamedTemplates", playerNamedTemplates, UUID::toString, namedTemplates -> NBTHelper.putMap(new CompoundTag(), "namedTemplates", namedTemplates, v -> v, settingsTag -> settingsTag));
		return tag;
	}

	public static SettingsTemplateStorage get() {
		if (!LogicalSidedProvider.WORKQUEUE.get(EnvType.CLIENT).isSameThread()) {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (server != null) {
				ServerLevel overworld = server.getLevel(Level.OVERWORLD);
				//noinspection ConstantConditions - by this time overworld is loaded
				DimensionDataStorage storage = overworld.getDataStorage();
				return storage.computeIfAbsent(SettingsTemplateStorage::load, SettingsTemplateStorage::new, SAVED_DATA_NAME);
			}
		}
		return clientStorageCopy;
	}

	private static SettingsTemplateStorage load(CompoundTag tag) {
		return new SettingsTemplateStorage(
				NBTHelper.getMap(tag, "playerTemplates", UUID::fromString,
				(key, playerTemplatesTag) -> NBTHelper.getMap((CompoundTag) playerTemplatesTag, "slotTemplates", Integer::valueOf, (k, settingsTag) -> Optional.of((CompoundTag) settingsTag))
		).orElse(new HashMap<>()),
				NBTHelper.getMap(tag, "playerNamedTemplates", UUID::fromString,
				(key, playerNamedTemplatesTag) -> NBTHelper.getMap((CompoundTag) playerNamedTemplatesTag, "namedTemplates", v -> v, (k, settingsTag) -> Optional.of((CompoundTag) settingsTag), TreeMap::new)
		).orElse(new TreeMap<>()));
	}

	public void clearPlayerTemplates(Player player) {
		playerTemplates.remove(player.getUUID());
		playerNamedTemplates.remove(player.getUUID());
		setDirty();
	}
}
