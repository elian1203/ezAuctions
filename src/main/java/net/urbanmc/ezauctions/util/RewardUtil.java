package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bidder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class RewardUtil {

	public static void rewardAuction(Auction auction, Economy econ) {
		double percentTax = ConfigManager.getConfig().getDouble("auctions.fees.tax-percent");
		double percentYield = (100 - percentTax) / 100;

		Bidder lastBid = auction.getLastBidder();

		double moneyYield = lastBid.getAmount() * percentYield;

		OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();
		OfflinePlayer bidder = lastBid.getBidder().getOfflinePlayer();

		econ.depositPlayer(auctioneer, moneyYield);

		if (auctioneer.isOnline()) {
			DecimalFormat df = new DecimalFormat("#.##");
			df.setRoundingMode(RoundingMode.DOWN);

			MessageUtil.privateMessage(auction.getAuctioneer().getOnlinePlayer(),
			                           "reward.money_given",
			                           df.format(moneyYield));
		}

		if (bidder.isOnline()) {
			Player p = lastBid.getBidder().getOnlinePlayer();

			MessageUtil.privateMessage(p, "reward.received");
			ItemUtil.addItemToInventory(p, auction.getItem(), auction.getAmount(), true);
		} else {
			ItemStack item = auction.getItem().clone();
			item.setAmount(auction.getAmount());

			lastBid.getBidder().getOfflineItems().add(item);
			AuctionsPlayerManager.getInstance().saveItems(lastBid.getBidder());
		}

		List<Bidder> bidders = auction.getLosingBidders();

		returnBidderMoney(bidders);
	}

	private static void returnBidderMoney(List<Bidder> bidders) {
		for (Bidder bidder : bidders) {
			EzAuctions.getEcon().depositPlayer(bidder.getBidder().getOfflinePlayer(), bidder.getAmount());
		}
	}

	public static void rewardCancel(Auction auction) {
		OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();

		if (auctioneer.isOnline()) {
			Player p = auction.getAuctioneer().getOnlinePlayer();

			MessageUtil.privateMessage(p, "reward.returned");
			ItemUtil.addItemToInventory(p, auction.getItem(), auction.getAmount(), true);
		} else {
			ItemStack item = auction.getItem().clone();
			item.setAmount(auction.getAmount());

			auction.getAuctioneer().getOfflineItems().add(item);
			AuctionsPlayerManager.getInstance().saveItems(auction.getAuctioneer());
		}

		List<Bidder> bidders = auction.getBidders();

		returnBidderMoney(bidders);
	}

	public static void rewardImpound(Auction auction, Player impounder) {
		ItemUtil.addItemToInventory(impounder, auction.getItem(), auction.getAmount(), true);

		List<Bidder> bidders = auction.getBidders();

		returnBidderMoney(bidders);
	}

	public static void rewardOffline(AuctionsPlayer ap) {
		boolean overflow = false;

		Player p = ap.getOnlinePlayer();

		MessageUtil.privateMessage(p, "reward.relogged");

		for (ItemStack is : ap.getOfflineItems()) {
			boolean b = ItemUtil.addItemToInventory(p, is, is.getAmount(), false);

			if (b) {
				overflow = true;
			}
		}

		ap.getOfflineItems().clear();
		AuctionsPlayerManager.getInstance().saveItems(ap);

		if (overflow) {
			MessageUtil.privateMessage(p, "reward.full_inventory");
		}
	}
}
