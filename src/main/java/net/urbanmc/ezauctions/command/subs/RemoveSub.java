package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveSub extends SubCommand {

	public RemoveSub() {
		super("remove", Permission.COMMAND_REMOVE, true, "r");
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Player p = (Player) sender;

		Auction auction = EzAuctions.getAuctionManager().removeFromQueue(p.getUniqueId());

		if (auction == null) {
			sendPropMessage(p, "command.auction.remove.not_in_queue");
			return;
		}

		sendPropMessage(p, "command.auction.remove.success");
		RewardUtil.rewardCancel(auction);
	}
}
