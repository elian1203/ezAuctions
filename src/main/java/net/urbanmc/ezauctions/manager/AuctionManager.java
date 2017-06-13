package net.urbanmc.ezauctions.manager;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.runnable.AuctionRunnable;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {

	private EzAuctions plugin;

	private AuctionRunnable currentRunnable;
	private List<Auction> queue;

	private boolean auctionsEnabled = true;

	public AuctionManager(EzAuctions plugin) {
		this.plugin = plugin;
		this.queue = new ArrayList<>();
	}

	public void addToQueue(Auction auction) {
		if (getCurrentRunnable() == null) {
			currentRunnable = new AuctionRunnable(auction, plugin);
		} else {
			queue.add(auction);
		}
	}

	public void next() {
		if (!isAuctionsEnabled())
			return;

		Auction auction = queue.get(0);

		if (auction == null)
			return;

		queue.remove(0);

		currentRunnable = new AuctionRunnable(auction, plugin);
	}

	public AuctionRunnable getCurrentRunnable() {
		return currentRunnable;
	}

	public Auction getCurrentAuction() {
		return getCurrentRunnable().getAuction();
	}

	public boolean isAuctionsEnabled() {
		return auctionsEnabled;
	}

	public void setAuctionsEnabled(boolean auctionsEnabled) {
		this.auctionsEnabled = auctionsEnabled;
	}
}
