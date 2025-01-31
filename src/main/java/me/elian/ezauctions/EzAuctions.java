package me.elian.ezauctions;

import co.aikar.commands.PaperCommandManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.elian.ezauctions.command.AuctionCommand;
import me.elian.ezauctions.command.BidCommand;
import me.elian.ezauctions.controller.AuctionController;
import me.elian.ezauctions.controller.MessageController;
import me.elian.ezauctions.controller.ScoreboardController;
import me.elian.ezauctions.controller.UpdateController;
import me.elian.ezauctions.data.Database;
import me.elian.ezauctions.scheduler.BukkitTaskScheduler;
import me.elian.ezauctions.scheduler.TaskScheduler;
import me.elian.ezauctions.scheduler.ThreadedRegionTaskScheduler;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EzAuctions extends JavaPlugin {
	private TaskScheduler scheduler;
	private Database database;
	private Metrics metrics;
	private AuctionController auctionController;
	private MessageController messageController;
	private ScoreboardController scoreboardController;
	private UpdateController updateController;
	private Injector injector;

	private static Class<? extends TaskScheduler> getSchedulerType() {
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
			return ThreadedRegionTaskScheduler.class;
		} catch (ClassNotFoundException e) {
			return BukkitTaskScheduler.class;
		}
	}

	public Injector getInjector() {
		return injector;
	}

	@Override
	public void onEnable() {
		Economy economy = getEconomy();
		if (economy == null) {
			setEnabled(false);
			return;
		}

		Permission permission = getPermission();

		injector = createInjector(economy, permission, getSchedulerType());
		registerCommands(injector);

		scheduler = injector.getInstance(TaskScheduler.class);
		database = injector.getInstance(Database.class);
		auctionController = injector.getInstance(AuctionController.class);
		messageController = injector.getInstance(MessageController.class);
		scoreboardController = injector.getInstance(ScoreboardController.class);
		metrics = new Metrics(this, 985);

		updateController = injector.getInstance(UpdateController.class);
		updateController.checkForUpdates();

		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			injector.getInstance(EzAuctionsPlaceholderExpansion.class).register();
		}
	}

	@Override
	public void onDisable() {
		// scheduler must be the first to shut down to ensure no async tasks are created
		if (scheduler != null) {
			scheduler.shutdown();
		}

		if (auctionController != null) {
			auctionController.shutdown();
		}

		if (messageController != null) {
			messageController.shutdown();
		}

		if (scoreboardController != null) {
			scoreboardController.shutdown();
		}

		if (updateController != null) {
			updateController.shutdown();
		}

		// database must be shut down last in case saved items need to be added
		if (database != null) {
			database.shutdown();
		}

		if (metrics != null) {
			metrics.shutdown();
		}
	}

	private Economy getEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			getLogger().severe("Vault plugin not Installed! Disabling ezAuctions...");
			return null;
		}

		ServicesManager servicesManager = getServer().getServicesManager();
		RegisteredServiceProvider<Economy> rsp = servicesManager.getRegistration(Economy.class);
		if (rsp == null) {
			getLogger().severe("Economy provider plugin not found! " +
					"Make sure you have an economy provider plugin installed that supports Vault! " +
					"Disabling ezAuctions...");
			return null;
		}

		return rsp.getProvider();
	}

	private Permission getPermission() {
		ServicesManager servicesManager = getServer().getServicesManager();
		RegisteredServiceProvider<Permission> rsp = servicesManager.getRegistration(Permission.class);
		if (rsp == null)
			return null;

		return rsp.getProvider();
	}

	private Injector createInjector(Economy economy, Permission permission,
	                                Class<? extends TaskScheduler> schedulerClass) {
		Plugin plugin = this;
		PaperCommandManager commandManager = new PaperCommandManager(plugin);

		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Plugin.class).toInstance(plugin);
				bind(Economy.class).toInstance(economy);
				bind(Permission.class).toInstance(permission);
				bind(PaperCommandManager.class).toInstance(commandManager);
				bind(TaskScheduler.class).to(schedulerClass);
			}
		});
	}

	private void registerCommands(Injector injector) {
		AuctionCommand auctionCommand = injector.getInstance(AuctionCommand.class);
		BidCommand bidCommand = injector.getInstance(BidCommand.class);
		PaperCommandManager manager = injector.getInstance(PaperCommandManager.class);

		manager.registerCommand(auctionCommand);
		manager.registerCommand(bidCommand);
	}
}
