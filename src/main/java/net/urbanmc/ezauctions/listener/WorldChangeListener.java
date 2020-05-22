package net.urbanmc.ezauctions.listener;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.UUID;

public class WorldChangeListener implements Listener {

	// run reward offline when player changes worlds as well
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
		UUID id = e.getPlayer().getUniqueId();
		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(id);

		if (!ap.getOfflineItems().isEmpty())
			RewardUtil.rewardOffline(ap);
	}
}
