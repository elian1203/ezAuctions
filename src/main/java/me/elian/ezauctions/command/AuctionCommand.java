package me.elian.ezauctions.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.RegisteredCommand;
import co.aikar.commands.annotation.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.controller.*;
import me.elian.ezauctions.data.Database;
import me.elian.ezauctions.event.AuctionCancelEvent;
import me.elian.ezauctions.event.AuctionImpoundEvent;
import me.elian.ezauctions.event.AuctionQueueEvent;
import me.elian.ezauctions.helper.ItemHelper;
import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.AuctionData;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.model.AuctionPlayerIgnore;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@CommandAlias("auction|auctions|auc|ezauctions")
@CommandPermission("ezauctions.auction")
@Description("Auction Command")
public class AuctionCommand extends BaseCommand {
	private static final Pattern QUEUE_LIMIT_PERM_PATTERN = Pattern.compile("ezauctions\\.auction\\.queuelimit\\." +
			"([0-9]+)");
	private static final Pattern CURRENT_LIMIT_PERM_PATTERN = Pattern.compile("ezauctions\\.auction\\.currentlimit\\" +
			".([0-9]+)");
	private final Plugin plugin;
	private final Logger logger;
	private final Economy economy;
	private final AuctionController auctionController;
	private final AuctionPlayerController playerController;
	private final ConfigController config;
	private final MessageController messages;
	private final ScoreboardController scoreboard;
	private final UpdateController updateController;
	private final Database database;
	private final TaskScheduler scheduler;

	@Inject
	public AuctionCommand(Plugin plugin, Logger logger, Economy economy, AuctionController auctionController,
	                      AuctionPlayerController playerController, ConfigController config,
	                      MessageController messages, ScoreboardController scoreboard,
	                      UpdateController updateController, Database database, TaskScheduler scheduler) {
		this.plugin = plugin;
		this.logger = logger;
		this.economy = economy;
		this.auctionController = auctionController;
		this.playerController = playerController;
		this.config = config;
		this.messages = messages;
		this.scoreboard = scoreboard;
		this.updateController = updateController;
		this.database = database;
		this.scheduler = scheduler;
	}

	private static int getPermissionLimit(Player player, Pattern pattern) {
		int maxLimit = 0;
		for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
			Matcher matcher = pattern.matcher(permission.getPermission());
			if (!matcher.find())
				continue;

			int limit = Integer.parseInt(matcher.group(1));
			if (limit > maxLimit) {
				maxLimit = limit;
			}
		}

