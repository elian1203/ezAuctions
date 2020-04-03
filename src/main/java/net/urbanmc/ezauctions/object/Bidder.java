package net.urbanmc.ezauctions.object;

import net.urbanmc.ezauctions.EzAuctions;

public class Bidder {

	private AuctionsPlayer bidder;
	private double amount;
	private int timesBid = 0, consecutiveBids = 0;

	public Bidder(AuctionsPlayer bidder) {
		this.bidder = bidder;
	}

	public Bidder(AuctionsPlayer bidder, double amount) {
		this.bidder = bidder;
		this.amount = amount;
	}

	public AuctionsPlayer getBidder() {
		return bidder;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		getCurrentAuction().updateConsecutiveBidder(this, amount);
		this.amount = amount;
		timesBid++;
		consecutiveBids++;
	}

	public int getTimesBid() {
		return timesBid;
	}

	public int getConsecutiveBids() {
		return consecutiveBids;
	}

	public void resetConsecutiveBids() {
		consecutiveBids = 0;
	}

	private Auction getCurrentAuction() {
		return EzAuctions.getAuctionManager().getCurrentAuction();
	}
}
