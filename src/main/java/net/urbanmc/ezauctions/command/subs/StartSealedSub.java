package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionQueueEvent;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartSealedSub extends SubCommand{

    public StartSealedSub() {
        super("startsealed", Permission.COMMAND_START_SEALED, true, "ss", "ssealed", "starts");
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Player p = (Player) sender;

        if (!ConfigManager.getConfig().getBoolean("sealed-auctions.enabled")) {
            sendPropMessage(p, "command.auction.start_sealed.disabled");
            return;
        }

        if (args.length < 3 || args.length > 6) {
            //TODO Add property message
            sendPropMessage(p, "command.auction.start.help");
            return;
        }

        if (EzAuctions.getAuctionManager().getQueueSize() == ConfigManager.getConfig().getInt("general.auction-queue-limit")) {
            sendPropMessage(p, "command.auction.start.queue_full");
            return;
        }

        if (!EzAuctions.getAuctionManager().isAuctionsEnabled()) {
            sendPropMessage(p, "command.auction.start.disabled");
            return;
        }

        Auction auc = AuctionUtil.getInstance().parseAuction(p,
                args[1],
                args[2],
                args.length < 4 ? String.valueOf(ConfigManager.getInstance().get("default.increment")) : args[3],
                args.length < 5 ? String.valueOf(ConfigManager.getInstance().get("default.autobuy")) : args[4],
                args.length < 6 ? String.valueOf(ConfigManager.getConfig().getInt("default.auction-time")) : args[5],
                true);

        AuctionQueueEvent event = new AuctionQueueEvent(auc);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        EzAuctions.getAuctionManager().addToQueue(auc);
    }

}
