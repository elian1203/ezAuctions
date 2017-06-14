package net.urbanmc.ezauctions.object;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Auction {

	private UUID auctioneer;
	private ItemStack item;
	private int amount, auctionTime;
	private double starting, increment, autoBuy;
	private Bid lastBid;
	private boolean isSealed;
	private Map<UUID, Integer> bidders;

	public Auction(UUID auctioneer, ItemStack item, int amount, int auctionTime, double starting, double increment,
	               double autoBuy, boolean isSealed) {
		this.auctioneer = auctioneer;
		this.item = item;
		this.amount = amount;
		this.auctionTime = auctionTime;
		this.starting = starting;
		this.increment = increment;
		this.autoBuy = autoBuy;
		this.isSealed = isSealed;

		if (isSealed)
			bidders = new HashMap<>();

	}

	public UUID getAuctioneer() {
		return auctioneer;
	}

	public ItemStack getItem() {
		return item;
	}

	public String getFormattedItem() {
		// TODO: Fancy message with hover
		return null;
	}

	public int getAmount() {
		return amount;
	}

	public int getAuctionTime() {
		return auctionTime;
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

	public boolean isSealed() {
		return isSealed;
	}

	public Bid getLastBid() {
		return lastBid;
	}

	public void setLastBid(Bid lastBid) {
		this.lastBid = lastBid;
	}
}
