package net.urbanmc.ezauctions.listener;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		UUID id = e.getPlayer().getUniqueId();

		AuctionsPlayerManager.getInstance().createPlayer(id);

		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(id);

		if (ap.getOfflineItems().isEmpty())
			return;

		RewardUtil.rewardOffline(ap);
	}
}
