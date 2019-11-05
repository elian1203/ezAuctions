package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnorePlayerSub extends SubCommand {

    public IgnorePlayerSub() {
        super("ignoreplayer", Permission.COMMAND_IGNORE_PLAYER, true, "ignorep");
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sendPropMessage(sender, "command.auction.ignoreplayer.help");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendPropMessage(sender, "command.auction.ignoreplayer.not_found");
            return;
        }

        Player p = (Player) sender;

        if (p.getUniqueId() == target.getUniqueId()) {
            sendPropMessage(p, "command.auction.ignoreplayer.cannot_ignore_self");
            return;
        }

        AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

        boolean alreadyIgnoring = ap.getIgnoringPlayers().contains(target.getUniqueId());

        if (alreadyIgnoring) {
            ap.getIgnoringPlayers().remove(target.getUniqueId());
            sendPropMessage(p, "command.auction.ignoreplayer.not_ignoring", target.getName());
        } else {
            ap.getIgnoringPlayers().add(target.getUniqueId());
            sendPropMessage(p, "command.auction.ignoreplayer.is_ignoring", target.getName());
        }

        AuctionsPlayerManager.getInstance().saveIgnored(ap);
    }
}
