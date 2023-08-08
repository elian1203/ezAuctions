package me.elian.ezauctions.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

@DatabaseTable(tableName = "ezAuctions_AuctionPlayer")
public class AuctionPlayer {
	@DatabaseField(id = true)
	private UUID uniqueId;

	@DatabaseField
	private boolean ignoringSpammy;

	@DatabaseField
	private boolean ignoringAll;

	@DatabaseField
	private boolean ignoringScoreboard;

	@ForeignCollectionField(eager = true)
	private ForeignCollection<AuctionPlayerIgnore> ignoredPlayers;

	@ForeignCollectionField(eager = true)
	private ForeignCollection<SavedItem> savedItems;

	public AuctionPlayer() {
	}

	public AuctionPlayer(UUID uniqueId) {
		this.uniqueId = uniqueId;
	}

	@Nullable
	public Player getOnlinePlayer() {
		return Bukkit.getPlayer(uniqueId);
	}

	@NotNull
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(uniqueId);
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public boolean isIgnoringSpammy() {
		return ignoringSpammy;
	}

	public void setIgnoringSpammy(boolean ignoringSpammy) {
		this.ignoringSpammy = ignoringSpammy;
	}

	public boolean isIgnoringAll() {
		return ignoringAll;
	}

	public void setIgnoringAll(boolean ignoringAll) {
		this.ignoringAll = ignoringAll;
	}

	public boolean isIgnoringScoreboard() {
		return ignoringScoreboard;
	}

	public void setIgnoringScoreboard(boolean ignoringScoreboard) {
		this.ignoringScoreboard = ignoringScoreboard;
	}

	public Collection<AuctionPlayerIgnore> getIgnoredPlayers() {
		return ignoredPlayers;
	}

	public Collection<SavedItem> getSavedItems() {
		return savedItems;
	}
}
