package net.urbanmc.ezauctions.manager;

import net.md_5.bungee.api.chat.TranslatableComponent;
import net.milkbowl.vault.chat.Chat;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ScoreboardManager {

    private static ScoreboardManager instance = new ScoreboardManager();

    private Scoreboard board;
    private Objective objective;
    private Team price, time;

    private ScoreboardManager() {
        createScoreboard();
    }

    public static ScoreboardManager getInstance() {
        return instance;
    }

    private void createScoreboard() {
        board = Bukkit.getScoreboardManager().getNewScoreboard();

        String displayName = Messages.getString("scoreboard.title");

        try {
            objective = board.registerNewObjective("auctionBoard", "dummy", displayName);
        }
        catch (NoSuchMethodError ex) {
            // 1.12 Compat
            objective = board.registerNewObjective("auctionBoard", "dummy");
            objective.setDisplayName(displayName);
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        price = board.registerNewTeam("auctionPrice");
        time = board.registerNewTeam("auctionTime");

        price.addEntry(ChatColor.BLACK + "" + ChatColor.WHITE);
        time.addEntry(ChatColor.RED + "" + ChatColor.WHITE);
    }

    public void updateScoreboard(Auction auc) {
        if (!isScoreboardActive())
            setupScoreboard(auc);

        double currentPrice = auc.getStartingPrice();

        if (!auc.isSealed() && !auc.getBidList().isEmpty()) {
            currentPrice = auc.getBidList().getTopBid().getAmount();
        }

        String priceString = auc.isSealed() ?
                Messages.getString("scoreboard.current_bid_sealed", currentPrice) :
                Messages.getString("scoreboard.current_bid", currentPrice);

        try {
            price.setPrefix(priceString);
        } catch (IllegalArgumentException ex) {
            // 1.12 Compatibility
            EzAuctions.getPluginLogger().warning("Price String longer than 16 characters! Please shorten in messages.properties!");
            // Error-values are hardcoded so it's certain that the same exception won't be thrown
            price.setPrefix(auc.isSealed() ?
                    ChatColor.BLUE  + "Bid: " + ChatColor.GOLD + currentPrice :
                    ChatColor.BLUE + "Price: " + ChatColor.GOLD + currentPrice);
        }

        try {
            time.setPrefix(Messages.getString("scoreboard.time", auc.getAuctionTime()));
        } catch (IllegalArgumentException ex) {
            // 1.12 Compatibility
            EzAuctions.getPluginLogger().warning("Time string longer than 16 characters! Please shorten in messages.properties!");
            time.setPrefix(ChatColor.BLUE + "Time: " + ChatColor.GOLD + auc.getAuctionTime());
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            AuctionsPlayer ap = AuctionsPlayerManager.getInstance().getPlayer(p.getUniqueId());

            if (ap.isIgnoringScoreboard() || ap.getIgnoringPlayers().contains(auc.getAuctioneer().getUniqueId()))
                continue;

            if (p.getScoreboard() == board)
                continue;

            if (!ConfigManager.getConfig().getBoolean("scoreboard.overlap")
                    && p.getScoreboard().getEntries().size() > 0)
                continue;

            p.setScoreboard(board);
        }
    }

    private void addMeta(List<String> list, Auction auc) {
        List<String> metaOptions = ConfigManager.getConfig().getStringList("scoreboard.meta");

        for (String option : metaOptions) {
            if (option.equalsIgnoreCase("autobuy")) {
                double autoBuy = auc.getAutoBuy();

                if (autoBuy > 0) {
                    list.add(Messages.getString("scoreboard.autobuy", autoBuy));
                }
            } else if (option.equalsIgnoreCase("skull")) {
                if (ConfigManager.getConfig().getBoolean("auctions.toggles.broadcast-head") &&
                        auc.getItem().getItemMeta() instanceof SkullMeta) {
                    SkullMeta meta = (SkullMeta) auc.getItem().getItemMeta();

                    if (meta.hasOwner()) {
                        list.add(Messages.getString("scoreboard.skull", meta.getOwner()));
                    }
                }
            } else if (option.equalsIgnoreCase("repair")) {
                if (ConfigManager.getConfig().getBoolean("auctions.toggles.broadcast-repair")) {
                    int xpToRepair = ReflectionUtil.getXPForRepair(auc.getItem());

                    if (xpToRepair == -1) {
                        list.add(Messages.getString("scoreboard.repair.impossible"));
                    } else if (xpToRepair > 0) {
                        list.add(Messages.getString("scoreboard.repair.price", xpToRepair));
                    }
                }
            } else if (option.equalsIgnoreCase("sealed")) {
                if (auc.isSealed()) {
                    list.add(Messages.getString("scoreboard.sealed"));
                }
            }
        }
    }

    public boolean isScoreboardActive() {
        return board.getEntries().size() > 0;
    }

    public void setupScoreboard(Auction auc) {
        String auctioneer = Bukkit.getOfflinePlayer(auc.getAuctioneer().getUniqueId()).getName();
        int amount = auc.getAmount();
        String item = getItemName(auc.getItem());

        double increment = auc.getIncrement();

        String body = Messages.getString("scoreboard.body",
                auctioneer,
                amount,
                item,
                ChatColor.BLACK + "" + ChatColor.WHITE, // price
                increment,
                ChatColor.RED + "" + ChatColor.WHITE); // time

        String[] lines = body.split("\n");

        List<String> list = new ArrayList<>(Arrays.asList(lines));

        addMeta(list, auc);

        Collections.reverse(list);

        for (int i = 0; i < list.size(); i++) {
            objective.getScore(list.get(i)).setScore(i);
        }
    }

    private void clearBoard() {
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
    }

    public void removeBoards() {
        clearBoard();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() == board) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }
    }

    private String getItemName(ItemStack item) {
        String minecraftItemName = ReflectionUtil.getMinecraftName(item);
        String itemName = new TranslatableComponent(minecraftItemName).toPlainText();

        if (ConfigManager.getConfig().getBoolean("auctions.toggles.display-custom-name") && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()) {
            itemName = item.getItemMeta().getDisplayName();
        }

        return itemName;
    }

    public void reload() {
        String displayName = Messages.getString("scoreboard.title");
        objective.setDisplayName(displayName);
    }
}
