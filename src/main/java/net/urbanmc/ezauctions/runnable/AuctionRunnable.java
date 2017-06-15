package net.urbanmc.ezauctions.runnable;

import mkremins.fanciful.FancyMessage;
import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionEndEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Bid;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AuctionRunnable extends BukkitRunnable {

	private Auction auction;
	private int timeLeft;
	private List<Integer> broadcastTimes = ConfigManager.getConfig().getIntegerList("auctions.broadcast-times");

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
			String message = Messages.getString("auction.time_left", timeLeft);
			Bukkit.broadcastMessage(message);
		}

		if (timeLeft == 0) {
			cancel();

			AuctionEndEvent event = new AuctionEndEvent(getAuction());
			Bukkit.getPluginManager().callEvent(event);

			Bid lastBid = getAuction().getLastBid();

			String lastBidderName = lastBid.getBidder().getOfflinePlayer().getName();
			double lastBidAmount = lastBid.getAmount();

			Bukkit.broadcastMessage(Messages.getString("auction.finish", lastBidderName, lastBidAmount));

			EzAuctions.getAuctionManager().next();

			Auction current = getAuction();
			Economy econ = EzAuctions.getEcon();

			RewardUtil.rewardAuction(current, econ);
			RewardUtil.returnLosingBidders(current, econ);

			return;
		}

		timeLeft--;
		getAuction().setAuctionTime(timeLeft);
	}

	private void broadcastStart() {
		List<CommandSender> recipients = new ArrayList<>(Bukkit.getOnlinePlayers());
		recipients.add(Bukkit.getConsoleSender());

		FancyMessage fancy = getAuction().getStartingMessage();

		fancy.send(recipients);
	}

	public Auction getAuction() {
		return auction;
	}

	public void cancelAuction() {
		cancel();

		Bukkit.broadcastMessage(Messages.getString("auction.cancelled"));
		EzAuctions.getAuctionManager().next();

		RewardUtil.rewardCancel(getAuction());
	}

	public void impoundAuction(Player impounder) {
		cancel();

		Bukkit.broadcastMessage(Messages.getString("auction.impounded"));
		EzAuctions.getAuctionManager().next();

		RewardUtil.rewardImpound(getAuction(), impounder);
	}
}
