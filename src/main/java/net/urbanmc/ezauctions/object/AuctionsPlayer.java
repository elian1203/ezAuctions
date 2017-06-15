package net.urbanmc.ezauctions.object;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class AuctionsPlayer {

	private UUID id;
	private boolean ignoringSpammy, ignoringAll;
	private List<ItemStack> offlineItems;

	public AuctionsPlayer(UUID id, boolean ignoringSpammy, boolean ignoringAll, List<ItemStack> offlineItems) {
		this.id = id;
		this.ignoringSpammy = ignoringSpammy;
		this.ignoringAll = ignoringAll;
		this.offlineItems = offlineItems;
	}

	public UUID getUniqueId() {
		return id;
	}

	public boolean isIgnoringSpammy() {
		return ignoringSpammy;
	}

	public boolean isIgnoringAll() {
		return ignoringAll;
	}

	public List<ItemStack> getOfflineItems() {
		return offlineItems;
	}

	public Player getOnlinePlayer() {
		return Bukkit.getPlayer(id);
	}

	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(id);
	}
}
