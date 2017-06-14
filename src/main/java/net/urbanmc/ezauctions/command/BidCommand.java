package net.urbanmc.ezauctions.command;

import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BidCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!isPlayer(sender)) return true;



        return true;
    }

    private boolean isPlayer(CommandSender sender) {
        if(sender instanceof Player) return true;

        sender.sendMessage(Messages.getString("command.player_only"));
        return false;
    }
}
