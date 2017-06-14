package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class AuctionCancelEvent extends AuctionEndEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	private CommandSender canceller;

	public AuctionCancelEvent(Auction auction, CommandSender canceller) {
		super(auction);
		this.canceller = canceller;
	}

	public static HandlerList getHandlerList() {
		return handlers;
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
	public HandlerList getHandlers() {
		return handlers;
	}
}
