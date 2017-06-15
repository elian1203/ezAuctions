package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;

public class EnableSub extends SubCommand {

	public EnableSub() {
		super("enable", Permission.COMMAND_ENABLE, false);
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		boolean enabled = EzAuctions.getAuctionManager().isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.enable.already_enabled";
		} else {
			prop = "command.auction.enable.success";
			EzAuctions.getAuctionManager().setAuctionsEnabled(true);
		}

		sendPropMessage(sender, prop);
	}
}
