package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionCancelEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelSub extends SubCommand {

	public CancelSub() {
		super("cancel", Permission.COMMAND_CANCEL, false, "c");
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return;
		}

		if (!sender.hasPermission(Permission.COMMAND_CANCEL_OTHERS.toString())) {
			Player p = (Player) sender;

			if (!p.getUniqueId().equals(current.getAuctioneer().getUniqueId())) {
				sendPropMessage(p, "command.auction.cancel.not_yours");
				return;
			}

			int minTime = ConfigManager.getConfig().getInt("general.must-cancel-before");

			if (current.getAuctionTime() < minTime) {
				sendPropMessage(p, "command.auction.cancel.too_late");
				return;
			}
		}

		AuctionCancelEvent event = new AuctionCancelEvent(current, sender);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		EzAuctions.getAuctionManager().getCurrentRunnable().cancelAuction();
		sendPropMessage(sender, "command.auction.cancel.success");
		Bukkit.broadcastMessage(Messages.getString("auction.cancelled"));
	}
}
