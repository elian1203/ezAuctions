package net.urbanmc.ezauctions.object;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.ReflectionUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Auction {

	private AuctionsPlayer auctioneer;
	private ItemStack item;
	private int amount, auctionTime;
	private double starting, increment, autoBuy;
	private boolean isSealed;
	private List<Bid> bids;

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
		bids = new ArrayList<>();
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

	public void addBid(Bid b) {
		bids.add(b);

		if(getAutoBuy() != 0 && b.getAmount() == getAutoBuy()) {
			EzAuctions.getAuctionManager().getCurrentRunnable().endAuction();
			return;
		}

		if (!isSealed()) {
			String bidder = b.getBidder().getOnlinePlayer().getName();
			double amount = b.getAmount();

			MessageUtil.broadcastSpammy("auction.bid", bidder, amount);
		}

		FileConfiguration data = ConfigManager.getConfig();

		if (data.getBoolean("antisnipe.enabled") && getAuctionTime() <= data.getInt("antisnipe.seconds-for-start")) {
			EzAuctions.getAuctionManager().getCurrentRunnable().antiSnipe();
		}
	}

	public boolean anyBids() {
		return !bids.isEmpty();
	}

	public Bid getLastBid() {
		if (!bids.isEmpty())
			return bids.get(bids.size() - 1);

		return null;
	}

	public int getTimesBid(AuctionsPlayer p) {
		int amt = 0;

		for (Bid b : bids) {
			if (b.getBidder().getUniqueId().equals(p.getUniqueId()))
				amt += 1;
		}

		return amt;
	}

	public Map<AuctionsPlayer, Double> getLosingBids() {
		if (bids.size() < 2)
			return new HashMap<>();

		List<Bid> bidsReversed = Lists.reverse(bids).subList(0, bids.size() - 2);
		Map<AuctionsPlayer, Double> losing = new HashMap<>();

		for (Bid bid : bidsReversed) {
			if (!losing.containsKey(bid.getBidder())) {
				losing.put(bid.getBidder(), bid.getAmount());
			}
		}

		return losing;
	}

	public Bid getLastBidFrom(AuctionsPlayer ap) {
		List<Bid> bidsReversed = Lists.reverse(bids);

		for (Bid bid : bidsReversed) {
			if (bid.getBidder().equals(ap))
				return bid;
		}

		return null;
	}

	public BaseComponent getStartingMessage() {
		StringBuilder message = new StringBuilder(Messages.getInstance().getStringWithoutColoring(
				"auction.info",
				getAuctioneer().getOfflinePlayer().getName(),
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

		return formatStartingMessage(message.toString());
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

	private BaseComponent formatStartingMessage(String message) {
		BaseComponent main = new TextComponent();
		String[] split = message.split("(= )");

		for (String split2 : split) {
			for (String arg : split2.split("(?=&)")) {
				TextComponent extra = new TextComponent();
				ChatColor color = null;

				if (arg.startsWith("&")) {
					color = ChatColor.getByChar(arg.charAt(1));
					arg = arg.substring(2);
				} else {
					List<BaseComponent> extraList = main.getExtra();

					if (extraList == null) {
						System.out.println("null");
						color = ChatColor.WHITE;
					}
				}

				if (color != null) {
					extra.setColor(color);
				}

				System.out.println("color = " + extra.getColor().name());

				if (arg.contains("%item%")) {
					arg = arg.replace("%item%", ReflectionUtil.getFriendlyName(getItem()));

					BaseComponent[] comp = {new TextComponent(ReflectionUtil.getItemAsJson(getItem()))};

					extra.setHoverEvent(new HoverEvent(Action.SHOW_ITEM, comp));
				}

				extra.setText(arg);

				main.addExtra(extra);
			}
		}

		System.out.println("main comp = " + main.toLegacyText());

		return main;
	}
}
