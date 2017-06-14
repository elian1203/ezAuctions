package net.urbanmc.ezauctions.command.subs;

import mkremins.fanciful.FancyMessage;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;

public class InfoSub extends SubCommand {

	public InfoSub() {
		super("info", Permission.COMMAND_INFO, false, "i");
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return;
		}

		FancyMessage fancy = current.getStartingMessage();
		fancy.send(sender);
	}
}
