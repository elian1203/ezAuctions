package me.elian.ezauctions.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.PluginLogger;
import me.elian.ezauctions.controller.AuctionController;
import me.elian.ezauctions.controller.AuctionPlayerController;
import me.elian.ezauctions.controller.ConfigController;
import me.elian.ezauctions.controller.MessageController;
import me.elian.ezauctions.event.AuctionBidEvent;
import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.model.Bid;
import me.elian.ezauctions.model.BidList;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ExecutionException;

@Singleton
@CommandAlias("bid|b")
@CommandPermission("ezauctions.bid")
@Description("Bid on an auction")
public class BidCommand extends BaseCommand {
	private final AuctionController auctionController;
	private final AuctionPlayerController playerController;
	private final ConfigController config;
	private final MessageController messages;
	private final Plugin plugin;
	private final PluginLogger logger;
	private final Economy economy;
	private final TaskScheduler scheduler;

	@Inject
	public BidCommand(AuctionController auctionController, AuctionPlayerController playerController,
	                  ConfigController config, MessageController messages, Plugin plugin, PluginLogger logger,
	                  Economy economy, TaskScheduler scheduler) {
		this.auctionController = auctionController;
		this.playerController = playerController;
		this.config = config;
		this.messages = messages;
		this.plugin = plugin;
		this.logger = logger;
		this.economy = economy;
		this.scheduler = scheduler;
	}

	@Default
	public void bid(Player player, @Default(value = "0") final double amount) {
		scheduler.runAsyncPlayerCommandTask(player, () -> {
			if (amount < 0) {
				messages.sendMessage(player, "command.bid.invalid_amount");
				return;
			}

			Auction activeAuction = auctionController.getActiveAuction();
			if (activeAuction == null) {
				messages.sendMessage(player, "command.no_current_auction");
				return;
			}

			String playerWorld = player.getWorld().getName();
			if (config.getConfig().getStringList("auctions.blocked-worlds").stream()
					.anyMatch(playerWorld::equalsIgnoreCase)) {
				messages.sendAuctionMessage(player, "command.bid.blocked_world", activeAuction,
						Placeholder.unparsed("playerworld", playerWorld));
				return;
			}

			if (config.getConfig().getBoolean("auctions.per-world-auctions")
					&& !playerWorld.equals(activeAuction.getAuctionData().getWorld())) {
				messages.sendAuctionMessage(player, "command.bid.wrong_world", activeAuction,
						Placeholder.unparsed("playerworld", playerWorld));
				return;
			}

			if (activeAuction.getAuctionData().getAuctioneer().getUniqueId().equals(player.getUniqueId())) {
				messages.sendAuctionMessage(player, "command.bid.self_bid", activeAuction);
				return;
			}

			try {
				AuctionPlayer auctionPlayer = playerController.getPlayer(player).get();
				activeAuction.getBidList().withSync(() -> tryPlaceBid(player,
						auctionPlayer, activeAuction, amount));
			} catch (InterruptedException | ExecutionException e) {
				logger.severe("Failed to complete player command!", e);
			}
		});
	}

	private void tryPlaceBid(Player player, AuctionPlayer auctionPlayer, Auction activeAuction, double amount) {
		BidList bidList = activeAuction.getBidList();

		double minimumRequiredAmount = bidList.getMinimumRequiredBid(auctionPlayer);

		if (amount == 0) {
			amount = minimumRequiredAmount;
		}

		double pow = Math.pow(10, config.getConfig().getInt("auctions.decimal.bid"));
		amount = Math.round(amount * pow) / pow;

		if (amount < minimumRequiredAmount) {
			messages.sendMessage(player, "command.bid.too_low",
					Formatter.number("requiredamount", minimumRequiredAmount));
			return;
		}

		double autoBuyPrice = activeAuction.getAuctionData().getAutoBuyPrice();
		if (autoBuyPrice != 0 && amount > autoBuyPrice) {
			amount = autoBuyPrice;
		}

		if (activeAuction.getAuctionData().isSealed()) {
			int numberOfBids = bidList.getNumberOfBids(auctionPlayer);
			if (numberOfBids == config.getConfig().getInt("sealed-auctions.max-bids")) {
				messages.sendAuctionMessage(player, "command.bid.max-bids", activeAuction,
						Formatter.number("maxbidsamount", numberOfBids));
				return;
			}
		}

		int consecutiveBids = bidList.getConsecutiveBids(auctionPlayer);
		int maxConsecutiveBids = config.getConfig().getInt("auctions.maximum.consecutive-bids");

		if (maxConsecutiveBids != 0 && consecutiveBids == maxConsecutiveBids) {
			messages.sendAuctionMessage(player, "command.bid.consecutive_limit", activeAuction,
					Formatter.number("consecutivelimit", consecutiveBids));
			return;
		}

		Bid highestBidForPlayer = bidList.getHighestBidForPlayer(auctionPlayer);
		double existingBidAmount = highestBidForPlayer == null ? 0 : highestBidForPlayer.amount();
		double amountToRemove = amount - existingBidAmount;

		if (economy.getBalance(auctionPlayer.getOfflinePlayer()) < amountToRemove) {
			messages.sendAuctionMessage(player, "command.bid.lacking_money", activeAuction,
					Formatter.number("requiredamount", amountToRemove));
			return;
		}

		Bid bid = new Bid(auctionPlayer, amount);

		AuctionBidEvent event = new AuctionBidEvent(activeAuction, bid);
		plugin.getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		economy.withdrawPlayer(auctionPlayer.getOfflinePlayer(), amountToRemove);
		bidList.placeBid(bid);
		messages.sendAuctionMessage(player, "command.bid.placed", activeAuction);
		messages.broadcastAuctionMessage(playerController.getOnlinePlayers(), activeAuction, true, "auction.bid");
	}
}
