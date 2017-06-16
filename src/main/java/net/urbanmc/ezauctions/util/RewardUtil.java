package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bid;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;

public class RewardUtil {

	public static void rewardAuction(Auction auction, Economy econ) {
		double percentTax = ConfigManager.getConfig().getDouble("auctions.fees.tax-percent");
		double percentYield = (100 - percentTax) / 100;

		Bid lastBid = auction.getLastBid();

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
			AuctionsPlayerManager.getInstance().saveGson();
		}
	}

	public static void returnLosingBidders(Auction auction, Economy econ) {
		Map<AuctionsPlayer, Double> map = auction.getLosingBids();

		for (Entry<AuctionsPlayer, Double> entry : map.entrySet()) {
			econ.depositPlayer(entry.getKey().getOfflinePlayer(), entry.getValue());
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
			AuctionsPlayerManager.getInstance().saveGson();
		}
	}

	public static void rewardImpound(Auction auction, Player impounder) {
		ItemUtil.addItemToInventory(impounder, auction.getItem(), auction.getAmount(), true);
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
		AuctionsPlayerManager.getInstance().saveGson();

		if (overflow) {
			MessageUtil.privateMessage(p, "reward.full_inventory");
		}
	}
}
