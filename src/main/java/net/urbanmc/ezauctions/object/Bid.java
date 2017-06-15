package net.urbanmc.ezauctions.object;

public class Bid {

	private AuctionsPlayer bidder;
	private double amount;

	public Bid(AuctionsPlayer bidder, double amount) {
		this.bidder = bidder;
		this.amount = amount;
	}

	public AuctionsPlayer getBidder() {
		return bidder;
	}

	public double getAmount() {
		return amount;
	}
}
