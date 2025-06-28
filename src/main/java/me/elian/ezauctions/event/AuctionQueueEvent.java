package me.elian.ezauctions.event;

import me.elian.ezauctions.model.AuctionData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionQueueEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final AuctionData auctionData;

	private boolean cancelled;

	public AuctionQueueEvent(AuctionData auctionData) {
		super(true);
		this.auctionData = auctionData;
	}

	public AuctionData getAuctionData() {
		return auctionData;
	}

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
