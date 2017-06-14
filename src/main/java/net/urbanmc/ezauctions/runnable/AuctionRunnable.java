package net.urbanmc.ezauctions.runnable;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class AuctionRunnable extends BukkitRunnable {

	private Auction auction;
	private int timeLeft;
	private List<Integer> broadcastTimes = ConfigManager.getConfig().getIntegerList("auctions.broadcast-times");

	public AuctionRunnable(Auction auction, EzAuctions plugin) {
		this.auction = auction;
		this.timeLeft = auction.getAuctionTime();

		broadcastStart();
		runTaskTimer(plugin, 0, 20);
	}

	@Override
	public void run() {
		if (broadcastTimes.contains(timeLeft)) {
			String message = Messages.getString("auction.time_left", timeLeft);
			Bukkit.broadcastMessage(message);
		}

		if (timeLeft == 0) {
			// TODO: Manage money/items

			EzAuctions.getAuctionManager().next();

			cancel();
			return;
		}

		timeLeft--;
	}

	private void broadcastStart() {
		Bukkit.broadcastMessage(getStartingMessage());
	}

	public String getStartingMessage() {
		String auctioneerName = Bukkit.getOfflinePlayer(getAuction().getAuctioneer()).getName();

		String message = Messages.getString(
				"auction.starting",
				auctioneerName,
				getAuction().getAmount(),
				getAuction().getFormattedItem(),
				getAuction().getStartingPrice(),
				getAuction().getIncrement(),
				timeLeft);

		if (getAuction().getAutoBuy() > 0) {
			message += "\n" + Messages.getString("auction.autobuy", getAuction().getAutoBuy());
		}

		if (getAuction().isSealed()) {
			message += "\n" + Messages.getString("auction.sealed");
		}

		return message;
	}

	public Auction getAuction() {
		return auction;
	}

	public int getTimeLeft() {
		return timeLeft;
	}
}
