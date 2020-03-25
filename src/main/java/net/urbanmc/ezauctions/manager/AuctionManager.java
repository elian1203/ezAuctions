package net.urbanmc.ezauctions.manager;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionStartEvent;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionQueue;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.runnable.AuctionRunnable;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

public class AuctionManager {

	private EzAuctions plugin;

	private AuctionRunnable currentRunnable;
	private AuctionQueue queue;

	private boolean auctionsEnabled = true, inDelayedTask = false;

	public AuctionManager(EzAuctions plugin) {
		this.plugin = plugin;
		this.queue = new AuctionQueue(3);
	}

	public int getQueueSize() {
		return queue.size();
	}

	public boolean addToQueue(Auction auction) {
		if (getCurrentRunnable() == null && !inDelayedTask) {
			AuctionStartEvent event = new AuctionStartEvent(auction);
			Bukkit.getPluginManager().callEvent(event);

			if (!event.isCancelled()) {
				currentRunnable = new AuctionRunnable(auction, plugin);
			}

			return false;
		} else {
			queue.enqueue(auction);
			return true;
		}
	}

	public boolean inQueueOrCurrent(UUID auctioneer) {
		if (getCurrentAuction() != null && getCurrentAuction().getAuctioneer().getUniqueId().equals(auctioneer))
			return true;

		return queue.indexOf(auctioneer) != -1;
	}

	public int getPositionInQueue(AuctionsPlayer ap) {
		if (queue.isEmpty())
			return -1;

		int index = queue.indexOf(ap);

		return index == -1 ? index : index + 1;
	}

	public Auction removeFromQueue(UUID auctioneer) {
		int index = queue.indexOf(auctioneer);

		if (index == -1)
			return null;

		Auction auction = queue.get(index);

		queue.remove(index);

		return auction;
	}

	public void next() {
		currentRunnable = null;

		if (!isAuctionsEnabled())
			return;

		if (queue.isEmpty())
			return;

		Auction auction = queue.poll();

		AuctionStartEvent event = new AuctionStartEvent(auction);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isCancelled()) {
			long delay = 20 * ConfigManager.getConfig().getLong("general.time-between");

			inDelayedTask = true;

			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
				currentRunnable = new AuctionRunnable(auction, plugin);
				inDelayedTask = false;
			}, delay);
		}
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

	public void forEachAuctionInQueue(Consumer<Auction> consumer) {
		queue.forEach(consumer);
	}

	public Iterator<Auction> getQueue() {
		return queue.iterator();
	}

	public void disabling() {
		if (getCurrentRunnable() != null) {
			getCurrentRunnable().cancelAuction();
		}

		queue.forEach(RewardUtil::rewardCancel);

		queue.clear();
	}
}
