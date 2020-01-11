package net.urbanmc.ezauctions.command.subs;

import net.md_5.bungee.api.chat.BaseComponent;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Iterator;

public class QueueSub extends SubCommand {

    public QueueSub() {
        super("queue", Permission.COMMAND_QUEUE, false, "q");
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        sender.sendMessage(Messages.getString("command.auction.queue.list"));

        if (EzAuctions.getAuctionManager().getQueueSize() == 0) {
            sender.sendMessage(Messages.getString("command.auction.queue.empty"));
            return;
        }

        Iterator<Auction> auctionIterator = EzAuctions.getAuctionManager().getQueue();

        int aucPos = 1;

        while (auctionIterator.hasNext()) {
            Auction auc = auctionIterator.next();

            BaseComponent[] auctionQueueMessage = getQueueMessage(aucPos++, auc);

            // Get message
            MessageUtil.privateMessage(sender, auctionQueueMessage);
        }
    }

    private BaseComponent[] getQueueMessage(int pos, Auction auc) {
        String message = Messages.getInstance()
                .getStringWithoutColoring("command.auction.queue.item",
                        pos, auc.getAmount(), "%item%", auc.getStartingPrice(),
                        auc.getAuctioneer().getOfflinePlayer().getName());

        // Use the auction format message method
        return auc.formatMessage(message);
    }
}
