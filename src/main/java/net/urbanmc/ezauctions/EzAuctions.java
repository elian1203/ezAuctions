package net.urbanmc.ezauctions;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import co.aikar.locales.MessageKey;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.command.BidCommand;
import net.urbanmc.ezauctions.datastorage.DataSource;
import net.urbanmc.ezauctions.listener.CommandListener;
import net.urbanmc.ezauctions.listener.JoinListener;
import net.urbanmc.ezauctions.listener.WorldChangeListener;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;

public class EzAuctions extends JavaPlugin {

	private static AuctionManager auctionManager;
	private static Economy econ;
	private static Permission perms;

	private static boolean updateAvailable = false;

	private static File dataDir;
	private static Logger pluginLogger;

	public static AuctionManager getAuctionManager() {
		return auctionManager;
	}

	public static Economy getEcon() {
		return econ;
	}

	public static boolean isUpdateAvailable() {
		return updateAvailable;
	}

	@Override
	public void onLoad() {
		// check if there are players during plugin load
		// if there are players online, the plugin was likely reloaded or loaded by an external plugin
		// THIS IS NOT SUPPORTED

		if (!Bukkit.getOnlinePlayers().isEmpty()) {
			getLogger().warning("-------------------------------------------");
			getLogger().warning("WARNING: We have detected that you have loaded the plugin using /reload or an " +
					"external plugin! This is NOT supported.  Doing this will likely result in errors during plugin " +
					"operation. Please do a full server restart for ezAuctions to be properly loaded. " +
					"If you have restarted the server completely, you may ignore this message.");
			getLogger().warning("-------------------------------------------");
		}
	}

	@Override
	public void onEnable() {
		if (!setupEconomy()) {
			getLogger().severe("Vault not detected! Is Vault installed along with a supported economy provider? " +
					"Disabling plugin...");
			setEnabled(false);

			return;
		}

		setupPerms();

		// Set the data directory.
		// Note: This must be set before anything else (config, data source, etc) is called!!
		dataDir = getDataFolder();

		// Set logger
		pluginLogger = getLogger();

		// Establish the data source:
		// We do this here because we need to inject the plugin into the data source
		// and we also have to make sure the config has already loaded.
		DataSource dataSource = DataSource.determineDataSource(this);

		// Check access to the data source
		if (dataSource == null || !dataSource.testAccess()) {
			getLogger().severe("Could not load auction player data properly! Please check above messages for more " +
					"detail.");
			setEnabled(false);
			return;
		}

		AuctionsPlayerManager.getInstance().setDataSource(dataSource);
		AuctionsPlayerManager.getInstance().loadData();

		registerListeners();
		registerCommands();
		registerAuctionManger();
		registerMetrics();

		if (ConfigManager.getConfig().getBoolean("general.check-updates", true)) {
			getServer().getScheduler().runTaskAsynchronously(this, this::checkUpdateAvailable);
		}
	}

	@Override
	public void onDisable() {
		// Make sure auction manager is valid
		if (getAuctionManager() != null)
			getAuctionManager().disabling();

		// Save auction player data on the disable
		AuctionsPlayerManager.getInstance().saveAndDisable();
	}

	private boolean setupEconomy() {
		try {
			RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

			if (rsp == null)
				return false;

			econ = rsp.getProvider();
		} catch (Exception ignored) {
		}

		return econ != null;
	}

	private void setupPerms() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
	}

	private void registerListeners() {
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		getServer().getPluginManager().registerEvents(new CommandListener(), this);
		getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);
	}

	private void registerCommands() {
		PaperCommandManager manager = new PaperCommandManager(this);

		// Replace ACF messages with our own
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.permission_denied"),
				Messages.getString("command.no_perm"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.permission_denied_parameter"),
				Messages.getString("command.no_perm"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.error_prefix"),
				Messages.getString("command.error_prefix", "{message}"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.invalid_syntax"),
				Messages.getString("command.usage", "{command}", "{syntax}"));

		manager.getCommandContexts().registerIssuerOnlyContext(AuctionsPlayer.class, c -> {
			BukkitCommandIssuer issuer = c.getIssuer();

			if (!issuer.isPlayer())
				throw new InvalidCommandArgument("Console may not execute this command.");

			return AuctionsPlayerManager.getInstance().getPlayer(issuer.getPlayer().getUniqueId());
		});

		manager.getCommandContexts().registerIssuerOnlyContext(Auction.class, c -> {

			Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

			if (current == null) {
				throw new InvalidCommandArgument(Messages.getString("command.no_current_auction"), false);
			}

			return current;
		});

		manager.registerCommand(new AuctionCommand());
		manager.registerCommand(new BidCommand());
	}

	private void registerAuctionManger() {
		auctionManager = new AuctionManager(this);
	}

	private void registerMetrics() {
		// 985 is the bstats plugin id
		new Metrics(this, 985);
	}

	private void checkUpdateAvailable() {
		String serverVersion = getDescription().getVersion();

		// This is taken from the page: https://www.spigotmc.org/resources/ezauctions.42574/
		int resourceId = 42574;

		try {
			InputStream input =
					new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openStream();

			Scanner scanner = new Scanner(input);

			String latestVersion = scanner.nextLine();

			if (serverVersion.equalsIgnoreCase(latestVersion))
				return;

			scanner.close();

			updateAvailable = true;
			getLogger().info("Version " + latestVersion + " is available! You are currently running version " +
					serverVersion + ".");
		} catch (Exception ex) {
			getLogger().warning("Error checking for updates!");
		}
	}

	public static int copy(InputStream input, OutputStream output)
			throws IOException {
		byte[] buffer = new byte[1024 * 4];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}

		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	public static File getDataDirectory() {
		return dataDir;
	}

	public static Logger getPluginLogger() {
		return pluginLogger;
	}

	public static Permission getPerms() {
		return perms;
	}
}
