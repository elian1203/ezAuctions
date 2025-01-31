package me.elian.ezauctions;

import com.google.inject.Inject;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.elian.ezauctions.controller.AuctionController;
import me.elian.ezauctions.controller.ConfigController;
import me.elian.ezauctions.model.Auction;
import me.elian.ezauctions.model.AuctionData;
import me.elian.ezauctions.model.Bid;
import me.elian.ezauctions.model.BidList;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EzAuctionsPlaceholderExpansion extends PlaceholderExpansion {
	private final Plugin plugin;
	private final AuctionController auctionController;
	private final ConfigController configController;
	private final Economy economy;

	@Inject
	public EzAuctionsPlaceholderExpansion(Plugin plugin, AuctionController auctionController,
	                                      ConfigController configController, Economy economy) {
		this.plugin = plugin;
		this.auctionController = auctionController;
		this.configController = configController;
		this.economy = economy;
	}

	@Override
	public @NotNull String getIdentifier() {
		return plugin.getName();
	}

	@Override
	public @NotNull String getAuthor() {
		return String.join(" -> ", plugin.getDescription().getAuthors());
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		Auction auction = auctionController.getActiveAuction();
		// if there is no active auction -> return a blank string
		if (auction == null) {
			return "";
		}

		AuctionData data = auction.getAuctionData();

		ItemStack item = data.getItem();

		String auctioneerName = data.getAuctioneer().getOfflinePlayer().getName();
		if (auctioneerName == null) {
			auctioneerName = "";
		}

		double highestBidAmount = data.getStartingPrice();
		String highestBidderName = null;
		String highestBidderUniqueId = "";
		BidList bidList = auction.getBidList();
		if (bidList != null) {
			Bid highestBid = bidList.getHighestBid();
			if (highestBid != null && !data.isSealed()) {
				highestBidAmount = highestBid.amount();
				highestBidderName = highestBid.auctionPlayer().getOfflinePlayer().getName();
				highestBidderUniqueId = highestBid.auctionPlayer().getUniqueId().toString();
			}
		}

		if (highestBidderName == null) {
			highestBidderName = "";
		}

		int remainingSeconds = auction.getRemainingSeconds();

		String placeholder = params.toLowerCase();
		return switch (placeholder) {
			case "auctioneer" -> auctioneerName;
			case "auctioneeruuid" -> data.getAuctioneer().getUniqueId().toString();
			case "itemamount" -> Integer.toString(data.getAmount());
			case "minecraftname" -> data.getMinecraftName();
			case "customname" -> data.getCustomName();
			case "materialtype" -> item.getType().toString().toLowerCase();
			case "startingprice" -> Double.toString(data.getStartingPrice());
			case "highestbidamount" -> Double.toString(highestBidAmount);
			case "highestbidder" -> highestBidderName;
			case "highestbidderuuid" -> highestBidderUniqueId;
			case "increment" -> Double.toString(data.getIncrementPrice());
			case "starttime" -> Integer.toString(data.getStartingAuctionTime());
			case "remainingtime" -> Integer.toString(remainingSeconds);
			case "autobuy" -> Double.toString(data.getAutoBuyPrice());
			case "world" -> data.getWorld();
			case "skullowner" -> data.getSkullOwner();
			case "repairprice" -> Integer.toString(data.getRepairPrice());
			case "antisnipetime" -> Integer.toString(configController.getConfig().getInt("antisnipe.time"));
			case "currencynameplural" -> economy.currencyNamePlural();
			case "currencynamesingular" -> economy.currencyNameSingular();
			default -> null;
		};
	}
}
