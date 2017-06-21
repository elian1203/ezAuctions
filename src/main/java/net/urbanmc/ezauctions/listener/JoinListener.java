package net.urbanmc.ezauctions.listener;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		UUID id = p.getUniqueId();

		AuctionsPlayerManager.getInstance().createPlayer(id);

		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(id);

		if (EzAuctions.isUpdateAvailable() && p.hasPermission(Permission.NOTIFY_UPDATE.toString())) {
			MessageUtil.privateMessage(p, "login.updatemessage");
		}

		if (ap.getOfflineItems().isEmpty())
			return;

		RewardUtil.rewardOffline(ap);
	}
}
