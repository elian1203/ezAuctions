package net.urbanmc.ezauctions.runnable;

import mkremins.fanciful.FancyMessage;
import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionEndEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Bid;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class AuctionRunnable extends BukkitRunnable {

	private Auction auction;
	private int timeLeft;
	private List<Integer> broadcastTimes = ConfigManager.getConfig().getIntegerList("auctions.broadcast-times");
	private int antiSnipeTimesRun = 0;

	public AuctionRunnable(Auction auction, EzAuctions plugin) {
		this.auction = auction;
		this.timeLeft = auction.getAuctionTime();

		long delay = 20 * ConfigManager.getConfig().getLong("general.time-between");

		broadcastStart();
		runTaskTimer(plugin, delay, 20);
	}

	@Override
	public void run() {
		if (broadcastTimes.contains(timeLeft)) {
			MessageUtil.broadcastRegular("auction.time_left", timeLeft);
		}

		if (timeLeft == 0) {
			cancel();

			AuctionEndEvent event = new AuctionEndEvent(getAuction());
			Bukkit.getPluginManager().callEvent(event);

			EzAuctions.getAuctionManager().next();

			if (getAuction().anyBids()) {
				Auction current = getAuction();
				Economy econ = EzAuctions.getEcon();

				Bid lastBid = getAuction().getLastBid();

				String lastBidderName = lastBid.getBidder().getOfflinePlayer().getName();
				double lastBidAmount = lastBid.getAmount();

				MessageUtil.broadcastRegular("auction.finish", lastBidderName, lastBidAmount);

				RewardUtil.rewardAuction(current, econ);
				RewardUtil.returnLosingBidders(current, econ);
			} else {
				MessageUtil.broadcastRegular("auction.finish.no_bids");
				RewardUtil.rewardCancel(getAuction());
			}

			return;
		}

		timeLeft--;
		getAuction().setAuctionTime(timeLeft);
	}

	private void broadcastStart() {
		FancyMessage fancy = getAuction().getStartingMessage();

		MessageUtil.broadcastRegular(fancy);
	}

	public Auction getAuction() {
		return auction;
	}

	public int getAntiSnipeTimesRun() {
		return antiSnipeTimesRun;
	}

	public void antiSnipe() {
		FileConfiguration data = ConfigManager.getConfig();

		if (!(getAntiSnipeTimesRun() < data.getInt("antisnipe.run-times")))
			return;

		int addTime = data.getInt("antisnipe.time");

		MessageUtil.broadcastSpammy("auction.antisnipe", addTime);
		timeLeft += addTime;

		antiSnipeTimesRun++;
	}

	public void cancelAuction() {
		cancel();

		MessageUtil.broadcastRegular("auction.cancelled");
		EzAuctions.getAuctionManager().next();

		RewardUtil.rewardCancel(getAuction());
	}

	public void impoundAuction(Player impounder) {
		cancel();

		MessageUtil.broadcastRegular("auction.impounded");
		EzAuctions.getAuctionManager().next();

		RewardUtil.rewardImpound(getAuction(), impounder);
	}
}
