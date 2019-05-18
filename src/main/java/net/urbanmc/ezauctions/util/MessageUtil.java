package net.urbanmc.ezauctions.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MessageUtil {

    public static void broadcastRegular(UUID auctioneer, String prop, Object... args) {
        String message = Messages.getString(prop, args);

        Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
                .filter(ap -> !ap.isIgnoringAll() && !ap.getIgnoringPlayers().contains(auctioneer))
                .forEach(ap -> ap.getOnlinePlayer().sendMessage(message));

        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
    }

    public static void broadcastRegular(UUID auctioneer, BaseComponent... comp) {
        Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
                .filter(ap -> !ap.isIgnoringAll() && !ap.getIgnoringPlayers().contains(auctioneer))
                .forEach(ap -> ap.getOnlinePlayer().spigot().sendMessage(comp));

        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(new TextComponent(comp).toPlainText()));
    }

    public static void broadcastSpammy(UUID auctioneer, String prop, Object... args) {
        String message = Messages.getString(prop, args);

        Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
                .filter(ap -> !ap.isIgnoringAll() && !ap.isIgnoringSpammy()
                        && !ap.getIgnoringPlayers().contains(auctioneer))
                .forEach(ap -> ap.getOnlinePlayer().sendMessage(message));

        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
    }

    public static void broadcastSpammy(UUID auctioneer, BaseComponent... comp) {
        Bukkit.getOnlinePlayers().stream().map(p -> AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId()))
                .filter(ap -> !ap.isIgnoringAll() && !ap.isIgnoringSpammy()
                        && !ap.getIgnoringPlayers().contains(auctioneer))
                .forEach(ap -> ap.getOnlinePlayer().spigot().sendMessage(comp));

        Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(new TextComponent(comp).toPlainText()));
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
