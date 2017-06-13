package net.urbanmc.ezauctions;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.manager.AuctionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EzAuctions extends JavaPlugin {

	private Economy econ;

	private static AuctionManager auctionManager;

	@Override
	public void onEnable() {
		if (!setupEconomy()) {
			getLogger().severe("Vault not detected! Is Vault installed along with a supported economy provider?");
			setEnabled(false);

			return;
		}

		registerCommand();
		registerAuctionManger();
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

		if (rsp == null)
			return false;

		econ = rsp.getProvider();

		return econ != null;
	}

	private void registerCommand() {
		getCommand("ezauctions").setExecutor(new AuctionCommand());
	}

	private void registerAuctionManger() {
		auctionManager = new AuctionManager(this);
	}

	public static AuctionManager getAuctionManager() {
		return auctionManager;
	}
}
