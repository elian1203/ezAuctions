package me.elian.ezauctions.controller;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.data.Database;
import me.elian.ezauctions.helper.ItemHelper;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.model.SavedItem;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class AuctionPlayerController implements Listener {
	private final Logger logger;
	private final Database database;
	private final TaskScheduler scheduler;
	private final ConfigController config;
	private final MessageController messages;
	private final ScoreboardController scoreboard;
	private final Set<AuctionPlayer> onlinePlayers = Sets.newConcurrentHashSet();

	@Inject
	public AuctionPlayerController(Plugin plugin, Logger logger, Database database, TaskScheduler scheduler,
	                               ConfigController config, MessageController messages,
	                               ScoreboardController scoreboard) {
		this.logger = logger;
		this.database = database;
		this.scheduler = scheduler;
		this.config = config;
		this.messages = messages;
		this.scoreboard = scoreboard;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

		scheduler.runAsyncTask(() -> {
			for (Player player : plugin.getServer().getOnlinePlayers()) {
				database.getAuctionPlayer(player.getUniqueId()).thenAccept(onlinePlayers::add);
			}
		});
	}

	public @NotNull CompletableFuture<AuctionPlayer> getPlayer(@NotNull Player player) {
		return getPlayer(player.getUniqueId());
	}

	public @NotNull CompletableFuture<AuctionPlayer> getPlayer(@NotNull UUID id) {
		for (AuctionPlayer ap : onlinePlayers) {
			if (ap.getUniqueId().equals(id))
				return CompletableFuture.completedFuture(ap);
		}

		return database.getAuctionPlayer(id);
	}

	/**
	 * For instances where a new AuctionPlayer record is required from a fresh connection to the database
	 * @param id Player UUID
	 * @return CompletableFuture for AuctionPlayer record
	 */
	public @NotNull CompletableFuture<AuctionPlayer> getPlayerFromDatabase(@NotNull UUID id) {
		return database.getAuctionPlayer(id);
	}

	public void savePlayer(@NotNull AuctionPlayer auctionPlayer) {
		scheduler.runAsyncTask(() -> database.saveAuctionPlayer(auctionPlayer));
	}

	public @NotNull Set<AuctionPlayer> getOnlinePlayers() {
		return Collections.unmodifiableSet(onlinePlayers);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		scheduler.runAsyncTask(() -> {
			Player p = e.getPlayer();
			UUID id = p.getUniqueId();

			database.getAuctionPlayer(id).thenAccept(ap -> {
				onlinePlayers.add(ap);
				scoreboard.addPlayer(p);
				returnSavedItems(ap);
			});
		});
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		UUID id = e.getPlayer().getUniqueId();

		for (AuctionPlayer ap : onlinePlayers) {
			if (ap.getUniqueId().equals(id)) {
				onlinePlayers.remove(ap);
				break;
			}
		}
	}

	// run reward offline when player changes worlds as well
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
		scheduler.runAsyncTask(() -> {
			Player player = e.getPlayer();
			getPlayerFromDatabase(player.getUniqueId()).thenAccept(this::returnSavedItems);
		});
	}

	private void returnSavedItems(AuctionPlayer ap) {
		if (ap.getSavedItems() == null || ap.getSavedItems().isEmpty())
			return;

		Player p = ap.getOnlinePlayer();

		if (p == null)
			return;

		scheduler.runPlayerRegionTask(() -> {
			List<SavedItem> returnedItems = new ArrayList<>();

			boolean overflow = false;

			String playerWorld = p.getWorld().getName();
			if (config.getConfig().getStringList("auctions.blocked-worlds")
					.stream().anyMatch(blockedWorld -> blockedWorld.equals(playerWorld))) {
				messages.sendMessage(p, "reward.relogged_blocked_world");
				return;
			}

			for (SavedItem savedItem : ap.getSavedItems()) {
				if (config.getConfig().getBoolean("auctions.per-world-auctions")
						&& !playerWorld.equals(savedItem.getWorld())) {
					messages.sendMessage(p, "reward.relogged_wrong_world",
							Placeholder.unparsed("itemworld", savedItem.getWorld()));
					continue;
				}

				try {
					ItemStack item = savedItem.getItemStack();
					boolean itemOverflowed = ItemHelper.addItemToPlayerInventory(p, item, savedItem.getAmount());
					if (itemOverflowed) {
						overflow = true;
					}
				} catch (IOException e) {
					logger.severe("Error occured while deserializing item stack! " +
							"Player item not returned! " + savedItem.getSerializedItemJson(), e);
				}

				returnedItems.add(savedItem);
			}

			if (returnedItems.size() == 0)
				return;

			scheduler.runAsyncTask(() -> ap.getSavedItems().removeAll(returnedItems));

			messages.sendMessage(p, "reward.relogged");

			if (overflow) {
				messages.sendMessage(p, "reward.full_inventory");
			}
		}, p);
	}
}
