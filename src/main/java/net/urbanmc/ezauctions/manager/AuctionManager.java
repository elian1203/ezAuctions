package net.urbanmc.ezauctions.manager;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.runnable.AuctionRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

	private EzAuctions plugin;

	private AuctionRunnable currentRunnable;
	private List<Auction> queue;

	private boolean auctionsEnabled = true;

	public AuctionManager(EzAuctions plugin) {
		this.plugin = plugin;
		this.queue = new ArrayList<>();
	}

	public int getQueueSize() {
		return queue.size();
	}

	public void addToQueue(Auction auction) {
		if (getCurrentRunnable() == null) {
			currentRunnable = new AuctionRunnable(auction, plugin);
		} else {
			queue.add(auction);
		}
	}

	public Auction removeFromQueue(UUID auctioneer) {
		Auction auction = null;

		for (Auction auc : queue) {
			if (auc.getAuctioneer().equals(auctioneer)) {
				auction = auc;
				break;
			}
		}

		if (auction != null) {
			queue.remove(auction);
		}

		return auction;
	}

	public void next() {
		currentRunnable = null;

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
		if (getCurrentRunnable() == null)
			return null;

		return getCurrentRunnable().getAuction();
	}

	public boolean isAuctionsEnabled() {
		return auctionsEnabled;
	}

	public void setAuctionsEnabled(boolean auctionsEnabled) {
		this.auctionsEnabled = auctionsEnabled;
	}
}
