package net.urbanmc.ezauctions.command;

import net.urbanmc.ezauctions.command.subs.StartSub;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.command.subs.SubCommand;
import net.urbanmc.ezauctions.object.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AuctionCommand implements CommandExecutor {

	private List<SubCommand> subs = new ArrayList<SubCommand>();

	public AuctionCommand() {
		registerSubs();
	}

	private void registerSubs() {
		subs.add(new StartSub());
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!hasPermission(sender))
			return true;

		if (args.length == 0)
			return help(sender);

		SubCommand sub = findSub(args[0]);

		if (sub == null) {
			sendPropMessage(sender, "command.invalid_sub");
			return true;
		}

		if (!sender.hasPermission(sub.getPermission())) {
			sendPropMessage(sender, "command.no_perm");
			return true;
		}

		if (sub.isPlayerOnly() && !(sender instanceof Player)) {
			sendPropMessage(sender, "command.player_only");
			return true;
		}

		sub.run(sender, args);

		return true;
	}


	private boolean hasPermission(CommandSender sender) {
		if (sender.hasPermission(Permissions.COMMAND_BASE.toString()))
			return true;

		sendPropMessage(sender, "command.no_perm");

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

	private void sendPropMessage(CommandSender sender, String property) {
		String message = Messages.getString(property);

		if (sender instanceof Player)
			sender.sendMessage(message);
		else
			sender.sendMessage(ChatColor.stripColor(message));
	}
}
