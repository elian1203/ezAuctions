package net.urbanmc.ezauctions.object;


import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {

	private UUID auctioneer;
	private ItemStack item;
	private int amount;
	private double starting, increment, autoBuy;
	private Bid lastBid;

	public Auction(UUID auctioneer, ItemStack item, int amount, double starting, double increment, double autoBuy) {
		this.auctioneer = auctioneer;
		this.item = item;
		this.amount = amount;
		this.starting = starting;
		this.increment = increment;
		this.autoBuy = autoBuy;
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
