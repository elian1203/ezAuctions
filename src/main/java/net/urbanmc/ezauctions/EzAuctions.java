package net.urbanmc.ezauctions;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.command.BidCommand;
import net.urbanmc.ezauctions.listener.JoinListener;
import net.urbanmc.ezauctions.manager.AuctionManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EzAuctions extends JavaPlugin {

	private static AuctionManager auctionManager;
	private static Economy econ;

	public static AuctionManager getAuctionManager() {
		return auctionManager;
	}

	public static Economy getEcon() {
		return econ;
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
}
