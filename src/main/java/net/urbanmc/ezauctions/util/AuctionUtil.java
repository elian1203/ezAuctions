package net.urbanmc.ezauctions.util;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.configuration.file.FileConfiguration;
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
    public Auction parseAuction(Player p, String amount, String price, String bidInc, String buyout, String time, boolean
            isSealed) {

        if ((getConfig().getStringList("auctions.blocked-worlds")).contains(p.getWorld().getName().toLowerCase()
        )) {
            sendPropMessage(p, "command.auction.start.blocked-worlds");
            return null;
        }

        if ((getConfig().getStringList("auctions.blocked-materials"))
                .contains(ItemUtil.getMaterial(p.getInventory().getItemInMainHand().getType().name().toLowerCase()))) {
            sendPropMessage(p, "command.auction.start.blocked-materials");
            return null;
        }

        int actualAmt = findAmtItems(p);

        int amt = isPosInt(amount);

        if (amount.equalsIgnoreCase("hand") || amount.equalsIgnoreCase("all"))
            amt = actualAmt;

        if (amt <= 0) {
            sendPropMessage(p, "command.auction.start.invalid-amt");
            return null;
        }

        double start = getValueBasedOnConfig("starting-price", price);

        if (start <= 0) {
            sendPropMessage(p, "command.auction.start.invalid_start_price");
            return null;
        }

        double inc = getValueBasedOnConfig("increment", bidInc);

        if (inc <= 0) {
            sendPropMessage(p, "command.auction.start.invalid-inc");
            return null;
        }

        double buyoutPrice = getValueBasedOnConfig("autobuy", buyout);

        if (buyoutPrice == -1) {
            sendPropMessage(p, "command.auction.start.invalid-buyout");
            return null;
        }


        int aucTime = isPosInt(time);

        if (aucTime <= 0) {
            sendPropMessage(p, "command.auction.start.invalid-time");
            return null;
        }

        AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

        return new Auction(ap, p.getInventory().getItemInMainHand(), amt, aucTime, start, inc, buyoutPrice, isSealed);
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
        MessageUtil.privateMessage(p, property);
    }

    public double getValueBasedOnConfig(String config, String value) {
        return getConfig().getBoolean("auctions.decimal." + config) ? isPosDouble(value) : isPosInt(value);
    }

    private FileConfiguration getConfig(String conf) {
        return ConfigManager.getConfig();
    }

    public boolean checkStartFee(Player p) {
        if (EzAuctions.getEcon().getBalance(p) < ConfigManager.getConfig().getDouble("auctions.start-price")) {

            sendPropMessage(p, "command.auction.start.invalid-fee");

            return false;
        }
        else EzAuctions.getEcon().withdrawPlayer(p, ConfigManager.getConfig().getDouble("auctions.start-price"));

        return true;
    }
}
