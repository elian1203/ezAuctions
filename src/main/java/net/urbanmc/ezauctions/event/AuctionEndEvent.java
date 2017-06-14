package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionEndEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private Auction auction;

	public AuctionEndEvent(Auction auction) {
		this.auction = auction;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Auction getAuction() {
		return auction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
