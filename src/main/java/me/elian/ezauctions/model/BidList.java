package me.elian.ezauctions.model;

import java.util.*;

public class BidList {
	private final List<Bid> bids = new ArrayList<>();
	private final Auction auction;
	private Bid highestBid;

	public BidList(Auction auction) {
		this.auction = auction;
	}

	public Bid getHighestBid() {
		return highestBid;
	}

	public boolean hasNoBids() {
		return bids.isEmpty();
	}

	public void withSync(Runnable runnable) {
		synchronized (this) {
			runnable.run();
		}
	}

	public double getMinimumRequiredBid(AuctionPlayer auctionPlayer) {
		// if not sealed, simply return the highest bid + increment or starting price if no bids
		if (!auction.getAuctionData().isSealed()) {
			return bids.isEmpty() ? auction.getAuctionData().getStartingPrice() :
					highestBid.amount() + auction.getAuctionData().getIncrementPrice();
		}

		// if sealed, return starting price or player's highest bid + increment if they already bid
		Bid highestBidForPlayer = getHighestBidForPlayer(auctionPlayer);

		return highestBidForPlayer == null ? auction.getAuctionData().getStartingPrice() :
				highestBidForPlayer.amount() + auction.getAuctionData().getIncrementPrice();
	}

	public Bid getHighestBidForPlayer(AuctionPlayer auctionPlayer) {
		Bid highestBidForPlayer = null;

		for (Bid bid : bids) {
			if (!bid.auctionPlayer().getUniqueId().equals(auctionPlayer.getUniqueId()))
				continue;

			if (highestBidForPlayer == null) {
				highestBidForPlayer = bid;
				continue;
			}

			if (highestBidForPlayer.amount() < bid.amount()) {
				highestBidForPlayer = bid;
			}
		}

		return highestBidForPlayer;
	}

	public int getNumberOfBids(AuctionPlayer auctionPlayer) {
		int count = 0;
		for (Bid bid : bids) {
			if (bid.auctionPlayer().getUniqueId().equals(auctionPlayer.getUniqueId())) {
				count++;
			}
		}

		return count;
	}

	public int getConsecutiveBids(AuctionPlayer auctionPlayer) {
		int bidIndex = bids.size() - 1;
		int consecutiveBids = 0;

		while (bidIndex >= 0 && bids.get(bidIndex).auctionPlayer().getUniqueId().equals(auctionPlayer.getUniqueId())) {
			bidIndex--;
			consecutiveBids++;
		}

		return consecutiveBids;
	}

	public void placeBid(Bid bid) {
		bids.add(bid);

		if (highestBid == null || bid.amount() > highestBid.amount()) {
			highestBid = bid;
		}

		double autoBuyPrice = auction.getAuctionData().getAutoBuyPrice();
		if (autoBuyPrice != 0 && bid.amount() >= autoBuyPrice) {
			auction.end();
		}

		auction.checkAntiSnipe();
	}

	public boolean playerHasAnyBids(UUID id) {
		for (Bid bid : bids) {
			if (bid.auctionPlayer().getUniqueId().equals(id))
				return true;
		}

		return false;
	}

	public Map<AuctionPlayer, Double> getBidMap() {
		Map<AuctionPlayer, Double> map = new HashMap<>();

		for (Bid bid : bids) {
			double current = map.getOrDefault(bid.auctionPlayer(), 0D);
			if (bid.amount() > current) {
				map.put(bid.auctionPlayer(), bid.amount());
			}
		}

		return map;
	}
}
