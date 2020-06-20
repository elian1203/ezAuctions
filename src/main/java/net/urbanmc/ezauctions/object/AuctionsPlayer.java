package net.urbanmc.ezauctions.object;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class AuctionsPlayer {

	private UUID id;
	private boolean ignoringSpammy, ignoringAll, ignoringScoreboard;
	private List<UUID> ignoringPlayers;
	private List<OfflineItem> offlineItems;

	public AuctionsPlayer(UUID id, boolean ignoringSpammy, boolean ignoringAll, boolean ignoringScoreboard,
	                      List<UUID> ignoringPlayers, List<OfflineItem> offlineItems) {
		this.id = id;
		this.ignoringSpammy = ignoringSpammy;
		this.ignoringAll = ignoringAll;
		this.ignoringScoreboard = ignoringScoreboard;
		this.offlineItems = offlineItems;
		this.ignoringPlayers = ignoringPlayers;
	}

	public UUID getUniqueId() {
		return id;
	}

	public boolean isIgnoringSpammy() {
		return ignoringSpammy;
	}

	public void setIgnoringSpammy(boolean ignoringSpammy) {
		this.ignoringSpammy = ignoringSpammy;
		AuctionsPlayerManager.getInstance().saveBooleans(this);
	}

	public boolean isIgnoringAll() {
		return ignoringAll;
	}

	public void setIgnoringAll(boolean ignoringAll) {
		this.ignoringAll = ignoringAll;
		AuctionsPlayerManager.getInstance().saveBooleans(this);
	}

	public boolean isIgnoringScoreboard() {
		return ignoringScoreboard;
	}

	public void setIgnoringScoreboard(boolean ignoringScoreboard) {
		this.ignoringScoreboard = ignoringScoreboard;
		AuctionsPlayerManager.getInstance().saveBooleans(this);
	}

	public List<OfflineItem> getOfflineItems() {
		return offlineItems;
	}

	public Player getOnlinePlayer() {
		return Bukkit.getPlayer(id);
	}

	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(id);
	}

	public List<UUID> getIgnoringPlayers() {
		return ignoringPlayers;
	}

	public AuctionsPlayer clone() {
		return new AuctionsPlayer(id, ignoringSpammy, ignoringAll, ignoringScoreboard, ignoringPlayers, offlineItems);
	}
}
