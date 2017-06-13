package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;

public class ReloadSub extends SubCommand {

	public ReloadSub() {
		super("reload", Permission.COMMAND_RELOAD, false);
	}

	public void run(CommandSender sender, String[] args) {
		ConfigManager.getInstance().reloadConfiguration();
		Messages.getInstance().reload();

		sendPropMessage(sender, "command.auction.reload");
	}
}
