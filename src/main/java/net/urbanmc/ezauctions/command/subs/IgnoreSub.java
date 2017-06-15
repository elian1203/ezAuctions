package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnoreSub extends SubCommand {

	public IgnoreSub() {
		super("ignore", Permission.COMMAND_IGNORE, true);
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		Player p = (Player) sender;
		AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

		boolean ignoringAll = ap.isIgnoringAll();

		String prop = "command.auction.ignroe" + (ignoringAll ? "disabled" : "enabled");

		sendPropMessage(p, prop);
		ap.setIgnoringAll(!ignoringAll);
	}
}
