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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
			// This method was taken from https://www.spigotmc.org/members/maximvdw.6687/
			HttpURLConnection con =
					(HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php").openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream()
					.write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=" +
							resourceId).getBytes("UTF-8"));

			String latestVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();

			if (serverVersion.equalsIgnoreCase(latestVersion))
				return;

			updateAvailable = true;
			getLogger().info("Version " + latestVersion + " is available! You are currently running version " +
					                 serverVersion + ".");
		} catch (Exception ex) {
			getLogger().warning("Error checking for updates!");
		}
	}
}
