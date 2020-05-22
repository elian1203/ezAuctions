package net.urbanmc.ezauctions.object;

import org.bukkit.inventory.ItemStack;

public class OfflineItem {

	private ItemStack item;
	private String world;

	public OfflineItem(ItemStack item, String world) {
		this.item = item;
		this.world = world;
	}

	public ItemStack getItem() {
		return item;
	}

	public String getWorld() {
		return world;
	}
}
