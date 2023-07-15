package net.p3pp3rf1y.sophisticatedcore.util;

import io.github.fabricators_of_create.porting_lib.item.XpRepairItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XpHelper {
	private XpHelper() {}

	private static final long RATIO = 20 * FluidHelper.BUCKET_VOLUME_IN_MILLIBUCKETS;

	public static float liquidToExperience(long liquid) {
		return (float) liquid / RATIO;
	}

	public static int experienceToLiquid(float xp) {
		return (int) (xp * RATIO);
	}

	public static int getExperienceForLevel(int level) {
		if (level == 0) {
			return 0;
		}
		if (level > 0 && level < 16) {
			return level * (12 + level * 2) / 2;
		} else if (level > 15 && level < 31) {
			return (level - 15) * (69 + (level - 15) * 5) / 2 + 315;
		} else {
			return (level - 30) * (215 + (level - 30) * 9) / 2 + 1395;
		}
	}

	public static int getExperienceLimitOnLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		} else {
			if (level >= 15) {
				return 37 + (level - 15) * 5;
			}
			return 7 + level * 2;
		}
	}

	public static int getLevelForExperience(int experience) {
		int i = 0;
		while (getExperienceForLevel(i) <= experience) {
			i++;
		}
		return i - 1;
	}

	public static double getLevelsForExperience(int experience) {
		int baseLevel = getLevelForExperience(experience);
		int remainingExperience = experience - getExperienceForLevel(baseLevel);
		return baseLevel + ((double) remainingExperience / getExperienceLimitOnLevel(baseLevel));
	}

	public static int getPlayerTotalExperience(Player player) {
		int currentLevelPoints = getExperienceForLevel(player.experienceLevel);
		int partialLevelPoints = (int) (player.experienceProgress * player.getXpNeededForNextLevel());
		return currentLevelPoints + partialLevelPoints;
	}

	public static float getXpRepairRatio(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof XpRepairItem xp) {
			return xp.getXpRepairRatio(stack);
		}

		return 0;
	}
}
