package net.urbanmc.ezauctions.command;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionBidEvent;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Bid;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BidCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player)) {
            sendPropMessage(sender, "command.player_only");
            return true;
        }

        if(!sender.hasPermission(Permission.COMMAND_BID.toString())) {
            sendPropMessage(sender,"command.no-perm");
            return true;
        }

        if(args.length > 1) {
            sendPropMessage(sender, "command.bid.help");
            return true;
        }

        Auction auc = EzAuctions.getAuctionManager().getCurrentAuction();

        if(auc == null) {
            sendPropMessage(sender,"command.bid.no-auc");
            return true;
        }

        double amount = AuctionUtil.getInstance().getValueBasedOnConfig("bid", args.length == 0 ? "0" : args[0]);

        if(args.length == 0) {
            if (auc.getLastBid() == null) amount = auc.getStartingPrice();
            else amount = auc.getLastBid().getAmount() + auc.getIncrement();
        }

        if(amount <= 0) {
            sendPropMessage(sender, "command.bid.invalid-amount");
            return true;
        }

        //TODO Incorperate # of bids for sealed auctions.

        Player p = (Player) sender;
        AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

        Bid bid = new Bid(ap, amount);

        AuctionBidEvent event = new AuctionBidEvent(auc, bid);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return true;

        removeMoney(p);
        auc.setLastBid(bid);

        return true;
    }

    private void sendPropMessage(CommandSender sender, String property) {
        String message = Messages.getString(property);

        if (sender instanceof Player)
            sender.sendMessage(message);
        else
            sender.sendMessage(ChatColor.stripColor(message));
    }

    private void removeMoney(Player p) {
        //TODO This method.
    }


}
