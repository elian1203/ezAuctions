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

import java.util.*;
import java.util.stream.Collectors;

public class Auction {

    private AuctionsPlayer auctioneer;
    private ItemStack item;
    private int amount, auctionTime;
    private double starting, increment, autoBuy;
    private boolean isSealed;
    private BidList bidders;
    private String world;

    public Auction(AuctionsPlayer auctioneer, ItemStack item, int amount, int auctionTime, double starting,
                   double increment, double autoBuy, boolean isSealed, String world) {
        this.auctioneer = auctioneer;
        this.item = item;
        this.amount = amount;
        this.auctionTime = auctionTime;
        this.starting = starting;
        this.increment = increment;
        this.autoBuy = autoBuy;
        this.isSealed = isSealed;
        this.world = world;
        bidders = new BidList();
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

    public String getWorld() {
        return world;
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

        if (lastBidder == null || lastBidder == upcomingLatestBidder)
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

            String message = Messages.getInstance().getStringWithoutColoring("auction.bid", bidder, amount, "%item%",
                    getAmount(), getAuctionTime());

            MessageUtil.broadcastSpammy(auctioneer.getUniqueId(), formatMessage(message));
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
        return bidders.getTopBid();
    }

    public int getTimesBid(AuctionsPlayer ap) {
        Bidder bidder = getBidder(ap);

        if (bidder != null)
            return bidder.getTimesBid();
        else
            return 0;
    }

    public int getConsecutiveBids(AuctionsPlayer ap) {
        Bidder bidder = getBidder(ap);

        if (bidder == null)
            return 0;
        else
            return bidder.getConsecutiveBids();
    }

    // Keep for API compatibility
    public List<Bidder> getBidders() {
        return bidders;
    }

    public BidList getBidList() {
        return bidders;
    }

    public List<Bidder> getLosingBidders() {
        return bidders.toArrayList(0, bidders.size() - 1);
    }

    public Bidder getBidder(AuctionsPlayer ap) {
        return bidders.get(ap);
    }

    public List<Bidder> getBiddersHighestToLowest() {
        return bidders.stream().sorted(Comparator.comparingDouble(Bidder::getAmount)).
                collect(Collectors.toList());
    }

    public boolean isParticipant(UUID id) {
        return auctioneer.getUniqueId().equals(id) || bidders.contains(id);
    }

    public BaseComponent[] getStartingMessage() {
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

        return formatMessage(message.toString());
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

    public BaseComponent[] formatMessage(String message) {
        if (message.contains("%item%")) {
            String[] split = message.split("%item%");
            BaseComponent[] first = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', split[0])),
                    second = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                            split[1]));

            BaseComponent lastComp = first[first.length - 1];

            String minecraftItemName = ReflectionUtil.getMinecraftName(getItem());
            String itemName = minecraftItemName;

            if (ConfigManager.getConfig().getBoolean("auctions.toggles.display-custom-name") && getItem().hasItemMeta()
                    && getItem().getItemMeta().hasDisplayName()) {
                itemName = getItem().getItemMeta().getDisplayName();
            }

            BaseComponent[] hover = { new TextComponent(ReflectionUtil.getItemAsJson(getItem())) };

            BaseComponent item;

            if (itemName.equals(minecraftItemName)) {
                item = new TranslatableComponent(itemName);
            } else {
                item = new TextComponent(itemName);
            }

            item.setColor(lastComp.getColor());
            item.setBold(lastComp.isBold());
            item.setItalic(lastComp.isItalic());
            item.setStrikethrough(lastComp.isStrikethrough());
            item.setUnderlined(lastComp.isUnderlined());

            item.setHoverEvent(new HoverEvent(Action.SHOW_ITEM, hover));

            List<BaseComponent> list = new ArrayList<>();

            list.addAll(Arrays.asList(first));
            list.add(item);
            list.addAll(Arrays.asList(second));

            BaseComponent[] combined = new BaseComponent[list.size()];
            combined = list.toArray(combined);

            return combined;
        } else {
            return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
