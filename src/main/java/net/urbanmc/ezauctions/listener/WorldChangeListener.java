package net.urbanmc.ezauctions.listener;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.ScoreboardManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.scoreboard.Score;

import java.util.UUID;

public class WorldChangeListener implements Listener {

	// run reward offline when player changes worlds as well
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();
		UUID id = player.getUniqueId();
		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(id);

		if (!ap.getOfflineItems().isEmpty())
			RewardUtil.rewardOffline(ap);

		// remove auctions board if moved to wrong world

		String world = player.getWorld().getName();
		String currentAuctionWorld = EzAuctions.getAuctionManager().getCurrentAuction().getWorld();

		if (ConfigManager.getConfig().getStringList("blocked-worlds").contains(world))
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

		if (ConfigManager.getConfig().getBoolean("per-world-broadcast") && !world.equals(currentAuctionWorld))
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	}
}
