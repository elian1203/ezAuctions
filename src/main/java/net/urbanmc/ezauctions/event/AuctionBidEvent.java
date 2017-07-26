package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Bidder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionBidEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	private Auction auction;
	private Bidder bid;

	public AuctionBidEvent(Auction auction, Bidder bid) {
		this.auction = auction;
		this.bid = bid;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Auction getAuction() {
		return auction;
	}

	public Bidder getBid() {
		return bid;
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
