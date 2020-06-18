package net.urbanmc.ezauctions.object;

import java.util.Collection;

public class AuctionsPlayerList {

	private Collection<AuctionsPlayer> players;

	public AuctionsPlayerList(Collection<AuctionsPlayer> players) {
		this.players = players;
	}

	public Collection<AuctionsPlayer> getPlayers() {
		return players;
	}
}
