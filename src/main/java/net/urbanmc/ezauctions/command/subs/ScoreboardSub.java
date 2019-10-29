package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardSub extends SubCommand {

    public ScoreboardSub() {
        super("scoreboard", Permission.COMMAND_SCOREBOARD, true, "sb");
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        Player p = (Player) sender;
        AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

        boolean ignoringScoreboard = ap.isIgnoringScoreboard();

        String prop = "command.auction.scoreboard." + (ignoringScoreboard ? "disabled" : "enabled");

        ap.setIgnoringScoreboard(!ignoringScoreboard);

        if (!ignoringScoreboard) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        sendPropMessage(p, prop);
    }
}
