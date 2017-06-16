package net.urbanmc.ezauctions.util;

import mkremins.fanciful.FancyMessage;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

	public static void broadcastRegular(String prop, Object... args) {
		String message = Messages.getString(prop, args);

		Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
				.filter(ap -> !ap.isIgnoringAll()).forEach(ap -> ap.getOnlinePlayer().sendMessage(message));

		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
	}

	public static void broadcastRegular(FancyMessage fancy) {
		Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
				.filter(ap -> !ap.isIgnoringAll()).forEach(ap -> fancy.send(ap.getOnlinePlayer()));

		fancy.send(Bukkit.getConsoleSender());
	}

	public static void broadcastSpammy(String prop, Object... args) {
		String message = Messages.getString(prop, args);

		Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
				.filter(ap -> !ap.isIgnoringAll() && !ap.isIgnoringSpammy())
				.forEach(ap -> ap.getOnlinePlayer().sendMessage(message));

		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
	}

	public static void privateMessage(Player p, String prop) {
		p.sendMessage(Messages.getString(prop));
	}

	public static void privateMessage(CommandSender sender, String prop) {
		if (sender instanceof Player) {
			privateMessage((Player) sender, prop);
		} else {
			String message = ChatColor.stripColor(Messages.getString(prop));
			sender.sendMessage(message);
		}
	}
}
