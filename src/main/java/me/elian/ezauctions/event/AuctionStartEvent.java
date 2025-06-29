package me.elian.ezauctions.event;

import me.elian.ezauctions.model.Auction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionStartEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final Auction auction;

	private boolean cancelled;

	public AuctionStartEvent(Auction auction) {
		super(true);
		this.auction = auction;
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
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
