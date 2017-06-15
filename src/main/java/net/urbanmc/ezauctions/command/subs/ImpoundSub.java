package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionImpoundEvent;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ImpoundSub extends SubCommand {

	public ImpoundSub() {
		super("impound", Permission.COMMAND_IMPOUND, true);
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return;
		}

		Player p = (Player) sender;

		AuctionImpoundEvent event = new AuctionImpoundEvent(current, p);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		sendPropMessage(p, "command.auction.impound");
		EzAuctions.getAuctionManager().getCurrentRunnable().impoundAuction(p);
	}
}
