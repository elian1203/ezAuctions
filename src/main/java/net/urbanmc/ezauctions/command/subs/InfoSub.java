package net.urbanmc.ezauctions.command.subs;

import net.md_5.bungee.api.chat.BaseComponent;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.MessageUtil;
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

		BaseComponent comp = current.getStartingMessage();
		MessageUtil.privateMessage(sender, comp);
	}
}
