package net.urbanmc.ezauctions.util;

import net.milkbowl.vault.economy.Economy;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bid;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class AuctionUtil {

	public static Auction parseAuction(AuctionsPlayer ap, String amount, String startingPrice, String increment,
	                                   String autoBuy, String time, boolean sealed) {


		System.out.println("Increment: " + increment + "; Buyout: " + autoBuy + "; Time:" + time);

		Player p = ap.getOnlinePlayer();

		if (blockedWorld(p)) {
			message(p, "command.auction.start.blocked-worlds");
			return null;
		}

		ItemStack item = p.getInventory().getItemInMainHand().clone();

		if (item == null || item.getType() == Material.AIR) {
			message(p, "command.auction.start.cannot_auction_air");
			return null;
		}

		if (blockedMaterial(item)) {
			message(p, "command.auction.start.blocked-materials");
			return null;
		}

		int finalAmount = parseAmount(amount, p, item);

		if (finalAmount == -1) {
			message(p, "command.auction.start.invalid-amt");
			return null;
		}

		if (!isPositiveDouble(startingPrice)) {
			message(p, "command.auction.start.invalid_start_price");
			return null;
		}

		double finalStartingPrice = parseNumberFromConfig(startingPrice, "starting-price");

		if (!isPositiveDouble(increment)) {
			message(p, "command.auction.start.invalid-inc");
			return null;
		}

		double finalIncrement = getValueBasedOnConfig(increment, "increment");

		if (!isPos0Double(autoBuy)) {
			message(p, "command.auction.start.invalid-buyout");
			return null;
		}

		double finalAutoBuy = getValueBasedOnConfig(autoBuy, "autobuy");

		if (!isPositiveDouble(time)) {
			message(p, "command.auction.start.invalid-time");
			return null;
		}

		int finalTime = getValueBasedOnConfig(time, "auction-time").intValue();

		return new Auction(ap, item, finalAmount, finalTime, finalStartingPrice, finalIncrement, finalAutoBuy, sealed);
	}

	private static int parseAmount(String amount, Player p, ItemStack item) {
		int finalAmount = -1;

		if (isPositiveDouble(amount)) {
			finalAmount = Integer.parseInt(amount);

			if (!p.getInventory().contains(item, finalAmount))
				return -1;
		} else if (amount.equalsIgnoreCase("hand")) {
			finalAmount = item.getAmount();
		} else if (amount.equalsIgnoreCase("all")) {
			finalAmount = getTotalItems(p, item);
		}

		return finalAmount;
	}

	private static int getTotalItems(Player p, ItemStack item) {
		int amount = 0;

		for (ItemStack is : p.getInventory().getContents()) {
			if (is != null && is.isSimilar(item)) {
				amount += is.getAmount();
			}
		}

		return amount;
	}

	private static Double getValueBasedOnConfig(String number, String config) {
		double d = parseNumberFromConfig(number, config);

		if (!betweenLimits(d, config)) {
			d = getDefault(config);
		}

		return d;
	}

	private static boolean betweenLimits(double number, String config) {
		double configMin = getConfig().getDouble("auctions.minimum." + config), configMax =
				getConfig().getDouble("auctions.maximum." + config);

		if (configMin == -1 && configMax == -1)
			return number == getDefault(config);
		else if (configMax == 0)
			return number >= configMin;
		else
			return number >= configMin && number <= configMax;
	}

	public static Double parseNumberFromConfig(String number, String config) {
		double d = Double.parseDouble(number);

		if (!getConfig().getBoolean("auctions.toggles.decimal." + config)) {
			d = (int) d;
		}

		return d;
	}

	private static boolean isPositiveDouble(String input) {
		try {
			double d = Double.parseDouble(input);
			return d > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isPos0Double(String input) {
		try {
			double d = Double.parseDouble(input);
			return d >= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}


	private static Double getDefault(String config) {
		return getConfig().getDouble("auctions.default." + config);
	}

	private static boolean blockedWorld(Player p) {
		List<String> blockedWorlds =
				getConfig().getStringList("auctions.blocked-worlds").stream().map(String::toLowerCase)
						.collect(Collectors.toList());

		return blockedWorlds.contains(p.getWorld().getName().toLowerCase());
	}

	private static boolean blockedMaterial(ItemStack is) {
		List<Material> blockedMaterials =
				getConfig().getStringList("auctions.blocked-materials").stream().map(ItemUtil::getMaterial)
						.collect(Collectors.toList());

		return blockedMaterials.contains(is.getType());
	}

	private static FileConfiguration getConfig() {
		return ConfigManager.getConfig();
	}

	private static void message(Player p, String prop) {
		MessageUtil.privateMessage(p, prop);
	}

}
