package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionQueueEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	private Auction auction;

	public AuctionQueueEvent(Auction auction) {
		this.auction = auction;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Auction getAuction() {
		return auction;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
