package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.Material;

public class ItemUtil {

	@SuppressWarnings("deprecation")
	public static Material getMaterial(String type) {
		Material material = Material.getMaterial(type);

		if (material != null)
			return material;

		if (isInt(type)) {
			int id = Integer.parseInt(type);
			material = Material.getMaterial(id);

			if (material != null)
				return material;
		}

		ItemInfo item = Items.itemByName(type);

		if (item != null)
			return item.getType();

		return null;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
