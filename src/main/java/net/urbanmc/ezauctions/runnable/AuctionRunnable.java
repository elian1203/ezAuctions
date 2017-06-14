package net.urbanmc.ezauctions.runnable;

import mkremins.fanciful.FancyMessage;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionEndEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AuctionRunnable extends BukkitRunnable {

	private Auction auction;
	private int timeLeft;
	private List<Integer> broadcastTimes = ConfigManager.getConfig().getIntegerList("auctions.broadcast-times");

	public AuctionRunnable(Auction auction, EzAuctions plugin) {
		this.auction = auction;
		this.timeLeft = auction.getAuctionTime();

		long delay = 20 * ConfigManager.getConfig().getLong("general.time-between");

		broadcastStart();
		runTaskTimer(plugin, delay, 20);
	}

	@Override
	public void run() {
		if (broadcastTimes.contains(timeLeft)) {
			String message = Messages.getString("auction.time_left", timeLeft);
			Bukkit.broadcastMessage(message);
		}

		if (timeLeft == 0) {
			AuctionEndEvent event = new AuctionEndEvent(getAuction());
			Bukkit.getPluginManager().callEvent(event);

			// TODO: Manage money/items

			EzAuctions.getAuctionManager().next();

			cancel();
			return;
		}

		timeLeft--;
		getAuction().setAuctionTime(timeLeft);
	}

	private void broadcastStart() {
		List<CommandSender> recipients = new ArrayList<>(Bukkit.getOnlinePlayers());
		recipients.add(Bukkit.getConsoleSender());

		FancyMessage fancy = getAuction().getStartingMessage();

		fancy.send(recipients);
	}

	public Auction getAuction() {
		return auction;
	}

	public void cancelAuction() {
		cancel();
		EzAuctions.getAuctionManager().next();
	}
}
