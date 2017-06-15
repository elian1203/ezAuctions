package net.urbanmc.ezauctions.object;

import mkremins.fanciful.FancyMessage;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.util.ReflectionUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class Auction {

	private AuctionsPlayer auctioneer;
	private ItemStack item;
	private int amount, auctionTime;
	private double starting, increment, autoBuy;
	private Bid lastBid;
	private boolean isSealed;
	private Map<AuctionsPlayer, Integer> bidders;

	public Auction(AuctionsPlayer auctioneer, ItemStack item, int amount, int auctionTime, double starting,
	               double increment, double autoBuy, boolean isSealed) {
		this.auctioneer = auctioneer;
		this.item = item;
		this.amount = amount;
		this.auctionTime = auctionTime;
		this.starting = starting;
		this.increment = increment;
		this.autoBuy = autoBuy;
		this.isSealed = isSealed;

		if (isSealed)
			bidders = new HashMap<>();

	}

	public AuctionsPlayer getAuctioneer() {
		return auctioneer;
	}

	public ItemStack getItem() {
		return item;
	}

	public int getAmount() {
		return amount;
	}

	public int getAuctionTime() {
		return auctionTime;
	}

	public void setAuctionTime(int auctionTime) {
		this.auctionTime = auctionTime;
	}

	public double getStartingPrice() {
		return starting;
	}

	public double getIncrement() {
		return increment;
	}

	public double getAutoBuy() {
		return autoBuy;
	}

	public boolean isSealed() {
		return isSealed;
	}

	public Bid getLastBid() {
		return lastBid;
	}

	public void setLastBid(Bid lastBid) {
		this.lastBid = lastBid;
	}

	public FancyMessage getStartingMessage() {
		FancyMessage fancy = new FancyMessage();

		StringBuilder message = new StringBuilder(Messages.getString(
				"auction.info",
				getAuctioneer(),
				getAmount(),
				"%item%",
				getStartingPrice(),
				getIncrement(),
				getAuctionTime()));

		List<String> extra = new ArrayList<>();
		List<String> remove = new ArrayList<>();

		addAutoBuyBroadcast(extra);
		addHeadBroadcast(extra);
		addRepairBroadcast(extra);
		addSealedBroadcast(extra);

		extra.stream().filter(s -> !s.startsWith("\n")).forEach(s -> {
			message.append(s);
			remove.add(s);
		});

		extra.removeAll(remove);

		extra.forEach(message::append);

		return formatStartingMessage(fancy, message.toString());
	}

	private void addAutoBuyBroadcast(List<String> extra) {
		if (getAutoBuy() > 0) {
			extra.add(Messages.getString("auction.autobuy", getAutoBuy()));
		}
	}

	private void addHeadBroadcast(List<String> extra) {
		if (ConfigManager.getConfig().getBoolean("auctions.toggles.broadcast-head") &&
				getItem().getItemMeta() instanceof SkullMeta) {
			SkullMeta meta = (SkullMeta) getItem().getItemMeta();

			if (meta.hasOwner()) {
				extra.add(Messages.getString("auction.skull", meta.getOwner()));
			}
		}
	}

	private void addRepairBroadcast(List<String> extra) {
		if (ConfigManager.getConfig().getBoolean("auctions.toggles.broadcast-repair")) {
			int xpToRepair = ReflectionUtil.getXPForRepair(getItem());

			if (xpToRepair == -1) {
				extra.add(Messages.getString("auction.repair.impossible"));
			} else if (xpToRepair > 0) {
				extra.add(Messages.getString("auction.repair.price", xpToRepair));
			}
		}
	}

	private void addSealedBroadcast(List<String> extra) {
		if (isSealed()) {
			extra.add(Messages.getString("auction.sealed"));
		}
	}

	private FancyMessage formatStartingMessage(FancyMessage fancy, String message) {
		String[] split = message.split("(?=§)");

		for (String arg : split) {
			ChatColor color;

			if (arg.startsWith("§")) {
				color = ChatColor.getByChar(arg.charAt(1));
				arg = arg.substring(2);
			} else {
				color = ChatColor.WHITE;
			}

			fancy.color(color);

			if (arg.equals("%item%")) {
				fancy.text(ReflectionUtil.getFriendlyName(getItem()));
				ReflectionUtil.addItemHover(fancy, getItem());
			} else {
				fancy.text(arg);
			}

			fancy.then();
		}

		return fancy;
	}


}
