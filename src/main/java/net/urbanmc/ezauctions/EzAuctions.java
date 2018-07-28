package net.urbanmc.ezauctions;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.command.BidCommand;
import net.urbanmc.ezauctions.listener.JoinListener;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import org.bstats.Metrics;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
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

		registerListener();
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
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

		if (rsp == null)
			return false;

		econ = rsp.getProvider();

		return econ != null;
	}

	private void registerListener() {
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
	}

	private void registerCommands() {
		getCommand("ezauctions").setExecutor(new AuctionCommand());
		getCommand("bid").setExecutor(new BidCommand());
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
}
