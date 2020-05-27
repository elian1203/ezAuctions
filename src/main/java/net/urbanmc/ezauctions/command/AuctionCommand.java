package net.urbanmc.ezauctions.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.RegisteredCommand;
import co.aikar.commands.annotation.*;
import co.aikar.locales.MessageKey;
import net.md_5.bungee.api.chat.BaseComponent;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.event.AuctionCancelEvent;
import net.urbanmc.ezauctions.event.AuctionImpoundEvent;
import net.urbanmc.ezauctions.event.AuctionQueueEvent;
import net.urbanmc.ezauctions.manager.*;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.AuctionUtil;
import net.urbanmc.ezauctions.util.ItemUtil;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.RewardUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@CommandAlias("auction|auctions|auc|ezauctions|ezauction")
@CommandPermission("ezauctions.auction")
@Description("Auction Command")
public class AuctionCommand extends BaseCommand {

	@Default
	@CatchUnknown
	public void help(CommandSender sender) {
		List<RegisteredCommand> subs = getRegisteredCommands();
		List<String> display = new ArrayList<>();

		for (RegisteredCommand sub : subs) {
			Iterator<String> perms = sub.getRequiredPermissions().iterator();
			perms.next();

			if (!perms.hasNext())
				continue;

			String permission = perms.next();

			if (!sender.hasPermission(permission))
				continue;

			String[] split = sub.getCommand().split(" ");

			if (split.length == 1)
				continue;

			String subCommand = split[1];
			if (!display.contains(subCommand))
				display.add(subCommand);
		}

		Collections.sort(display);

		sendPropMessage(sender, "command.help");

		for (String command : display) {
			String message = "command.auction." + command + ".help";
			sendPropMessage(sender, message);
		}

		if (sender.hasPermission("ezauctions.bid")) {
			sendPropMessage(sender, "command.bid.help");
		}
	}

