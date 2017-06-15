package net.urbanmc.ezauctions.object;

import java.util.List;

public class AuctionsPlayerList {

	private List<AuctionsPlayer> players;

	public AuctionsPlayerList(List<AuctionsPlayer> players) {
		this.players = players;
	}

	public List<AuctionsPlayer> getPlayers() {
		return players;
	}
}
