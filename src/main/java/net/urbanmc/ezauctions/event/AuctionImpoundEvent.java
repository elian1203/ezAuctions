package net.urbanmc.ezauctions.event;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class AuctionImpoundEvent extends AuctionEndEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;

	private Player impounder;

	public AuctionImpoundEvent(Auction auction, Player impounder) {
		super(auction);
		this.impounder = impounder;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Player getImpounder() {
		return impounder;
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
