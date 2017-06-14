package net.urbanmc.ezauctions.util;

import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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

    public Auction parseAuction(Player p, String amount, String price, String bidInc, String buyout, boolean isSealed) {

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

        double buyoutPrice = getValueBasedOnConfig("autobuy",buyout);

        if (buyoutPrice == -1) {
            sendPropMessage(p, "command.auc.start.invalid_buyout");
            return null;
        }

        //TODO Incorperate auction time as one of the arguments and check if valid.
        return new Auction(p.getUniqueId(), p.getInventory().getItemInMainHand(), amt, 60, start, inc, buyoutPrice, isSealed);
    }


    private int findAmtItems(Player p) {
        int amt = 0;

        ItemStack it = p.getInventory().getItemInMainHand();

        for (ItemStack item : p.getInventory().getContents()) {
            if (!item.getType().equals(it.getType()))
                continue;
            if (item.getItemMeta().equals(it.getItemMeta()))
                continue;
            amt += item.getAmount();
        }

        return amt;
    }

    private void sendPropMessage(Player p, String property) {
        p.sendMessage(Messages.getString(property));
    }

    private double getValueBasedOnConfig(String config, String value) {
        return (boolean) ConfigManager.getConfig().get("auctions.decimal." + config) ? isPosDouble(value) : isPosInt(value);
    }
}
