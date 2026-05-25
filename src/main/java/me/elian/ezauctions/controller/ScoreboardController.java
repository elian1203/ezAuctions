package me.elian.ezauctions.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@Singleton
public class ScoreboardController implements Listener {
	private final TaskScheduler scheduler;
	private final ConfigController config;
	private final MessageController messages;
	private ScoreboardLibrary scoreboardLibrary;
	private Sidebar sidebar;

	@Inject
	public ScoreboardController(TaskScheduler scheduler, ConfigController config, MessageController messages,
	                            Plugin plugin, Logger logger) {
		this.scheduler = scheduler;
		this.config = config;
		this.messages = messages;

		try {
			// force scoreboardlib to load even when plugin not using latest library version
			String property = "net.mega".concat("vex.scoreboardlibrary.forceModern");
			System.setProperty(property, "true");
			scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(plugin);
		} catch (NoPacketAdapterAvailableException e) {
			scoreboardLibrary = new NoopScoreboardLibrary();
			logger.severe("Scoreboard functionality will not be visible due to server version not being " +
					"supported. If you are running the latest ezAuctions, please report this issue on GitHub.", e);
		} catch (RuntimeException e) {
			scoreboardLibrary = new NoopScoreboardLibrary();
			logger.info("Scoreboard functionality will not be visible due to error with plugin build. " +
					"This is normal when run in a test.");
		}

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void createForAuction(@NotNull Auction auction, @NotNull Set<AuctionPlayer> onlinePlayers) {
		if (!config.getConfig().getBoolean("scoreboard.enabled"))
			return;

		scheduler.runSyncTask(() -> {
			createSidebar(onlinePlayers);
			updateForAuction(auction);
		});
	}

	public void updateForAuction(@NotNull Auction auction) {
		scheduler.runSyncTask(() -> {
			if (sidebar == null)
				return;

			List<Component> title = messages.getAuctionComponentLines("auction.scoreboard.title", auction);
			List<Component> lines = messages.getAuctionComponentLines("auction.scoreboard.body", auction);

			SidebarComponent.Builder builder = SidebarComponent.builder();

			for (Component component : lines) {
				builder.addStaticLine(component);
			}

			Component titleComponent = title.size() == 0 ? Component.empty() : title.get(0);
			SidebarComponent linesSidebarComponent = builder.build();
			ComponentSidebarLayout componentSidebar =
					new ComponentSidebarLayout(SidebarComponent.staticLine(titleComponent), linesSidebarComponent);

			componentSidebar.apply(sidebar);
		});
	}

	public void addPlayer(@NotNull Player player) {
		scheduler.runSyncTask(() -> {
			if (sidebar != null) {
				sidebar.addPlayer(player);
			}
		});
	}

	public void removePlayer(@NotNull Player player) {
		scheduler.runSyncTask(() -> {
			if (sidebar != null) {
				sidebar.removePlayer(player);
			}
		});
	}

	public void remove() {
		scheduler.runSyncTask(() -> {
			if (sidebar == null)
				return;

			sidebar.close();
			sidebar = null;
		});
	}

	public void shutdown() {
		scoreboardLibrary.close();
	}

	private void createSidebar(@NotNull Set<AuctionPlayer> onlinePlayers) {
		sidebar = scoreboardLibrary.createSidebar();

		for (AuctionPlayer auctionPlayer : onlinePlayers) {
			Player player = auctionPlayer.getOnlinePlayer();
			if (!auctionPlayer.isIgnoringScoreboard() && player != null) {
				sidebar.addPlayer(player);
			}
		}
	}
}
