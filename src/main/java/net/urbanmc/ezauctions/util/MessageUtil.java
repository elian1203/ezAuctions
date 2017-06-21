package net.urbanmc.ezauctions.util;

import net.md_5.bungee.api.chat.BaseComponent;
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

	public static void broadcastRegular(BaseComponent comp) {
		Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
				.filter(ap -> !ap.isIgnoringAll()).forEach(ap -> ap.getOnlinePlayer().spigot().sendMessage(comp));

		Bukkit.getConsoleSender().sendMessage(comp.toPlainText());
	}

	public static void broadcastSpammy(String prop, Object... args) {
		String message = Messages.getString(prop, args);

		Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
				.filter(ap -> !ap.isIgnoringAll() && !ap.isIgnoringSpammy())
				.forEach(ap -> ap.getOnlinePlayer().sendMessage(message));

		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
	}

	public static void privateMessage(CommandSender sender, String prop, Object... args) {
		String message = Messages.getString(prop, args);

		if (sender instanceof Player) {
			sender.sendMessage(message);
		} else {
			message = ChatColor.stripColor(message);
			sender.sendMessage(message);
		}
	}

	public static void privateMessage(CommandSender sender, BaseComponent comp) {
		sender.spigot().sendMessage(comp);
	}
}
