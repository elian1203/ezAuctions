package net.urbanmc.ezauctions.runnable;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.scheduler.BukkitRunnable;

public class AuctionRunnable extends BukkitRunnable {

	private Auction auction;

	private int timeLeft = 60; // TODO: Add to config

	public AuctionRunnable(Auction auction, EzAuctions plugin) {
		this.auction = auction;

		runTaskTimer(plugin, 0, 20);
	}

	@Override
	public void run() {
		// TODO: Broadcast message

		if (timeLeft == 0) {
			// TODO: Manage money/items
			EzAuctions.getAuctionManager().next();
		}
	}

	public Auction getAuction() {
		return auction;
	}

	public int getTimeLeft() {
		return timeLeft;
	}
}
