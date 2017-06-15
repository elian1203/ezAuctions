package net.urbanmc.ezauctions.util;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class AuctionUtil extends JavaPlugin {

	private static AuctionUtil instance = new AuctionUtil();

	public static AuctionUtil getInstance() {
		return instance;
	}

	private int isPosInt(String num) {
		try {
			return Integer.valueOf(num);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private double isPosDouble(String doub) {
		try {
			return Double.valueOf(doub);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	//TODO Add property messages for all.
	public Auction parseAuction(Player p, String amount, String price, String bidInc, String buyout, boolean
			isSealed) {

		if (((ArrayList<String>) configGet("auctions.blocked-worlds")).contains(p.getWorld().getName().toLowerCase()
		)) {
			sendPropMessage(p, "command.auc.start.blocked-world");
			return null;
		}

		if (((ArrayList<String>) configGet("auctions.blocked-materials"))
				.contains(ItemUtil.getMaterial(p.getInventory().getItemInMainHand().getType().name().toLowerCase()))) {
			sendPropMessage(p, "command.auc.start.blocked-material");
			return null;
		}

		int actualAmt = findAmtItems(p);

		int amt = isPosInt(amount);

		if (amount.equalsIgnoreCase("hand") || amount.equalsIgnoreCase("all"))
			amt = actualAmt;

		if (amt <= 0) {
			sendPropMessage(p, "command.auc.start.invalid_amt");
			return null;
		}

		double start = getValueBasedOnConfig("starting-price", price);

		if (start <= 0) {
			sendPropMessage(p, "command.auc.start.invalid_start_price");
			return null;
		}

		double inc = getValueBasedOnConfig("increment", bidInc);

		if (inc <= 0) {
			sendPropMessage(p, "command.auc.start.invalid_inc");
			return null;
		}

		double buyoutPrice = getValueBasedOnConfig("autobuy", buyout);

		if (buyoutPrice == -1) {
			sendPropMessage(p, "command.auc.start.invalid_buyout");
			return null;
		}

		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

		//TODO Incorperate auction time as one of the arguments and check if valid.
		return new Auction(ap, p.getInventory().getItemInMainHand(), amt, 60, start, inc, buyoutPrice, isSealed);
	}


	private int findAmtItems(Player p) {
		int amt = 0;

		ItemStack it = p.getInventory().getItemInMainHand();

		for (ItemStack item : p.getInventory().getContents()) {
			if (item.isSimilar(it))
				amt += item.getAmount();
		}

		return amt;
	}

	private void sendPropMessage(Player p, String property) {
		p.sendMessage(Messages.getString(property));
	}

	public double getValueBasedOnConfig(String config, String value) {
		return (boolean) configGet("auctions.decimal." + config) ? isPosDouble(value) : isPosInt(value);
	}

	private Object configGet(String conf) {
		return ConfigManager.getInstance().get(conf);
	}
}
