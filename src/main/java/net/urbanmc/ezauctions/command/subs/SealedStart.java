package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.AuctionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SealedStart extends SubCommand{

    SealedStart() {
        super("startsealed", Permission.COMMAND_START_SEALED, true, "ss", "ssealed", "starts");
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        Player p = (Player) sender;

        if (args.length < 3 || args.length > 5) {
            //TODO Add property message
            sendPropMessage(p, "command.auc.");
            return;
        }

        if (EzAuctions.getAuctionManager().getQueueSize() == ConfigManager.getConfig().getInt("general.auction-queue-limit")) {
            sendPropMessage(p, "command.auc.start.queue-limit");
            return;
        }

        Auction auc = AuctionUtil.getInstance().parseAuction(p,
                args[1],
                args[2],
                args.length < 4 ? String.valueOf(ConfigManager.getInstance().get("default.increment")) : args[3],
                args.length < 5 ? String.valueOf(ConfigManager.getInstance().get("default.autobuy")) : args[4],
                true);

        EzAuctions.getAuctionManager().addToQueue(auc);
    }

}
