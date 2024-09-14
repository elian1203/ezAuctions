package me.elian.ezauctions.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import me.elian.ezauctions.controller.ConfigController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
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

	public boolean withinBoundary(ConfigController configController) {
		FileConfiguration config = configController.getConfig();
		if (!config.getBoolean("boundary.enabled", false)) {
			return true;
		}

		Player player = getOnlinePlayer();
		if (player == null) {
			return false;
		}

		Location playerLocation = player.getLocation();

		String world = config.getString("boundary.world");

		if (!player.getLocation().getWorld().getName().equalsIgnoreCase(world)) {
			return false;
		}

		double playerX = playerLocation.getX();
		double playerY = playerLocation.getY();
		double playerZ = playerLocation.getZ();

		double corner1X = config.getDouble("boundary.corner1.x");
		double corner1Y = config.getDouble("boundary.corner1.y");
		double corner1Z = config.getDouble("boundary.corner1.z");

		double corner2X = config.getDouble("boundary.corner2.x");
		double corner2Y = config.getDouble("boundary.corner2.y");
		double corner2Z = config.getDouble("boundary.corner2.z");

		// ensure player is within ranges on x, y, and z
		return valueWithinRange(playerX, corner1X, corner2X)
				&& valueWithinRange(playerY, corner1Y, corner2Y)
				&& valueWithinRange(playerZ, corner1Z, corner2Z);
	}

	/**
	 * Determines whether a value is within two bounds.
	 * @param bound1 first boundary, can be greater than or less than bound2
	 * @param bound2 second boundary, can be greater than or less than bound1
	 * @param value value to check if in between
	 * @return
	 */
	private boolean valueWithinRange(double bound1, double bound2, double value) {
		// bound1 can be greater or less than bound2 so need to check both ways
		return (value >= bound1 && value <= bound2) || (value >= bound2 && value <= bound1);
	}
}
