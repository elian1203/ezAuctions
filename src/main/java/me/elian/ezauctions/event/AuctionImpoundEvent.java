package me.elian.ezauctions.event;

import me.elian.ezauctions.model.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AuctionImpoundEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final Auction auction;
	private final Player impoundingPlayer;

	private boolean cancelled;

	public AuctionImpoundEvent(Auction auction, Player impoundingPlayer) {
		super(true);
		this.auction = auction;
		this.impoundingPlayer = impoundingPlayer;
	}

	public Auction getAuction() {
		return auction;
	}

	public Player getImpoundingPlayer() {
		return impoundingPlayer;
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