	@Subcommand("cancel|c")
	@CommandPermission("ezauctions.auction.cancel")
	public void cancel(CommandSender sender) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return;
		}

		if (!sender.hasPermission("ezauctions.auction.cancel.others")) {
			Player p = (Player) sender;

			if (!p.getUniqueId().equals(current.getAuctioneer().getUniqueId())) {
				sendPropMessage(p, "command.auction.cancel.not_yours");
				return;
			}

			int minTime = ConfigManager.getConfig().getInt("general.minimum-cancel-time");

			if (current.getAuctionTime() < minTime) {
				sendPropMessage(p, "command.auction.cancel.too_late");
				return;
			}
		}

		AuctionCancelEvent event = new AuctionCancelEvent(current, sender);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		EzAuctions.getAuctionManager().getCurrentRunnable().cancelAuction();
	}

	@Subcommand("disable")
	@CommandPermission("ezauctions.auction.disable")
	public void disable(CommandSender sender) {
		boolean enabled = EzAuctions.getAuctionManager().isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.disable.success";
			EzAuctions.getAuctionManager().setAuctionsEnabled(false);
		} else {
			prop = "command.auction.disable.already_disabled";
		}

		sendPropMessage(sender, prop);
	}

	@Subcommand("enable")
	@CommandPermission("ezauctions.auction.enable")
	public void enable(CommandSender sender) {
		boolean enabled = EzAuctions.getAuctionManager().isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.enable.already_enabled";
		} else {
			prop = "command.auction.enable.success";
			EzAuctions.getAuctionManager().setAuctionsEnabled(true);
		}

		sendPropMessage(sender, prop);
	}

	@Subcommand("ignoreplayer|ignorep")
	@CommandPermission("ezauctions.auction.ignore.player")
	public void ignorePlayer(AuctionsPlayer ap, String targetName) {
		Player p = ap.getOnlinePlayer();

		OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

		if (!target.hasPlayedBefore() && !target.isOnline()) {
			sendPropMessage(p, "command.auction.ignoreplayer.not_found");
			return;
		}

		if (p.getUniqueId() == target.getUniqueId()) {
			sendPropMessage(p, "command.auction.ignoreplayer.cannot_ignore_self");
			return;
		}

		boolean alreadyIgnoring = ap.getIgnoringPlayers().contains(target.getUniqueId());

		if (alreadyIgnoring) {
			ap.getIgnoringPlayers().remove(target.getUniqueId());
			sendPropMessage(p, "command.auction.ignoreplayer.not_ignoring", target.getName());
		} else {
			ap.getIgnoringPlayers().add(target.getUniqueId());
			sendPropMessage(p, "command.auction.ignoreplayer.is_ignoring", target.getName());
		}

		AuctionsPlayerManager.getInstance().saveIgnored(ap);
	}

	@Subcommand("ignore")
	@CommandPermission("ezauctions.auction.ignore")
	public void ignore(AuctionsPlayer ap) {
		boolean ignoringAll = ap.isIgnoringAll();

		String prop = "command.auction.ignore." + (ignoringAll ? "disabled" : "enabled");

		sendPropMessage(ap.getOnlinePlayer(), prop);
		ap.setIgnoringAll(!ignoringAll);
	}

	@Subcommand("impound")
	@CommandPermission("ezauctions.auction.impound")
	public void impound(Player p) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(p, "command.no_current_auction");
			return;
		}

		AuctionImpoundEvent event = new AuctionImpoundEvent(current, p);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		sendPropMessage(p, "command.auction.impound");
		EzAuctions.getAuctionManager().getCurrentRunnable().impoundAuction(p);
	}

	@Subcommand("info|i")
	@CommandPermission("ezauctions.auction.info")
	public void info(CommandSender sender) {
		Auction current = EzAuctions.getAuctionManager().getCurrentAuction();

		if (current == null) {
			sendPropMessage(sender, "command.no_current_auction");
			return;
		}

		BaseComponent[] comp = current.getStartingMessage();
		MessageUtil.privateMessage(sender, comp);
	}

	@Subcommand("queue|q")
	@CommandPermission("ezauctions.auction.queue")
	public void queue(CommandSender sender) {
		sender.sendMessage(Messages.getString("command.auction.queue.list"));

		if (EzAuctions.getAuctionManager().getQueueSize() == 0) {
			sender.sendMessage(Messages.getString("command.auction.queue.empty"));
			return;
		}

		Iterator<Auction> auctionIterator = EzAuctions.getAuctionManager().getQueue();

		int aucPos = 1;

		while (auctionIterator.hasNext()) {
			Auction auc = auctionIterator.next();

			BaseComponent[] auctionQueueMessage = getQueueMessage(aucPos++, auc);

			// Get message
			MessageUtil.privateMessage(sender, auctionQueueMessage);
		}
	}

	@Subcommand("reload")
	@CommandPermission("ezauctions.auction.reload")
	public void reload(CommandSender sender) {
		ConfigManager.getInstance().reloadConfiguration();
		Messages.getInstance().reload();

		// Re-inject our messages into ACF locale
		CommandManager manager = getCurrentCommandManager();
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.permission_denied"), Messages.getString("command.no_perm"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.permission_denied_parameter"), Messages.getString("command.no_perm"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.error_prefix"),
				Messages.getString("command.error_prefix", "{message}"));
		manager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.invalid_syntax"),
				Messages.getString("command.usage", "{command}", "{syntax}"));

		ScoreboardManager.getInstance().reload();

		final EzAuctions plugin = ((EzAuctions) Bukkit.getPluginManager().getPlugin("ezAuctions"));

		AuctionsPlayerManager.getInstance().reloadDataSource(plugin);

		sendPropMessage(sender, "command.auction.reload");
	}

	@Subcommand("remove|r")
	@CommandPermission("ezauctions.auction.remove")
	public void remove(Player p) {
		Auction auction = EzAuctions.getAuctionManager().removeFromQueue(p.getUniqueId());

		if (auction == null) {
			sendPropMessage(p, "command.auction.remove.not_in_queue");
			return;
		}

		sendPropMessage(p, "command.auction.remove.success");
		RewardUtil.rewardCancel(auction);
	}

	@Subcommand("save")
	@CommandPermission("ezauctions.auction.save")
	public void save(CommandSender sender) {
		AuctionsPlayerManager.getInstance().asyncSaveData();

		sendPropMessage(sender, "command.auction.save");
	}

	@Subcommand("scoreboard|sb")
	@CommandPermission("ezauctions.auction.scoreboard")
	public void scoreboard(AuctionsPlayer ap) {
		Player p = ap.getOnlinePlayer();

		boolean ignoringScoreboard = ap.isIgnoringScoreboard();

		String prop = "command.auction.scoreboard." + (ignoringScoreboard ? "disabled" : "enabled");

		ap.setIgnoringScoreboard(!ignoringScoreboard);

		if (!ignoringScoreboard) {
			p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		}

		sendPropMessage(p, prop);
	}

	@Subcommand("spam")
	@CommandPermission("ezauctions.auction.spam")
	public void spam(AuctionsPlayer ap) {
		boolean ignoringSpammy = ap.isIgnoringSpammy();

		String prop = "command.auction.spam." + (ignoringSpammy ? "disabled" : "enabled");

		sendPropMessage(ap.getOnlinePlayer(), prop);
		ap.setIgnoringSpammy(!ignoringSpammy);
	}

	@Subcommand("start|s")
	@CommandPermission("ezauctions.auction.start")
	public void start(AuctionsPlayer ap, String[] args) {
		startAuction(ap, args, false);
	}

	@Subcommand("startsealed|ss")
	@CommandPermission("ezauctions.auction.start.sealed")
	public void startSealed(AuctionsPlayer ap, String[] args) {
		if (!ConfigManager.getConfig().getBoolean("sealed-auctions.enabled"))
			throw new ConditionFailedException(Messages.getString("command.auction.startsealed.disabled"));
		else
			startAuction(ap, args, true);
	}

	private void startAuction(AuctionsPlayer ap, String[] args, boolean sealed) {
		Player p = ap.getOnlinePlayer();
		AuctionManager manager = EzAuctions.getAuctionManager();

		String command = sealed ? "startsealed" : "start";

		if (args.length < 2 || args.length > 5) {
			sendPropMessage(p, "command.auction." + command + ".help");
			return;
		}

		if (!manager.isAuctionsEnabled()) {
			sendPropMessage(p, "command.auction." + command + ".disabled");
			return;
		}

		if (manager.getQueueSize() == ConfigManager.getConfig().getInt("general.auction-queue-limit")) {
			sendPropMessage(p, "command.auction.start.queue_full");
			return;
		}

		if (manager.inQueueOrCurrent(p.getUniqueId())) {
			sendPropMessage(p, "command.auction.start.in_queue");
			return;
		}

		if (!hasFee(p)) {
			sendPropMessage(p, "command.auction.start.lacking_fee");
			return;
		}

		Auction auction = AuctionUtil.parseAuction(
				ap,
				args[0],
				args[1],
				args.length < 3 ? String
						.valueOf(ConfigManager.getInstance().get("auctions.default.increment")) : args[2],
				args.length < 4 ? String.valueOf(ConfigManager.getInstance().get("auctions.default.autobuy")) :
						args[3],
				args.length < 5 ? String
						.valueOf(ConfigManager.getConfig().getInt("auctions.default.auction-time")) : args[4],
				sealed);

		if (auction == null)
			return;

		AuctionQueueEvent event = new AuctionQueueEvent(auction);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		removeFee(p);

		ItemUtil.removeItemsFromInv(auction, p);

		if (EzAuctions.getAuctionManager().addToQueue(auction)) {
			int position = EzAuctions.getAuctionManager().getPositionInQueue(ap);
			sendPropMessage(p, "command.auction.start.added_to_queue", position);
		}
	}

	private BaseComponent[] getQueueMessage(int pos, Auction auc) {
		String message = Messages.getInstance()
				.getStringWithoutColoring("command.auction.queue.item",
						pos, auc.getAmount(), "%item%", auc.getStartingPrice(),
						auc.getAuctioneer().getOfflinePlayer().getName());

		// Use the auction format message method
		return auc.formatMessage(message);
	}

	private boolean hasFee(Player p) {
		double fee = ConfigManager.getConfig().getDouble("auctions.fees.start-price");

		return EzAuctions.getEcon().has(p, fee);
	}

	private void removeFee(Player p) {
		double fee = ConfigManager.getConfig().getDouble("auctions.fees.start-price");

		if (fee > 0) {
			EzAuctions.getEcon().withdrawPlayer(p, fee);
		}
	}

	private void sendPropMessage(CommandSender sender, String property, Object... args) {
		MessageUtil.privateMessage(sender, property, args);
	}
}