		return maxLimit;
	}

	@HelpCommand
	public void help(CommandSender sender) {
		List<RegisteredCommand> subs = getRegisteredCommands();
		List<String> display = new ArrayList<>();

		for (RegisteredCommand sub : subs) {
			Set<?> set = sub.getRequiredPermissions();
			if (set.stream().anyMatch(s -> !sender.hasPermission((String) s)))
				continue;

			String[] split = sub.getCommand().split(" ");

			if (split.length == 1)
				continue;

			String subCommand = split[1];
			if (!display.contains(subCommand))
				display.add(subCommand);
		}

		if (sender.hasPermission("ezauctions.bid")) {
			display.add("bid");
		}

		messages.sendMessage(sender, "command.help");

		display = getCommandOrder(display);

		for (String command : display) {
			if (command.equals("bid")) {
				messages.sendMessage(sender, "command.bid.help");
			} else {
				String message = "command.auction." + command + ".help";
				messages.sendMessage(sender, message);
			}
		}
	}

	/**
	 * Returns the subcommands in the order specified from the config help section
	 *
	 * @param allowedCommands list of commands that the sender has access to
	 */
	private List<String> getCommandOrder(List<String> allowedCommands) {
		ConfigurationSection helpSection = config.getConfig().getConfigurationSection("help");

		// using old config version, just display the help page in the order it used to: sorted, and bid at the end
		if (helpSection == null) {
			allowedCommands.remove("bid");
			Collections.sort(allowedCommands);
			allowedCommands.add("bid");
			return allowedCommands;
		}

		List<String> ordered = helpSection.getStringList("order");
		ordered.removeIf(command -> !allowedCommands.contains(command));
		return ordered;
	}

	@Subcommand("cancel|c")
	@CommandPermission("ezauctions.auction.cancel")
	public void cancel(CommandSender sender) {
		scheduler.runAsyncTask(() -> {
			Auction current = auctionController.getActiveAuction();

			if (current == null) {
				messages.sendMessage(sender, "command.no_current_auction");
				return;
			}

			boolean returnMoney = true;

			if (!sender.hasPermission("ezauctions.auction.cancel.others")) {
				if (!(sender instanceof Player player)
						|| !player.getUniqueId().equals(current.getAuctionData().getAuctioneer().getUniqueId())) {
					messages.sendMessage(sender, "command.auction.cancel.not_yours");
					return;
				}

				try {
					AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
					if (!auctionPlayer.withinBoundary(config)) {
						messages.sendMessage(player, "command.auction.cancel.outside_boundary");
						return;
					}
				} catch (InterruptedException | ExecutionException ignored) {
				}

				int minTime = config.getConfig().getInt("general.minimum-cancel-time");

				if (current.getRemainingSeconds() < minTime) {
					messages.sendMessage(sender, "command.auction.cancel.too_late");
					return;
				}

				returnMoney = false;
			}

			AuctionCancelEvent event = new AuctionCancelEvent(current, sender);
			plugin.getServer().getPluginManager().callEvent(event);

			if (event.isCancelled())
				return;

			final boolean returnFinal = returnMoney;
			scheduler.runAsyncTask(() -> {
				current.cancelAuction(returnFinal);
			});
		});
	}

	@Subcommand("disable")
	@CommandPermission("ezauctions.auction.disable")
	public void disable(CommandSender sender) {
		boolean enabled = auctionController.isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.disable.success";
			auctionController.setAuctionsEnabled(false);
		} else {
			prop = "command.auction.disable.already_disabled";
		}

		messages.sendMessage(sender, prop);
	}

	@Subcommand("enable")
	@CommandPermission("ezauctions.auction.enable")
	public void enable(CommandSender sender) {
		boolean enabled = auctionController.isAuctionsEnabled();

		String prop;

		if (enabled) {
			prop = "command.auction.enable.already_enabled";
		} else {
			prop = "command.auction.enable.success";
			auctionController.setAuctionsEnabled(true);
		}

		messages.sendMessage(sender, prop);
	}

	@Subcommand("ignoreplayer|ignorep")
	@CommandPermission("ezauctions.auction.ignore.player")
	@CommandCompletion("@players")
	@Syntax("[player]")
	public void ignorePlayer(Player player, String targetName) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

			if (!target.hasPlayedBefore() && !target.isOnline()) {
				messages.sendMessage(player, "command.auction.ignoreplayer.not_found");
				return;
			}

			if (player.getUniqueId() == target.getUniqueId()) {
				messages.sendMessage(player, "command.auction.ignoreplayer.cannot_ignore_self");
				return;
			}

			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				Optional<AuctionPlayerIgnore> ignore = auctionPlayer.getIgnoredPlayers()
						.stream()
						.filter(ip -> ip.getIgnored().equals(target.getUniqueId()))
						.findFirst();
				String targetNameActual = target.getName();
				if (targetNameActual == null) {
					targetNameActual = targetName;
				}

				if (ignore.isPresent()) {
					messages.sendMessage(player, "command.auction.ignoreplayer.not_ignoring",
							Placeholder.unparsed("ignoredplayer", targetNameActual));
					auctionPlayer.getIgnoredPlayers().remove(ignore.get());
				} else {
					messages.sendMessage(player, "command.auction.ignoreplayer.is_ignoring",
							Placeholder.unparsed("ignoredplayer", targetNameActual));
					auctionPlayer.getIgnoredPlayers().add(new AuctionPlayerIgnore(auctionPlayer,
							target.getUniqueId()));
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("ignore")
	@CommandPermission("ezauctions.auction.ignore")
	public void ignore(Player player) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				boolean ignoringAll = auctionPlayer.isIgnoringAll();

				String prop = "command.auction.ignore." + (ignoringAll ? "disabled" : "enabled");

				messages.sendMessage(player, prop);
				auctionPlayer.setIgnoringAll(!ignoringAll);
			} catch (InterruptedException | ExecutionException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("impound")
	@CommandPermission("ezauctions.auction.impound")
	public void impound(Player player) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				Auction current = auctionController.getActiveAuction();

				if (current == null) {
					messages.sendMessage(player, "command.no_current_auction");
					return;
				}

				AuctionImpoundEvent event = new AuctionImpoundEvent(current, player);
				plugin.getServer().getPluginManager().callEvent(event);

				if (event.isCancelled())
					return;

				messages.sendMessage(player, "command.auction.impound");
				current.impoundAuction(auctionPlayer);
			} catch (InterruptedException | ExecutionException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("info|i")
	@CommandPermission("ezauctions.auction.info")
	public void info(CommandSender sender) {
		Auction activeAuction = auctionController.getActiveAuction();

		if (activeAuction == null) {
			messages.sendMessage(sender, "command.no_current_auction");
			return;
		}

		messages.sendAuctionMessage(sender, "auction.info", activeAuction);
	}

	@Subcommand("queue|q")
	@CommandPermission("ezauctions.auction.queue")
	public void queue(CommandSender sender) {
		Collection<AuctionData> queue = auctionController.getAuctionQueue();
		if (queue.size() == 0) {
			messages.sendMessage(sender, "command.auction.queue.empty");
			return;
		}

		messages.sendMessage(sender, "command.auction.queue.list");

		int position = 1;
		for (AuctionData data : queue) {
			messages.sendAuctionMessage(sender, "command.auction.queue.item", data,
					Formatter.number("position", position++));
		}
	}

	@Subcommand("remove|r")
	@CommandPermission("ezauctions.auction.remove")
	public void remove(Player player) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			auctionController.withSync(() -> {
				try {
					AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
					AuctionData queued = auctionController.removeFirstItemFromQueue(auctionPlayer);

					if (queued == null) {
						messages.sendMessage(player, "command.auction.remove.not_in_queue");
						return;
					}

					messages.sendMessage(player, "command.auction.remove.success");
					queued.giveItemToPlayer(auctionPlayer, playerController, scheduler, config, messages);
				} catch (ExecutionException | InterruptedException e) {
					logger.severe("Failed to complete player command!", e);
				}
			});
		});
	}

	@Subcommand("scoreboard|sb")
	@CommandPermission("ezauctions.auction.scoreboard")
	public void scoreboard(Player player) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				boolean ignoringScoreboard = auctionPlayer.isIgnoringScoreboard();

				String prop = "command.auction.scoreboard." + (ignoringScoreboard ? "disabled" : "enabled");
				auctionPlayer.setIgnoringScoreboard(!ignoringScoreboard);

				if (auctionPlayer.isIgnoringScoreboard()) {
					scoreboard.removePlayer(player);
				} else {
					scoreboard.addPlayer(player);
				}

				messages.sendMessage(player, prop);
				playerController.savePlayer(auctionPlayer);
			} catch (ExecutionException | InterruptedException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("spam")
	@CommandPermission("ezauctions.auction.spam")
	public void spam(Player player) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				boolean ignoringSpammy = auctionPlayer.isIgnoringSpammy();

				String prop = "command.auction.spam." + (ignoringSpammy ? "disabled" : "enabled");
				messages.sendMessage(player, prop);

				auctionPlayer.setIgnoringSpammy(!ignoringSpammy);
				playerController.savePlayer(auctionPlayer);
			} catch (ExecutionException | InterruptedException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("end")
	@CommandPermission("ezauctions.auction.end")
	public void end(CommandSender sender) {
		scheduler.runAsyncTask(() -> {
			auctionController.withSync(() -> {
				Auction activeAuction = auctionController.getActiveAuction();

				if (activeAuction == null || activeAuction.isCompleted()) {
					messages.sendMessage(sender, "command.no_current_auction");
					return;
				}

				if (sender instanceof Player p
						&& !p.hasPermission("ezauctions.auction.end.others")
						&& !p.getUniqueId().equals(activeAuction.getAuctionData().getAuctioneer().getUniqueId())) {
					messages.sendMessage(sender, "command.auction.end.attempt-others");
					return;
				}

				messages.broadcastAuctionMessage(playerController.getOnlinePlayers(),
						activeAuction, false, "auction.end",
						Placeholder.unparsed("endingplayer", sender.getName()));

				activeAuction.end();
			});
		});
	}

	@Subcommand("start|s")
	@CommandPermission("ezauctions.auction.start")
	@CommandCompletion("hand|all|1|64 1 0|1 0|100000 0|60")
	@Syntax("[amount] [price] [increment] [autobuy] [time]")
	public void start(Player player, String amount, double price, @Default(value = "0") double increment,
	                  @Default(value = "0") double autoBuyPrice, @Default(value = "0") int time) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				auctionController.withSync(() -> startAuction(player, auctionPlayer, amount, price, increment,
						autoBuyPrice, time, false));
			} catch (ExecutionException | InterruptedException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	@Subcommand("startsealed|ss")
	@CommandPermission("ezauctions.auction.start.sealed")
	@CommandCompletion("hand|all|1|64 1 1 0|100000 0|60")
	@Syntax("[amount] [price] [increment] [autobuy] [time]")
	public void startSealed(Player player, String amount, double price, @Default(value = "0") double increment,
	                        @Default(value = "0") double autoBuyPrice, @Default(value = "0") int time) {
		if (!config.getConfig().getBoolean("sealed-auctions.enabled")) {
			messages.sendMessage(player, "command.auction.startsealed.disabled");
			return;
		}

		scheduler.runAsyncPlayerCommandTask(player, () -> {
			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				auctionController.withSync(() -> startAuction(player, auctionPlayer, amount, price, increment,
						autoBuyPrice, time, true));
			} catch (ExecutionException | InterruptedException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	private void startAuction(Player player, AuctionPlayer auctionPlayer, String amount, double price,
	                          double increment, double autoBuyPrice,
	                          int time, boolean sealed) {
		if (!auctionController.isAuctionsEnabled()) {
			messages.sendMessage(player, "command.auction.start.disabled");
			return;
		}

		if (!auctionPlayer.withinBoundary(config)) {
			messages.sendMessage(player, "command.auction.start.outside_boundary");
			return;
		}

		if (reachedQueueLimit(player))
			return;

		double fee = config.getConfig().getDouble("auctions.fees.start-price");
		if (fee != 0 && !economy.has(player, fee)) {
			messages.sendMessage(player, "command.auction.start.lacking_fee");
			return;
		}

		ItemStack item = player.getInventory().getItemInMainHand();
		AuctionData auctionData = new AuctionData(auctionPlayer, item, amount, time, price, increment,
				autoBuyPrice, sealed, player.getWorld().getName());
		auctionData.gatherAdditionalData(logger);
		auctionData.fillDefaults(config);

		if (!auctionData.validate(config, messages, player))
			return;

		AuctionQueueEvent event = new AuctionQueueEvent(auctionData);
		plugin.getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		auctionController.setCooldown(player.getUniqueId());
		economy.withdrawPlayer(player, fee);
		ItemHelper.removeItemFromPlayerInventory(player, auctionData.getItem(), auctionData.getAmount());

		if (auctionController.queueAuction(auctionData)) {
			int position = auctionController.getPositionInQueue(auctionData);
			messages.sendMessage(player, "command.auction.start.added_to_queue",
					Formatter.number("position", position));
		}
	}

	@Subcommand("reload")
	@CommandPermission("ezauctions.auction.reload")
	public void reload(CommandSender sender) {
		scheduler.runAsyncTask(() -> {
			try {
				config.reload();
			} catch (Exception e) {
				logger.severe("Could not load config file!", e);
				sender.sendMessage(Component.text("Failed to load configuration file. " +
						"Please check the console for more information."));
				return;
			}

			try {
				messages.reload();
			} catch (Exception e) {
				logger.severe("Could not load messages file!", e);
				sender.sendMessage(Component.text("Failed to load messages file. " +
						"Please check the console for more information."));
				return;
			}

			try {
				database.reconnect();
			} catch (Exception e) {
				logger.severe("Could not connect to database!", e);
				sender.sendMessage(Component.text("Failed to connect to database. " +
						"Please check the console for more information."));
				return;
			}

			messages.sendMessage(sender, "command.auction.reload");
		});
	}

	@Subcommand("update")
	@CommandPermission("ezauctions.auction.update")
	public void update(CommandSender sender) {
		scheduler.runAsyncTask(() -> {
			messages.sendMessage(sender, "command.auction.update.checking");
			try {
				updateController.fetchLatestSupportedVersion();
				String latestVersion = updateController.getLatestSupportedPluginVersion();
				String serverVersion = updateController.getServerPluginVersion();
				if (latestVersion == null || latestVersion.equals(serverVersion)) {
					if (latestVersion == null) {
						latestVersion = serverVersion;
					}

					messages.sendMessage(sender, "command.auction.update.already_up_to_date",
							Placeholder.unparsed("latestversion", latestVersion),
							Placeholder.unparsed("serverversion", serverVersion));
					return;
				}

				messages.sendMessage(sender, "command.auction.update.latest_version",
						Placeholder.unparsed("latestversion", latestVersion),
						Placeholder.unparsed("serverversion", serverVersion));

				updateController.downloadLatestSupportedVersion();
				messages.sendMessage(sender, "command.auction.update.downloaded",
						Placeholder.unparsed("latestversion", latestVersion),
						Placeholder.unparsed("serverversion", serverVersion));
			} catch (Exception e) {
				logger.warning("Error while updating plugin", e);
				messages.sendMessage(sender, "command.auction.update.error");
			}
		});
	}

	private boolean reachedQueueLimit(Player player) {
		Collection<AuctionData> queue = auctionController.getAuctionQueue();

		if (queue.size() >= config.getConfig().getInt("general.auction-queue-limit")
				&& queue.size() >= getPermissionLimit(player, QUEUE_LIMIT_PERM_PATTERN)) {
			messages.sendMessage(player, "command.auction.start.queue_full");
			return true;
		}

		Auction activeAuction = auctionController.getActiveAuction();
		long queuedByPlayer = queue.stream()
				.filter(d -> d.getAuctioneer().getUniqueId().equals(player.getUniqueId())).count();
		boolean currentAuctioneer = activeAuction != null
				&& activeAuction.getAuctionData().getAuctioneer().getUniqueId().equals(player.getUniqueId());
		if ((queuedByPlayer != 0 || currentAuctioneer)
				&& queuedByPlayer >= getPermissionLimit(player, CURRENT_LIMIT_PERM_PATTERN)) {
			messages.sendMessage(player, "command.auction.start.in_queue");
			return true;
		}

		if (auctionController.hasCooldown(player.getUniqueId()) && !player.hasPermission("ezauctions.cooldownexempt")) {
			long timeRequiredToWait = config.getConfig().getLong("general.queue-cooldown-time");
			long timeSinceLastAuction = auctionController.getCooldownTime(player.getUniqueId());
			long timeStillNeededToWait = timeRequiredToWait * 1000 - timeSinceLastAuction;

			long minutes = (timeStillNeededToWait / 1000) / 60;
			long seconds = (timeStillNeededToWait / 1000) % 60;

			if (minutes > 0) {
				messages.sendMessage(player, "command.auction.start.cooldown.time_minutes",
						Formatter.number("seconds", seconds),
						Formatter.number("minutes", minutes));
			} else {
				messages.sendMessage(player, "command.auction.start.cooldown.time_seconds",
						Formatter.number("seconds", seconds));
			}

			return true;
		}

		return false;
	}
}
