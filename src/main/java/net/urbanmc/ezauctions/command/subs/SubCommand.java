package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommand {

	private String sub;
	private String permission;
	private boolean playerOnly;
	private String[] aliases;

	SubCommand(String sub, Permission permission, boolean playerOnly, String... aliases) {
		this.sub = sub;
		this.permission = permission.toString();
		this.playerOnly = playerOnly;
		this.aliases = aliases;
	}

	public abstract void run(CommandSender sender, String[] args);

	public String getPermission() {
		return permission;
	}

	public boolean isPlayerOnly() {
		return playerOnly;
	}

	public boolean matchSub(String arg) {
		if (sub.equalsIgnoreCase(arg))
			return true;

		if (aliases == null)
			return false;

		for (String alias : aliases)
			if (alias.equalsIgnoreCase(arg))
				return true;

		return false;
	}

	void sendPropMessage(Player p, String property) {
		p.sendMessage(Messages.getString(property));
	}

	void sendPropMessage(CommandSender sender, String property) {
		sender.sendMessage(sender instanceof Player ? Messages.getString(property) : ChatColor
				.stripColor(Messages.getString(property)));
	}
}
