package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;

public class DisableSub extends SubCommand {

	public DisableSub() {
		super("disable", Permission.COMMAND_DISABLE, false);
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		boolean enabled = EzAuctions.getAuctionManager().isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.disable.success";
			EzAuctions.getAuctionManager().setAuctionsEnabled(false);
		} else {
			prop = "command.auction.disable.already_disabled";
		}

		sendPropMessage(sender, prop);
	}
}
