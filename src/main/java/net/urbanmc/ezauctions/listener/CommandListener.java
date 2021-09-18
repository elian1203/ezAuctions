package net.urbanmc.ezauctions.listener;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class CommandListener implements Listener {

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        if (!EzAuctions.getAuctionManager().isParticipatingInAuction(id))
            return;

        String command = e.getMessage().split(" ")[0];

        for (String blockedCommand : ConfigManager.getConfig().getStringList("auctions.blocked-commands")) {
            if (command.equalsIgnoreCase("/" + blockedCommand)) {
                e.setCancelled(true);
                MessageUtil.privateMessage(e.getPlayer(), "auction.blocked_command");

                return;
            }
        }
    }
}
