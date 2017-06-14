package net.urbanmc.ezauctions.object;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Auction {

	private UUID auctioneer;
	private ItemStack item;
	private int amount;
	private double starting, increment, autoBuy;
	private Bid lastBid;
	private boolean isSealed;
	private Map<UUID,Integer> bidders;

	public Auction(UUID auctioneer, ItemStack item, int amount, double starting, double increment, double autoBuy, boolean isSealed) {
		this.auctioneer = auctioneer;
		this.item = item;
		this.amount = amount;
		this.starting = starting;
		this.increment = increment;
		this.autoBuy = autoBuy;
		this.isSealed = isSealed;

		if(isSealed)
			bidders = new HashMap<>();

	}

	public UUID getAuctioneer() {
		return auctioneer;
	}

	public ItemStack getItem() {
		return item;
	}

	public int getAmount() {
		return amount;
	}

	public double getStartingPrice() {
		return starting;
	}

	public double getIncrement() {
		return increment;
	}

	public double getAutoBuy() {
		return autoBuy;
	}

	public Bid getLastBid() {
		return lastBid;
	}

	public void setLastBid(Bid lastBid) {
		this.lastBid = lastBid;
	}
}
