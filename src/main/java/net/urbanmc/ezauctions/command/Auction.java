package net.urbanmc.ezauctions.command;


import net.urbanmc.ezauctions.object.SubCommand;
import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Auction implements CommandExecutor {

    List<SubCommand> subs = new ArrayList<SubCommand>();

    public Auction() {
        registerSubs();
    }

    private void registerSubs() {

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!hasPermission(sender)) return true;

        if (args.length == 0) return help(sender);

        SubCommand sub = findSub(args[0]);

        if (sub == null) {
            sendPropMessage(sender, "invalid_subcmd");
            return true;
        }

        if(!sub.hasPermission(sender)) return true;

        if(sub.isPlayerOnly(sender)) return true;

        sub.run(sender, args);

        return true;
    }


    private boolean hasPermission(CommandSender sender) {
        if (sender.hasPermission("ezauctions.auction"))
            return true;

        sendMessage(sender, Messages.getInstance().getString("no_perm"));

        return false;
    }

    private boolean help(CommandSender sender) {

        if (!sender.hasPermission("ezauctions.auction.end")) {
            sendPropMessage(sender, "cmd_auc_help");
            return true;
        }

        sendPropMessage(sender, "cmd_auc_adminhelp");
        return true;
    }


    private SubCommand findSub(String arg) {

        for (SubCommand sub : subs)
            if (sub.matchSub(arg))
                return sub;

        return null;
    }


    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage((sender instanceof Player) ? message : ChatColor.stripColor(message));
    }

    private void sendPropMessage(CommandSender sender, String property) {
        if (!(sender instanceof Player))
            sender.sendMessage(ChatColor.stripColor(Messages.getInstance().getString(property)));
        else sender.sendMessage(Messages.getInstance().getString(property));
    }
}
