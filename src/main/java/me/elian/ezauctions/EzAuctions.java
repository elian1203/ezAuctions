package me.elian.ezauctions;

import co.aikar.commands.PaperCommandManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import me.elian.ezauctions.command.AuctionCommand;
import me.elian.ezauctions.command.BidCommand;
import me.elian.ezauctions.controller.*;
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
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EzAuctions extends JavaPlugin {
	private TaskScheduler scheduler;
	private Database database;
	private Metrics metrics;
	private AuctionController auctionController;
	private MessageController messageController;
	private ScoreboardController scoreboardController;
	private Injector injector;

	private static Class<? extends TaskScheduler> getSchedulerType() {
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
			return ThreadedRegionTaskScheduler.class;
		} catch (ClassNotFoundException e) {
			return BukkitTaskScheduler.class;
		}
	}

	private static void checkLatestVersionResponse(String response, Logger logger,
	                                               AuctionPlayerController playerController,
	                                               String serverVersionString) {
		try {
			InputStream inputStream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputStream);

			XPath xpath = XPathFactory.newInstance().newXPath();
			String latestVersionString = xpath.evaluate("/project/version", doc);
			var latestVersion = ModuleDescriptor.Version.parse(latestVersionString);
			var serverVersion = ModuleDescriptor.Version.parse(serverVersionString);

			if (serverVersion.compareTo(latestVersion) >= 0)
				return;

			logger.info("Version " + latestVersion + " is available! You are currently running " +
					"version " + serverVersion + ".");
			playerController.setUpdateAvailable();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error while checking latest release of plugin.", e);
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

		checkLatestVersion(injector);
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

	private void checkLatestVersion(Injector injector) {
		ConfigController config = injector.getInstance(ConfigController.class);
		if (!config.getConfig().getBoolean("general.check-updates"))
			return;

		AuctionPlayerController playerController = injector.getInstance(AuctionPlayerController.class);
		String serverVersion = getDescription().getVersion();

		String url = "https://raw.githubusercontent.com/elian1203/ezAuctions/main/pom.xml";
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();

		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.exceptionallyAsync(t -> {
					getLogger().log(Level.WARNING, "Error while checking latest release of plugin.", t);
					return null;
				})
				.thenAccept(response -> checkLatestVersionResponse(response, getLogger(), playerController,
						serverVersion));
	}
}
