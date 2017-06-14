package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class AuctionCancelEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	private Auction auction;
	private UUID canceller;

	public AuctionCancelEvent(Auction auction, UUID canceller) {
		this.auction = auction;
		this.canceller = canceller;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Auction getAuction() {
		return auction;
	}

	public UUID getCanceller() {
		return canceller;
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
