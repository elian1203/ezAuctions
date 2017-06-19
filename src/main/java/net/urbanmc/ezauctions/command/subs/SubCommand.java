package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.object.Permission;
import net.urbanmc.ezauctions.util.MessageUtil;
import org.bukkit.command.CommandSender;

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

	public String getHelpProperty() {
		return "command.auction." + sub + ".help";
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

	void sendPropMessage(CommandSender sender, String property, Object... args) {
		MessageUtil.privateMessage(sender, property, args);
	}
}
