package net.urbanmc.ezauctions.command;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionBidEvent;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bid;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BidCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sendPropMessage(sender, "command.player_only");
			return true;
		}

		if (!sender.hasPermission(Permission.COMMAND_BID.toString())) {
			sendPropMessage(sender, "command.no-perm");
			return true;
		}

		if (args.length > 1) {
			sendPropMessage(sender, "command.bid.help");
			return true;
		}

		Auction auc = EzAuctions.getAuctionManager().getCurrentAuction();

		if (auc == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return true;
		}

		Player p = (Player) sender;
		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

		if (auc.getAuctioneer().getUniqueId().equals(p.getUniqueId())) {
			sendPropMessage(sender, "command.bid.self_bid");
			return true;
		}

		if (args.length > 0 && !isPositiveDouble(args[0])) {
			sendPropMessage(sender, "command.bid.invalid_amount");
			return true;
		}

		double amount = AuctionUtil.parseNumberFromConfig(args.length == 0 ? "0" : args[0], "bid");

		if (args.length == 0) {
			if (auc.getLastBid() == null)
				amount = auc.getStartingPrice();
			else
				amount = auc.getLastBid().getAmount() + auc.getIncrement();
		}

		if (amount <= 0) {
			sendPropMessage(sender, "command.bid.invalid_amount");
			return true;
		}

		if (auc.getLastBid() == null) {
			if (amount < auc.getStartingPrice()) {
				sendPropMessage(sender, "command.bid.too_low");
				return true;
			}
		} else {
			if (amount < auc.getLastBid().getAmount() + auc.getIncrement()) {
				sendPropMessage(sender, "command.bid.too_low");
				return true;
			}
		}

		double amountToRemove = amount;

		Bid lastBid = auc.getLastBidFrom(ap);

		if (lastBid != null) {
			amountToRemove -= lastBid.getAmount();
		}

		if (!hasAmount(p, amountToRemove)) {
			sendPropMessage(sender, "command.bid.lacking_money");
			return true;
		}

		if (auc.isSealed() && auc.getTimesBid(ap) == ConfigManager.getConfig().getInt("sealed-auctions.max-bids")) {
			sendPropMessage(sender, "command.bid.max-bids");
			return true;
		}

		int consecutiveBids = auc.getConsecutiveBids(ap), maxConsecutiveBids =
				ConfigManager.getConfig().getInt("auctions.maximum.consecutive-bids");

		if (maxConsecutiveBids != 0 && consecutiveBids == maxConsecutiveBids) {
			sendPropMessage(sender, "command.bid.consecutive_limit");
			return true;
		}

		if(auc.getAutoBuy() != 0 && amount > auc.getAutoBuy())
			amount = auc.getAutoBuy();

		Bid bid = new Bid(ap, amount);

		AuctionBidEvent event = new AuctionBidEvent(auc, bid);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return true;

		removeMoney(ap, amountToRemove);
		auc.addBid(bid);

		if (auc.isSealed()) {
			sendPropMessage(sender, "command.bid.placed");
		}

		return true;
	}

	private void sendPropMessage(CommandSender sender, String property) {
		String message = Messages.getString(property);

		if (sender instanceof Player)
			sender.sendMessage(message);
		else
			sender.sendMessage(ChatColor.stripColor(message));
	}

	private void removeMoney(AuctionsPlayer ap, double amt) {
		EzAuctions.getEcon().withdrawPlayer(ap.getOfflinePlayer(), amt);
	}

	private boolean hasAmount(Player p, double amt) {
		return EzAuctions.getEcon().getBalance(p) >= amt;
	}

	private boolean isPositiveDouble(String number) {
		try {
			double d = Double.parseDouble(number);
			return d > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
