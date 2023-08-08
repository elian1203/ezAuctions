package me.elian.ezauctions.event;

import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.Bid;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionBidEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final Auction auction;
	private final Bid bid;

	private boolean cancelled;

	public AuctionBidEvent(Auction auction, Bid bid) {
		super(true);
		this.auction = auction;
		this.bid = bid;
	}

	public Auction getAuction() {
		return auction;
	}

	public Bid getBid() {
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
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
