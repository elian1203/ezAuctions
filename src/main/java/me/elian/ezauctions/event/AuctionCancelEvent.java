package me.elian.ezauctions.event;

import me.elian.ezauctions.model.Auction;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionCancelEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final Auction auction;
	private final CommandSender canceller;

	private boolean cancelled;

	public AuctionCancelEvent(Auction auction, CommandSender canceller) {
		super(true);
		this.auction = auction;
		this.canceller = canceller;
	}

	public Auction getAuction() {
		return auction;
	}

	public CommandSender getCanceller() {
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
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
