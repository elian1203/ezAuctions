package net.urbanmc.ezauctions.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Stream;

public class MessageUtil {

	public static void broadcastRegular(UUID auctioneer, String worldName, String prop, Object... args) {
		String message = Messages.getString(prop, args);

		Stream<AuctionsPlayer> playerStream = getPlayersToSendToRegular(auctioneer, worldName);

		// send to proper players
		playerStream.forEach(ap -> ap.getOnlinePlayer().sendMessage(message));
		// send to console
		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
	}

	public static void broadcastRegular(UUID auctioneer, String worldName, BaseComponent[] comp) {
		Stream<AuctionsPlayer> playerStream = getPlayersToSendToRegular(auctioneer, worldName);

		// send to proper players
		playerStream.forEach(ap -> ap.getOnlinePlayer().spigot().sendMessage(comp));
		// send to console
		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(new TextComponent(comp).toPlainText()));
	}

	public static void broadcastSpammy(UUID auctioneer, String worldName, String prop, Object... args) {
		String message = Messages.getString(prop, args);

		Stream<AuctionsPlayer> playerStream = getPlayersToSendToSpammy(auctioneer, worldName);

		playerStream.forEach(ap -> ap.getOnlinePlayer().sendMessage(message));
		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
	}

	public static void broadcastSpammy(UUID auctioneer, String worldName, BaseComponent... comp) {
		Stream<AuctionsPlayer> playerStream = getPlayersToSendToSpammy(auctioneer, worldName);

		playerStream.forEach(ap -> ap.getOnlinePlayer().spigot().sendMessage(comp));
		Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(new TextComponent(comp).toPlainText()));
	}

	private static Stream<AuctionsPlayer> getPlayersToSendToRegular(UUID auctioneer, String worldName) {
		Stream<AuctionsPlayer> playerStream =
				Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
						.filter(ap -> !ap.isIgnoringAll() && !ap.getIgnoringPlayers().contains(auctioneer));

		// if world name specified, match players to that world and only broadcast if they are in it
		if (worldName != null) {
			playerStream = playerStream.filter(ap -> ap.getOnlinePlayer().getWorld().getName().equals(worldName));
		}

		return playerStream;
	}

	private static Stream<AuctionsPlayer> getPlayersToSendToSpammy(UUID auctioneer, String worldName) {
		Stream<AuctionsPlayer> playerStream = getPlayersToSendToRegular(auctioneer, worldName);

		playerStream = playerStream.filter(ap -> !ap.isIgnoringSpammy());

		return playerStream;
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

	public static void privateMessage(CommandSender sender, BaseComponent... comp) {
		if (sender instanceof Player) {
			((Player) sender).spigot().sendMessage(comp);
		} else {
			sender.sendMessage(ChatColor.stripColor(new TextComponent(comp).toPlainText()));
		}
	}
}
