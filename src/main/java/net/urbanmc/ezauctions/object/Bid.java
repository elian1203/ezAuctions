package net.urbanmc.ezauctions.object;

import java.util.UUID;

public class Bid {

	private UUID bidder;
	private double amount;

	public Bid(UUID bidder, double amount) {
		this.bidder = bidder;
		this.amount = amount;
	}

	public UUID getBidder() {
		return bidder;
	}

	public double getAmount() {
		return amount;
	}
}
