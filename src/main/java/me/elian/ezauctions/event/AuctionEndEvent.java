package me.elian.ezauctions.event;

import me.elian.ezauctions.model.Auction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionEndEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Auction auction;

	private boolean cancelled;

	public AuctionEndEvent(Auction auction) {
		super(true);
		this.auction = auction;
	}

	public Auction getAuction() {
		return auction;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
