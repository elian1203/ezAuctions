package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Bid;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class RewardUtil {

	public static void rewardAuction(Auction auction, Economy econ) {
		double percentTax = ConfigManager.getConfig().getDouble("auctions.fees.tax-percent");
		double percentYield = (100 - percentTax) / 100;

		Bid lastBid = auction.getLastBid();

		double moneyYield = lastBid.getAmount() * percentYield;

		OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();
		OfflinePlayer bidder = lastBid.getBidder().getOfflinePlayer();

		econ.depositPlayer(auctioneer, moneyYield);
		econ.withdrawPlayer(bidder, lastBid.getAmount());

		ItemUtil.addItemsToInventory(auctioneer, auction.getItem(), auction.getAmount());
	}

	public static void rewardCancel(Auction auction) {
		OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();

		ItemUtil.addItemsToInventory(auctioneer, auction.getItem(), auction.getAmount());
	}

	public static void rewardImpound(Auction auction, Player impounder) {
		ItemUtil.addItemsToInventory(impounder, auction.getItem(), auction.getAmount());
	}
}
