package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpamSub extends SubCommand {

	public SpamSub() {
		super("spam", Permission.COMMAND_SPAM, true);
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Player p = (Player) sender;
		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

		boolean ignoringSpammy = ap.isIgnoringSpammy();

		String prop = "command.auction.spam." + (ignoringSpammy ? "disabled" : "enabled");

		sendPropMessage(p, prop);
		ap.setIgnoringSpammy(!ignoringSpammy);
	}
}
