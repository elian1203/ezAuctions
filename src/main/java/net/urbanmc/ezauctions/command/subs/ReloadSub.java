package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.command.AuctionCommand;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.manager.ScoreboardManager;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ReloadSub extends SubCommand {

	public ReloadSub() {
		super("reload", Permission.COMMAND_RELOAD, false);
	}

	public void run(CommandSender sender, String[] args) {
		ConfigManager.getInstance().reloadConfiguration();
		Messages.getInstance().reload();
		ScoreboardManager.getInstance().reload();

		((EzAuctions) Bukkit.getPluginManager().getPlugin("ezAuctions")).getCommand("ezauctions")
				.setExecutor(new AuctionCommand());

		sendPropMessage(sender, "command.auction.reload");
	}
}
