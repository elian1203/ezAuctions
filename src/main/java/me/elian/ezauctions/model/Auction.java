package me.elian.ezauctions.model;

import com.google.inject.Inject;
import me.elian.ezauctions.controller.AuctionPlayerController;
import me.elian.ezauctions.controller.ConfigController;
import me.elian.ezauctions.controller.MessageController;
import me.elian.ezauctions.controller.ScoreboardController;
import me.elian.ezauctions.event.AuctionEndEvent;
import me.elian.ezauctions.event.AuctionStartEvent;
import me.elian.ezauctions.scheduler.CancellableTask;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class Auction implements Runnable {
	private final Plugin plugin;
	private final TaskScheduler scheduler;
	private final Economy economy;
	private final Permission permission;
	private final AuctionPlayerController playerController;
	private final ConfigController config;
	private final MessageController messages;
	private final ScoreboardController scoreboard;
	private final List<Integer> broadcastTimes;
	private AuctionData auctionData;
	private BidList bidList;
	private Runnable completedRunnable;
	private CancellableTask repeatingTask;

	private boolean started;
	private boolean running;
	private int remainingSeconds;
	private int antiSnipeRunTimes;

	@Inject
	public Auction(Plugin plugin, TaskScheduler scheduler, Economy economy, Permission permission,
	               AuctionPlayerController playerController, ConfigController config,
	               MessageController messages, ScoreboardController scoreboard) {
		this.plugin = plugin;
		this.scheduler = scheduler;
		this.economy = economy;
		this.permission = permission;
		this.playerController = playerController;
		this.config = config;
		this.messages = messages;
		this.scoreboard = scoreboard;
		broadcastTimes = config.getConfig().getIntegerList("auctions.broadcast-times");
	}

	public AuctionData getAuctionData() {
		return auctionData;
	}

	public BidList getBidList() {
		return bidList;
	}

	public int getRemainingSeconds() {
		return remainingSeconds;
	}

	public boolean isCompleted() {
		return !running;
	}

	public void startAuction(@NotNull AuctionData auctionData, @NotNull Runnable completedRunnable) {
		if (started) {
			throw new IllegalStateException("Can not start an auction that has already been started!");
		}

		started = true;
		running = true;

		this.auctionData = auctionData;
		this.completedRunnable = completedRunnable;

		AuctionStartEvent event = new AuctionStartEvent(this);
		plugin.getServer().getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			completedRunnable.run();
			return;
		}

		loadAuctionData(auctionData);
		messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
				this, false, "auction.info");
		scoreboard.createForAuction(this, playerController.getOnlinePlayers());
		repeatingTask = scheduler.runAsyncRepeatingTask(plugin, this, 1, 1);
	}

	public void cancelAuction(boolean returnMoney) {
		synchronized (this) {
			if (!running)
				return;

			cancelRepeatingTask();
			messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
					this, false, "auction.cancelled");
			auctionData.giveItemToPlayer(auctionData.getAuctioneer(), scheduler, config, messages);

			if (returnMoney) {
				returnStartPriceToAuctioneer();
			}

			returnBidderMoney(true);
		}
	}

	public void impoundAuction(@NotNull AuctionPlayer impoundingPlayer) {
		synchronized (this) {
			if (!running)
				return;

			cancelRepeatingTask();
			String impoundingPlayerName = impoundingPlayer.getOfflinePlayer().getName();
			if (impoundingPlayerName == null) {
				impoundingPlayerName = "";
			}

			messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
					this, false, "auction.impounded",
					Placeholder.unparsed("impoundingplayer", impoundingPlayerName));
			auctionData.giveItemToPlayer(impoundingPlayer, scheduler, config, messages);
			returnStartPriceToAuctioneer();
			returnBidderMoney(true);
		}
	}

	public void end() {
		synchronized (this) {
			if (!running)
				return;

			cancelRepeatingTask();
			handleAuctionTimeCompleted();
		}
	}

	public void checkAntiSnipe() {
		if (!running)
			return;

		if (!config.getConfig().getBoolean("antisnipe.enabled"))
			return;

		if (antiSnipeRunTimes >= config.getConfig().getInt("antisnipe.run-times"))
			return;

		if (remainingSeconds > config.getConfig().getInt("antisnipe.seconds-for-start"))
			return;

		antiSnipeRunTimes++;
		remainingSeconds += config.getConfig().getInt("antisnipe.time");
		messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
				this, true, "auction.antisnipe");
	}

	@Override
	public void run() {
		synchronized (this) {
			if (!running)
				return;

			remainingSeconds -= 1;
			scoreboard.updateForAuction(this);

			if (remainingSeconds == 0) {
				cancelRepeatingTask();
				handleAuctionTimeCompleted();

				return;
			}

			if (broadcastTimes.contains(remainingSeconds)) {
				messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
						this, false, "auction.time_left");
			}
		}
	}

	private void cancelRepeatingTask() {
		running = false;

		if (repeatingTask != null) {
			repeatingTask.cancel();
		}

		scoreboard.remove();
		completedRunnable.run();
	}

	private void loadAuctionData(AuctionData auctionData) {
		remainingSeconds = auctionData.getStartingAuctionTime();
		bidList = new BidList(this);
	}

	private void handleAuctionTimeCompleted() {
		Bid winningBid = bidList.getHighestBid();
		if (winningBid == null) {
			auctionData.giveItemToPlayer(auctionData.getAuctioneer(), scheduler, config, messages);
			messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
					this, false, "auction.finish.no_bids");
			messages.sendAuctionMessage(auctionData.getAuctioneer().getOnlinePlayer(), "reward.returned", this);
			return;
		}

		messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
				this, false, "auction.finish");

		if (auctionData.giveItemToPlayer(winningBid.auctionPlayer(), scheduler, config, messages)) {
			messages.sendAuctionMessage(winningBid.auctionPlayer().getOnlinePlayer(), "reward.received", this);
		}

		OfflinePlayer offlinePlayer = auctionData.getAuctioneer().getOfflinePlayer();
		double payout = winningBid.amount();
		double taxPercentage = 0D;

		if (!isTaxExempt(offlinePlayer)) {
			taxPercentage = config.getConfig().getDouble("auctions.fees.tax-percent");
		}

		double payoutPercentage = (100D - taxPercentage) / 100D;
		payout *= payoutPercentage;

		economy.depositPlayer(offlinePlayer, payout);

		messages.sendAuctionMessage(offlinePlayer.getPlayer(), "reward.money_given", this,
				Formatter.number("payout", payout),
				Formatter.number("taxpercentage", taxPercentage));

		returnBidderMoney(false);

		AuctionEndEvent event = new AuctionEndEvent(this);
		plugin.getServer().getPluginManager().callEvent(event);
	}

	private boolean isTaxExempt(OfflinePlayer offlinePlayer) {
		if (permission == null) {
			return offlinePlayer.getPlayer() != null
					&& offlinePlayer.getPlayer().hasPermission("ezauctions.taxexempt");
		}

		return permission.playerHas(auctionData.getWorld(), offlinePlayer, "ezauctions.taxexempt");
	}

	private void returnStartPriceToAuctioneer() {
		double startPrice = config.getConfig().getDouble("auction.fees.start-price");
		if (startPrice != 0) {
			economy.depositPlayer(auctionData.getAuctioneer().getOfflinePlayer(), startPrice);
		}
	}

	private void returnBidderMoney(boolean returnToHighestBidder) {
		Map<AuctionPlayer, Double> bidMap = bidList.getBidMap();

		if (bidMap.isEmpty())
			return;

		if (!returnToHighestBidder) {
			bidMap.remove(bidList.getHighestBid().auctionPlayer());
		}

		for (Map.Entry<AuctionPlayer, Double> entry : bidMap.entrySet()) {
			economy.depositPlayer(entry.getKey().getOfflinePlayer(), entry.getValue());
		}
	}
}
