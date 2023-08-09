package me.elian.ezauctions.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.AuctionData;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.scheduler.TaskScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
public class AuctionController implements Listener {
	private final Logger logger;
	private final TaskScheduler scheduler;
	private final ConfigController config;
	private final MessageController messages;
	private final Provider<Auction> auctionProvider;
	private final Queue<AuctionData> auctionQueue = new LinkedList<>();
	private final Map<UUID, Long> queueCooldown = new HashMap<>();

	private boolean auctionsEnabled = true;
	private Auction activeAuction;
	private long lastAuctionEndTimeMillis;

	@Inject
	public AuctionController(Plugin plugin, Logger logger, TaskScheduler scheduler, ConfigController config,
	                         MessageController messages, Provider<Auction> auctionProvider) {
		this.logger = logger;
		this.scheduler = scheduler;
		this.config = config;
		this.messages = messages;
		this.auctionProvider = auctionProvider;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public boolean isAuctionsEnabled() {
		return auctionsEnabled;
	}

	public void setAuctionsEnabled(boolean auctionsEnabled) {
		this.auctionsEnabled = auctionsEnabled;
	}

	public void withSync(Runnable runnable) {
		synchronized (this) {
			runnable.run();
		}
	}

	public @NotNull Collection<AuctionData> getAuctionQueue() {
		return Collections.unmodifiableCollection(auctionQueue);
	}

	public boolean hasActiveAuction() {
		return activeAuction != null;
	}

	public @Nullable Auction getActiveAuction() {
		return activeAuction;
	}

	public long getCooldownTime(@NotNull UUID uniqueId) {
		return queueCooldown.getOrDefault(uniqueId, 0L);
	}

	public boolean hasCooldown(@NotNull UUID uniqueId) {
		if (!queueCooldown.containsKey(uniqueId))
			return false;

		long time = queueCooldown.get(uniqueId);
		long timeSince = System.currentTimeMillis() - time;
		long cooldown = config.getConfig().getLong("general.queue-cooldown-time");
		if (timeSince >= cooldown) {
			queueCooldown.remove(uniqueId);
			return false;
		}

		return false;
	}

	public void setCooldown(@NotNull UUID uniqueId) {
		queueCooldown.put(uniqueId, System.currentTimeMillis());
	}

	public int getPositionInQueue(@NotNull AuctionData auctionData) {
		int position = 1;
		for (AuctionData data : auctionQueue) {
			if (data == auctionData)
				return position;

			position++;
		}

		return 0;
	}

	public @Nullable AuctionData removeFirstItemFromQueue(@NotNull AuctionPlayer auctionPlayer) {
		for (AuctionData data : auctionQueue) {
			if (data.getAuctioneer().getUniqueId().equals(auctionPlayer.getUniqueId())) {
				auctionQueue.remove(data);
				logItemMessage(data, "Item removed from auction queue. Auctioneer: %s Amount: %d Item: %s NBT: %s");
				return data;
			}
		}

		return null;
	}

	@EventHandler
	public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
		UUID id = e.getPlayer().getUniqueId();

		Auction activeAuction = getActiveAuction();
		if (activeAuction == null)
			return;

		if (!activeAuction.getAuctionData().getAuctioneer().getUniqueId().equals(id)
				&& !activeAuction.getBidList().playerHasAnyBids(id))
			return;

		String command = e.getMessage().split(" ")[0];

		for (String blockedCommand : config.getConfig().getStringList("auctions.blocked-commands")) {
			if (command.equalsIgnoreCase("/" + blockedCommand)) {
				e.setCancelled(true);
				messages.sendAuctionMessage(e.getPlayer(), "auction.blocked_command", activeAuction);

				return;
			}
		}
	}

	/***
	 * Queues the auction to run
	 * @param auctionData the auction data associated with the auction
	 * @return true if queued, false if executing immediately
	 */
	public boolean queueAuction(@NotNull AuctionData auctionData) {
		if (hasActiveAuction() || auctionQueue.size() != 0) {
			auctionQueue.add(auctionData);
			logItemMessage(auctionData,
					"Item added to auction queue. Auctioneer: %s Amount: %d Item: %s NBT: %s");
			return true;
		}

		int delay = config.getConfig().getInt("general.time-between");
		long timeSinceLastAuction = System.currentTimeMillis() - lastAuctionEndTimeMillis;
		if (timeSinceLastAuction < delay * 1000L) {
			auctionQueue.add(auctionData);
			logItemMessage(auctionData,
					"Item added to auction queue. Auctioneer: %s Amount: %d Item: %s NBT: %s");
			pullNextAuctionFromQueue();
			return true;
		}

		Auction auction = auctionProvider.get();
		activeAuction = auction;
		auction.startAuction(auctionData, this::handleAuctionCompleted);
		logItemMessage(auctionData, "Item starting in auction. Auctioneer: %s Amount: %d Item: %s NBT: %s");
		return false;
	}

	public void shutdown() {
		for (AuctionData queued : auctionQueue) {
			queued.giveItemToPlayer(queued.getAuctioneer(), scheduler, config, messages);
		}

		auctionQueue.clear();

		Auction activeAuction = getActiveAuction();
		if (activeAuction != null) {
			activeAuction.cancelAuction(true);
		}
	}

	private void handleAuctionCompleted() {
		activeAuction = null;
		lastAuctionEndTimeMillis = System.currentTimeMillis();
		withSync(this::pullNextAuctionFromQueue);
	}

	private void pullNextAuctionFromQueue() {
		if (auctionQueue.isEmpty())
			return;

		int delay = config.getConfig().getInt("general.time-between");
		if (lastAuctionEndTimeMillis != 0) {
			long timeSinceLastAuction = (System.currentTimeMillis() - lastAuctionEndTimeMillis) / 1000;
			if (timeSinceLastAuction < delay) {
				delay -= timeSinceLastAuction;
			}
		}

		scheduler.runAsyncDelayedTask(() -> withSync(this::startNextAuctionFromQueue), delay);
	}

	private void startNextAuctionFromQueue() {
		if (auctionQueue.isEmpty())
			return;

		AuctionData nextAuctionData = auctionQueue.remove();
		Auction nextAuction = auctionProvider.get();
		activeAuction = nextAuction;
		nextAuction.startAuction(nextAuctionData, this::handleAuctionCompleted);
		logItemMessage(nextAuctionData, "Item starting in auction. Auctioneer: %s Amount: %d Item: %s NBT: %s");
	}

	private void logItemMessage(AuctionData data, String message) {
		if (!config.getConfig().getBoolean("general.log-items-to-console"))
			return;

		logger.info(String.format(
				message,
				data.getAuctioneer().getOfflinePlayer().getName(),
				data.getAmount(),
				data.getItem().getType(),
				data.getItemNbt()));
	}
}
