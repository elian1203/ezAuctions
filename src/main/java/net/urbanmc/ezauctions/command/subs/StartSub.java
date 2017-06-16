package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionQueueEvent;
import net.urbanmc.ezauctions.manager.AuctionManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartSub extends SubCommand {

    public StartSub() {
        super("start", Permission.COMMAND_START, true, "s");
    }

    public void run(CommandSender sender, String[] args) {

        Player p = (Player) sender;

        if (args.length < 3 || args.length > 6) {
            sendPropMessage(p, "command.auction.start.help");
            return;
        }

	    AuctionManager manager = EzAuctions.getAuctionManager();

	    if (!manager.isAuctionsEnabled()) {
		    sendPropMessage(p, "command.auction.start.disabled");
		    return;
	    }

        if (manager.getQueueSize() == ConfigManager.getConfig().getInt("general.auction-queue-limit")) {
            sendPropMessage(p, "command.auction.start.queue_full");
            return;
        }

		if (manager.inQueueOrCurrent(p.getUniqueId())) {
	    	sendPropMessage(p, "command.auction.start.in_queue");
	    	return;
		}

        if(!AuctionUtil.getInstance().checkStartFee(p)) return;

        Auction auc = AuctionUtil.getInstance().parseAuction(p,
                args[1],
                args[2],
                args.length < 4 ? String.valueOf(ConfigManager.getInstance().get("default.increment")) : args[3],
                args.length < 5 ? String.valueOf(ConfigManager.getInstance().get("default.autobuy")) : args[4],
                args.length < 6 ? String.valueOf(ConfigManager.getConfig().getInt("default.auction-time")) : args[5],
                false);

        if (auc == null)
        	return;

        AuctionQueueEvent event = new AuctionQueueEvent(auc);
	    Bukkit.getPluginManager().callEvent(event);

	    if (event.isCancelled())
	    	return;

	    EzAuctions.getAuctionManager().addToQueue(auc);
    }

}
