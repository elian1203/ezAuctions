package net.urbanmc.ezauctions.object;

import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommand {

    private String sub;
    private String perm;
    private String[] aliases;
    private boolean playerOnly;

    public SubCommand(String sub, String permission, boolean playerOnly, String... aliases) {
        this.sub = sub;
        this.perm = "ezauctions.auction." + permission;
        this.aliases = aliases;
    }

    public abstract void run(CommandSender sender, String[] args);

    public boolean hasPermission(CommandSender sender) {
        if(sender.hasPermission(perm)) return true;
        sender.hasPermission((sender instanceof Player) ? Messages.getInstance().getString("no_perm") : ChatColor.stripColor(Messages.getInstance().getString("no_perm")));
        return false;
    }

    public boolean isPlayerOnly(CommandSender sender) {
        if(playerOnly && (sender instanceof Player))
            sender.sendMessage(Messages.getInstance().getString("player_only"));

        return playerOnly;
    }

    public boolean matchSub(String arg) {
        if (sub.equalsIgnoreCase(arg)) return true;

        if(aliases == null) return false;

        for(String alias : aliases)
            if(alias.equalsIgnoreCase(arg))
                return true;

        return false;
    }

}
