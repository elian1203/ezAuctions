package net.urbanmc.ezauctions.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionBidEvent;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bidder;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("bid|b")
@CommandPermission("ezauctions.bid")
@Description("Bid on an auction")
public class BidCommand extends BaseCommand {

	@Syntax("[amount]")
	@Default
	public void bid(AuctionsPlayer ap, Auction auc, @Default(value = "0") double amount) {
		Player p = ap.getOnlinePlayer();

		if (amount < 0) {
			sendPropMessage(p, "command.bid.invalid_amount");
			return;
		}

		if (AuctionUtil.blockedWorld(p)) {
			sendPropMessage(p, "command.bid.blocked_world");
			return;
		}

		// check if player is in the right world of the auction when per-world auctions is enabled
		if (ConfigManager.getConfig().getBoolean("auctions.per-world-auctions")
				&& !p.getWorld().getName().equals(auc.getWorld())) {
			sendPropMessage(p, "command.bid.wrong_world", auc.getWorld());
			return;
		}

		if (auc.getAuctioneer().getUniqueId().equals(p.getUniqueId())) {
			sendPropMessage(p, "command.bid.self_bid");
			return;
		}

		if (amount < 0) {
			sendPropMessage(p, "command.bid.invalid_amount");
			return;
		}

		Bidder lastAuctionBidder = auc.getLastBidder();

		if (amount == 0) {
			if (lastAuctionBidder == null)
				amount = auc.getStartingPrice();
			else
				amount = lastAuctionBidder.getAmount() + auc.getIncrement();
		}

		if (lastAuctionBidder == null) {
			if (amount < auc.getStartingPrice()) {
				sendPropMessage(p, "command.bid.too_low");
				return;
			}
		} else {
			if (amount < lastAuctionBidder.getAmount() + auc.getIncrement()) {
				sendPropMessage(p, "command.bid.too_low");
				return;
			}
		}

		if (auc.getAutoBuy() != 0 && amount > auc.getAutoBuy()) {
			amount = auc.getAutoBuy();
		}

		double amountToRemove = amount;

		int bidderIndex = auc.getBidList().indexOf(ap);
		Bidder bid = auc.getBidList().get(bidderIndex);

		if (bid != null) {
			amountToRemove -= bid.getAmount();
		}

		if (!hasAmount(p, amountToRemove)) {
			sendPropMessage(p, "command.bid.lacking_money");
			return;
		}

		if (auc.isSealed() && auc.getTimesBid(ap) == ConfigManager.getConfig().getInt("sealed-auctions.max-bids")) {
			sendPropMessage(p, "command.bid.max-bids");
			return;
		}

		int consecutiveBids = auc.getConsecutiveBids(ap), maxConsecutiveBids =
				ConfigManager.getConfig().getInt("auctions.maximum.consecutive-bids");

		if (maxConsecutiveBids != 0 && consecutiveBids == maxConsecutiveBids) {
			sendPropMessage(p, "command.bid.consecutive_limit");
			return;
		}

		boolean newBid = false;

		if (bid == null) {
			bid = new Bidder(ap);
			newBid = true;
		}

		bid.setAmount(amount);

		AuctionBidEvent event = new AuctionBidEvent(auc, bid);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		removeMoney(ap, amountToRemove);

		if (newBid) {
			auc.addNewBidder(bid);
		} else {
			auc.getBidList().updateBid(bidderIndex);
			auc.updateBidder(bid);
		}

		if (auc.isSealed()) {
			sendPropMessage(p, "command.bid.placed");
		}
	}

	private void sendPropMessage(CommandSender sender, String property, String... args) {
		String message = Messages.getString(property, args);

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
}
