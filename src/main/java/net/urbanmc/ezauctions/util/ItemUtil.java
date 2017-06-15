package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUtil {

	@SuppressWarnings("deprecation")
	public static Material getMaterial(String type) {
		Material material = Material.getMaterial(type.toUpperCase());

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

	/**
	 * @return true if there is overflow, false if not
	 */
	static boolean addItemToInventory(Player p, ItemStack is, int amount, boolean message) {
		List<ItemStack> items = new ArrayList<>();

		int maxStackSize = ReflectionUtil.getMaxStackSize(is);

		while (amount > maxStackSize) {
			ItemStack cloned = is.clone();
			cloned.setAmount(maxStackSize);

			items.add(cloned);
			amount -= maxStackSize;
		}

		if (amount != 0) {
			ItemStack cloned = is.clone();
			cloned.setAmount(amount);

			items.add(cloned);
		}

		ItemStack[] array = new ItemStack[items.size()];
		array = items.toArray(array);

		Map<Integer, ItemStack> leftover = p.getInventory().addItem(array);

		if (!leftover.isEmpty()) {
			leftover.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));

			if (message) {
				p.sendMessage(Messages.getString("reward.full_inventory"));
			}
		}

		return !leftover.isEmpty();
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
