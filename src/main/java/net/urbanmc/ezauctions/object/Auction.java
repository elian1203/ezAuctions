package net.urbanmc.ezauctions.object;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.manager.Messages;
import net.urbanmc.ezauctions.util.MessageUtil;
import net.urbanmc.ezauctions.util.ReflectionUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Auction {

	private AuctionsPlayer auctioneer;
	private ItemStack item;
	private int amount, auctionTime;
	private double starting, increment, autoBuy;
	private boolean isSealed;
	private List<Bidder> bidders;

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
		bidders = new ArrayList<>();
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

	public void addNewBidder(Bidder b) {
		updateConsecutiveBidder(b, b.getAmount());
		bidders.add(b);
		broadcastBid(b);
	}

	public void updateBidder(Bidder b) {
		broadcastBid(b);
	}

	public void updateConsecutiveBidder(Bidder upcomingLatestBidder, double upcomingAmount) {
		Bidder lastBidder = getLastBidder();

		if (lastBidder == null)
			return;

		if (lastBidder == upcomingLatestBidder)
			return;

		lastBidder.resetConsecutiveBids();
	}

	public void broadcastBid(Bidder b) {
		if (getAutoBuy() != 0 && b.getAmount() == getAutoBuy()) {
			EzAuctions.getAuctionManager().getCurrentRunnable().endAuction();
			return;
		}

		if (!isSealed()) {
			String bidder = b.getBidder().getOnlinePlayer().getName();
			double amount = b.getAmount();

			MessageUtil.broadcastSpammy("auction.bid", bidder, amount);
		}

		FileConfiguration data = ConfigManager.getConfig();

		if (!isSealed() && data.getBoolean("antisnipe.enabled") &&
				getAuctionTime() <= data.getInt("antisnipe.seconds-for-start")) {
			EzAuctions.getAuctionManager().getCurrentRunnable().antiSnipe();
		}
	}

	public boolean anyBids() {
		return !bidders.isEmpty();
	}

	public Bidder getLastBidder() {
		if (!bidders.isEmpty()) {
			List<Bidder> sorted = getBiddersHighestToLowest();
			return sorted.get(sorted.size() - 1);
		}

		return null;
	}

	public int getTimesBid(AuctionsPlayer ap) {
		int timesBid = 0;

		for (Bidder b : bidders) {
			if (b.getBidder() == ap) {
				timesBid = b.getTimesBid();
				break;
			}
		}

		return timesBid;
	}

	public int getConsecutiveBids(AuctionsPlayer ap) {
		Bidder bidder = getBidder(ap);

		if (bidder == null)
			return 0;
		else
			return bidder.getConsecutiveBids();
	}

	public List<Bidder> getBidders() {
		return bidders;
	}

	public List<Bidder> getLosingBidders() {
		ArrayList<Bidder> losing = new ArrayList<>(bidders);
		losing.remove(getLastBidder());

		return losing;
	}

	public Bidder getBidder(AuctionsPlayer ap) {
		for (Bidder bid : bidders) {
			if (bid.getBidder().equals(ap))
				return bid;
		}

		return null;
	}

	public List<Bidder> getBiddersHighestToLowest() {
		return bidders.stream().sorted(Comparator.comparingDouble(Bidder::getAmount)).
				collect(Collectors.toList());
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
				BaseComponent extra = new TextComponent();
				ChatColor color = null;

				if (arg.startsWith("&")) {
					color = ChatColor.getByChar(arg.charAt(1));
					arg = arg.substring(2);
				} else {
					List<BaseComponent> extraList = main.getExtra();

					if (extraList == null) {
						color = ChatColor.WHITE;
					}
				}

				if (arg.contains("%item%")) {
					String minecraftItemName = ReflectionUtil.getMinecraftName(getItem());
					arg = arg.replace("%item%", minecraftItemName);

					String[] split3 = arg.split("((?<=" + minecraftItemName + ")|(?=" + minecraftItemName + "))");

					List<BaseComponent> addtoExtra = new ArrayList<>();

					for (String s : split3) {
						if (s.equalsIgnoreCase(minecraftItemName)) {
							addtoExtra.add(new TranslatableComponent(s));
						} else {
							addtoExtra.add(new TextComponent(s));
						}
					}

					BaseComponent[] hover = {new TextComponent(ReflectionUtil.getItemAsJson(getItem()))};

					extra.setHoverEvent(new HoverEvent(Action.SHOW_ITEM, hover));

					addtoExtra.forEach(extra::addExtra);
				} else {
					((TextComponent) extra).setText(arg);
				}

				if (color != null) {
					extra.setColor(color);
				}

				main.addExtra(extra);
			}
		}

		return main;
	}
}
