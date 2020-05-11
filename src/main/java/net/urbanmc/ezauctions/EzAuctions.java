package net.urbanmc.ezauctions;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.command.BidCommand;
import net.urbanmc.ezauctions.datastorage.DataSource;
import net.urbanmc.ezauctions.listener.CommandListener;
import net.urbanmc.ezauctions.listener.JoinListener;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;

public class EzAuctions extends JavaPlugin {

	private static AuctionManager auctionManager;
	private static Economy econ;

	private static boolean updateAvailable = false;

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
	public void onEnable() {
		if (!setupEconomy()) {
			getLogger().severe("Vault not detected! Is Vault installed along with a supported economy provider? " +
					"Disabling plugin...");
			setEnabled(false);

			return;
		}

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

	private void registerListeners() {
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		getServer().getPluginManager().registerEvents(new CommandListener(), this);
	}

	private void registerCommands() {
		PaperCommandManager manager = new PaperCommandManager(this);

		manager.getCommandContexts().registerIssuerAwareContext(AuctionsPlayer.class, c -> {
			BukkitCommandIssuer issuer = c.getIssuer();

			if (!issuer.isPlayer())
				throw new InvalidCommandArgument("Console may not execute this command.");

			return AuctionsPlayerManager.getInstance().getPlayer(issuer.getPlayer().getUniqueId());
		});

		manager.registerCommand(new AuctionCommand());
		manager.registerCommand(new BidCommand());
	}

	private void registerAuctionManger() {
		auctionManager = new AuctionManager(this);
	}

	private void registerMetrics() {
		new Metrics(this);
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
}
