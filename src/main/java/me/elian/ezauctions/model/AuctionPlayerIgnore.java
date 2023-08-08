package me.elian.ezauctions.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

@DatabaseTable(tableName = "ezAuctions_AuctionPlayerIgnore")
public class AuctionPlayerIgnore {
	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(foreign = true)
	private AuctionPlayer auctionPlayer;

	@DatabaseField
	private UUID ignored;

	public AuctionPlayerIgnore() {
	}

	public AuctionPlayerIgnore(AuctionPlayer auctionPlayer, UUID ignored) {
		this.auctionPlayer = auctionPlayer;
		this.ignored = ignored;
	}

	public AuctionPlayer getAuctionPlayer() {
		return auctionPlayer;
	}

	public UUID getIgnored() {
		return ignored;
	}
}
