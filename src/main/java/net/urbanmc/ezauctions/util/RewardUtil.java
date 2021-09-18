package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

public class RewardUtil {

	public static void rewardAuction(Auction auction, Economy econ) {
		// run async since we will be calling vault with offline player
		Plugin plugin = Bukkit.getPluginManager().getPlugin("ezAuctions");
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Bidder lastBid = auction.getLastBidder();

			OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();
			OfflinePlayer bidder = lastBid.getBidder().getOfflinePlayer();


			double percentTax = ConfigManager.getConfig().getDouble("auctions.fees.tax-percent");
			double percentYield = (100 - percentTax) / 100;

			// vault perms not set up, attempt to check player online status
			// this may result in logged-off players not receiving an exemption even if they have the permission
			if (EzAuctions.getPerms() == null) {
				if (auctioneer.isOnline() && auctioneer.getPlayer().hasPermission("ezauctions.taxexempt"))
					percentYield = 1;
			} else if (EzAuctions.getPerms().playerHas(auction.getWorld(), auctioneer, "ezauctions.taxexempt")) {
				// vault permission checked, exempt from taxes
				percentYield = 1;
			}

			double moneyYield = lastBid.getAmount() * percentYield;

			econ.depositPlayer(auctioneer, moneyYield);

			if (auctioneer.isOnline()) {
				DecimalFormat df = new DecimalFormat("#.##");
				df.setRoundingMode(RoundingMode.DOWN);

				MessageUtil.privateMessage(auction.getAuctioneer().getOnlinePlayer(),
						"reward.money_given",
						df.format(moneyYield));
			}

			returnBidderMoney(auction.getBidList(), false);

			if (bidder.isOnline()) {
				Player p = lastBid.getBidder().getOnlinePlayer();

			/*
			if player per-world-auctions is enabled and player is not in right world we will
			send them a message to notify them that they must return to claim their winnings
			and then add the item to their offline items

			otherwise, we will give them their item and be done
			 */
				if (ConfigManager.getConfig().getBoolean("auctions.per-world-auctions")
						&& !p.getWorld().getName().equals(auction.getWorld())) {
					MessageUtil.privateMessage(p, "reward.wrong_world", auction.getWorld());
				} else if (AuctionUtil.blockedWorld(p)) {
					MessageUtil.privateMessage(p, "reward.blocked_world");
				} else {
					MessageUtil.privateMessage(p, "reward.received");
					ItemUtil.addItemToInventory(p, auction.getItem(), auction.getAmount(), true);

					return;
				}
			}

			ItemStack item = auction.getItem().clone();
			item.setAmount(auction.getAmount());

			OfflineItem offlineItem = new OfflineItem(item, auction.getWorld());
			lastBid.getBidder().getOfflineItems().add(offlineItem);
			AuctionsPlayerManager.getInstance().saveItems(lastBid.getBidder());
		});
	}

	private static void returnBidderMoney(List<Bidder> bidders) {
		for (Bidder bidder : bidders) {
			EzAuctions.getEcon().depositPlayer(bidder.getBidder().getOfflinePlayer(), bidder.getAmount());
		}
	}

	private static void returnBidderMoney(BidList bidList, boolean allBidders) {
		// run async since we will be calling vault with offline player
		Plugin plugin = Bukkit.getPluginManager().getPlugin("ezAuctions");
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			bidList.forEach((bidder) -> EzAuctions.getEcon().depositPlayer(bidder.getBidder().getOfflinePlayer(),
							bidder.getAmount()),
					0, bidList.size() - (allBidders ? 0 : 1));
			// Ending position is size - 1 because the last bidder is the winning bidder.
		});
	}

	public static void rewardCancel(Auction auction) {
		OfflinePlayer auctioneer = auction.getAuctioneer().getOfflinePlayer();

		returnBidderMoney(auction.getBidList(), true);

		if (auctioneer.isOnline()) {
			Player p = auctioneer.getPlayer();

			/*
			if player per-world-auctions is enabled and player is not in right world we will
			send them a message to notify them that they must return to claim their winnings
			and then add the item to their offline items

			otherwise, we will give them their item and be done
			 */
			if (ConfigManager.getConfig().getBoolean("auctions.per-world-auctions")
					&& !p.getWorld().getName().equals(auction.getWorld())) {
				MessageUtil.privateMessage(p, "reward.wrong_world", auction.getWorld());
			} else if (AuctionUtil.blockedWorld(p)) {
				MessageUtil.privateMessage(p, "reward.blocked_world");
			} else {
				MessageUtil.privateMessage(p, "reward.returned");
				ItemUtil.addItemToInventory(p, auction.getItem(), auction.getAmount(), true);

				return;
			}
		}

		AuctionsPlayer ap = auction.getAuctioneer();

		ItemStack item = auction.getItem().clone();
		item.setAmount(auction.getAmount());

		OfflineItem offlineItem = new OfflineItem(item, auction.getWorld());
		ap.getOfflineItems().add(offlineItem);
		AuctionsPlayerManager.getInstance().saveItems(ap);
	}

	public static void rewardImpound(Auction auction, Player impounder) {
		ItemUtil.addItemToInventory(impounder, auction.getItem(), auction.getAmount(), true);

		returnBidderMoney(auction.getBidList(), true);
	}

	public static void rewardOffline(AuctionsPlayer ap) {
		boolean overflow = false;
		// this is used to know whether or not to send the reward.relogged message
		boolean didDrop = false;

		Player p = ap.getOnlinePlayer();

		Iterator<OfflineItem> iterator = ap.getOfflineItems().iterator();

		while (iterator.hasNext()) {
			OfflineItem offlineItem = iterator.next();

			String playerWorld = p.getWorld().getName(), itemWorld = offlineItem.getWorld();

			// if the offline item has a world stored in it (offline items before 1.5.5 will not have this),
			// and per-world-auctions is enabled, we will check to see if the player is in the right world
			if (itemWorld != null && ConfigManager.getConfig().getBoolean("auctions.per-world-auctions")) {
				if (!playerWorld.equals(itemWorld)) {
					MessageUtil.privateMessage(p, "reward.relogged_wrong_world", itemWorld);
					continue;
				}
			}

			if (AuctionUtil.blockedWorld(p)) {
				MessageUtil.privateMessage(p, "reward.relogged_blocked_world");
				continue;
			}

			didDrop = true;
			iterator.remove();

			ItemStack item = offlineItem.getItem();
			boolean b = ItemUtil.addItemToInventory(p, item, item.getAmount(), false);

			if (b) {
				overflow = true;
			}
		}

		AuctionsPlayerManager.getInstance().saveItems(ap);

		if (didDrop) {
			MessageUtil.privateMessage(p, "reward.relogged");
		}

		if (overflow) {
			MessageUtil.privateMessage(p, "reward.full_inventory");
		}
	}
}
