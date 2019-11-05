package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.Auction;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public abstract class SQLStorage extends DataSource{

    /*
     * Easy, but inefficient way:
     * Save entire list to database every time save is called.
     *
     * Harder to implement:
     * Schema Controlled Inserting
     */

    private final String CREATE_PLAYER_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS " +
            "( player CHAR(36) NOT NULL PRIMARY KEY," +
            " ignoringSpam BOOLEAN, ignoringAll BOOLEAN, ignoringScoreboard BOOLEAN)";

    // SQL is a relational database storage. We have to create separate tables for list objects
    private final String CREATE_IGNORED_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_IGNORED " +
            "( player CHAR(36), ignored CHAR(36))";

    private final String CREATE_ITEMS_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_ITEMS " +
            "( player CHAR(36), items TEXT)";

    String SAVE_PLAYER_STMT; // Abstract field set by respective SQLStorages

    private final String SAVE_IGNORED_STMT =  "INSERT INTO AUCTION_PLAYERS_IGNORED (player, ignored) VALUES(?, ?)";

    private final String SAVE_ITEMS_STMT =  "INSERT INTO AUCTION_PLAYERS_ITEMS (player, items) VALUES(?, ?)";

    private final String DELETE_IGNORED_QUERY = "DELETE FROM AUCTION_PLAYERS_IGNORED";

    private final String DELETE_ITEMS_QUERY = "DELETE FROM AUCTION_PLAYERS_ITEMS";

    private final String DELETE_IGNORED_STMT = "DELETE FROM AUCTION_PLAYERS_IGNORED WHERE player = ?";

    private final String DELETE_ITEMS_STMT = "DELETE FROM AUCTION_PLAYERS_ITEMS WHERE player = ?";

    private final String LOAD_PLAYERS_QRY = "SELECT * FROM AUCTION_PLAYERS_IGNORED";

    private final String LOAD_ITEMS_STMT = "SELECT items FROM AUCTION_PLAYERS_ITEMS WHERE player = ?";

    private final String LOAD_IGNORED_STMT = "SELECT ignored FROM AUCTION_PLAYERS_IGNORED WHERE player = ?";

    public SQLStorage(EzAuctions plugin) {
        super(plugin);
    }

    protected abstract Connection getConnection();

    protected boolean createTables() {
        try {
            Connection con = getConnection();

            if (con == null) return false;

            Statement statement = con.createStatement();

            statement.execute(CREATE_PLAYER_TABLE);
            statement.execute(CREATE_IGNORED_TABLE);
            statement.execute(CREATE_ITEMS_TABLE);

            statement.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error creating tables for SQLLite", e);
            return false;
        }

        return true;
    }

    @Override
    public void save(List<AuctionsPlayer> auctionPlayers) {
        try {
            Connection con = getConnection();

            if (con == null) return;

            // Since we are saving the entire list, we need to delete everything from the ignored and items table
            Statement deleteStmt = con.createStatement();

            deleteStmt.execute(DELETE_IGNORED_QUERY);
            deleteStmt.execute(DELETE_ITEMS_QUERY);

            // Now we formulate three prepared statements so we only have to pass-through the list once.
            PreparedStatement playerStatement = con.prepareStatement(SAVE_PLAYER_STMT);
            PreparedStatement ignoredStatement = con.prepareStatement(SAVE_IGNORED_STMT);
            PreparedStatement itemStatement = con.prepareStatement(SAVE_ITEMS_STMT);

            for (AuctionsPlayer player : auctionPlayers) {
                String id = player.getUniqueId().toString();

                // Insert the data into the player table
                playerStatement.setString(1, id);
                playerStatement.setBoolean(2, player.isIgnoringSpammy());
                playerStatement.setBoolean(3, player.isIgnoringAll());
                playerStatement.setBoolean(4, player.isIgnoringScoreboard());
                playerStatement.setBoolean(5, player.isIgnoringSpammy());
                playerStatement.setBoolean(6, player.isIgnoringAll());
                playerStatement.setBoolean(7, player.isIgnoringScoreboard());

                playerStatement.addBatch();

                // Insert data for the ignored table
                for (UUID ignored : player.getIgnoringPlayers()) {
                    ignoredStatement.setString(1, id);
                    ignoredStatement.setString(2, ignored.toString());
                    ignoredStatement.addBatch();
                }

                // Insert data for the items table
                for (ItemStack item : player.getOfflineItems()) {
                    itemStatement.setString(1, id);
                    itemStatement.setString(2, ItemUtil.serialize(item));
                    itemStatement.addBatch();
                }
            }

            // Execute all statements
            playerStatement.executeBatch();
            ignoredStatement.executeBatch();
            itemStatement.executeBatch();

            // Close all statements
            playerStatement.close();
            ignoredStatement.close();
            itemStatement.close();

        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error saving data for SQL", ex);
        }
    }

    private AuctionsPlayer getPlayerFromList(UUID player, List<AuctionsPlayer> list) {
        for (AuctionsPlayer aP : list) {
            if (aP.getUniqueId().equals(player))
                return aP.clone();
        }

        return null;
    }

    @Override
    public void updateBooleanValue(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try {
                Connection connection = getConnection();

                if (connection == null) return;

                PreparedStatement savePlayerStmt = connection.prepareStatement(SAVE_PLAYER_STMT);

                savePlayerStmt.setString(1, player.getUniqueId().toString());
                savePlayerStmt.setBoolean(2, player.isIgnoringSpammy());
                savePlayerStmt.setBoolean(3, player.isIgnoringAll());
                savePlayerStmt.setBoolean(4, player.isIgnoringScoreboard());
                savePlayerStmt.setBoolean(5, player.isIgnoringSpammy());
                savePlayerStmt.setBoolean(6, player.isIgnoringAll());
                savePlayerStmt.setBoolean(7, player.isIgnoringScoreboard());

                savePlayerStmt.execute();

                savePlayerStmt.close();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error updating SQL player data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    @Override
    public void updateIgnored(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try {
                Connection connection = getConnection();

                if (connection == null) return;

                String id = player.getUniqueId().toString();

                PreparedStatement deleteIgnoredStmt = connection.prepareStatement(DELETE_IGNORED_STMT);
                deleteIgnoredStmt.setString(1, id);

                deleteIgnoredStmt.execute();
                deleteIgnoredStmt.close();

                if (player.getIgnoringPlayers().isEmpty()) return; // Don't continue if no players are ignored

                PreparedStatement saveIgnoredStmt = connection.prepareStatement(SAVE_IGNORED_STMT);

                for (UUID ignored : player.getIgnoringPlayers()) {
                    saveIgnoredStmt.setString(1, id);
                    saveIgnoredStmt.setString(2, ignored.toString());
                    saveIgnoredStmt.addBatch();
                }

                saveIgnoredStmt.executeBatch();

                saveIgnoredStmt.close();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error updating SQL ignored player data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    @Override
    public void updateItems(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try {
                Connection connection = getConnection();

                if (connection == null) return;

                String id = player.getUniqueId().toString();

                PreparedStatement deleteItemsStmt = connection.prepareStatement(DELETE_ITEMS_STMT);
                deleteItemsStmt.setString(1, id);

                deleteItemsStmt.execute();
                deleteItemsStmt.close();

                if (player.getOfflineItems().isEmpty()) return; // Don't continue if list is empty

                PreparedStatement saveItemsStmt = connection.prepareStatement(SAVE_ITEMS_STMT);

                for (ItemStack item : player.getOfflineItems()) {
                    saveItemsStmt.setString(1, id);
                    saveItemsStmt.setString(2, ItemUtil.serialize(item));
                    saveItemsStmt.addBatch();
                }

                saveItemsStmt.executeBatch();

                saveItemsStmt.close();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error updating SQL player items data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    @Override
    public List<AuctionsPlayer> load() {
        ArrayList<AuctionsPlayer> auctionsPlayers = new ArrayList<>();

        try {
            Connection con = getConnection();

            if (con == null) return auctionsPlayers;

            // Load the players from the main player table
            Statement loadPlayersQuery = con.createStatement();

            ResultSet resultSet = loadPlayersQuery.executeQuery(LOAD_PLAYERS_QRY);

            while (resultSet.next()) {
                // Get the player ID as a string
                String playerID = resultSet.getString("player");

                // Get the ignored players for this specific player
                List<UUID> ignoredPlayers = new ArrayList<>();

                PreparedStatement ignoredStmt = con.prepareStatement(LOAD_IGNORED_STMT);
                ignoredStmt.setString(1, playerID);

                ResultSet ignoredResult = ignoredStmt.executeQuery();

                while (ignoredResult.next()) {
                    ignoredPlayers.add(UUID.fromString(ignoredResult.getString("ignored")));
                }

                ignoredResult.close();

                // Get the offline items for this player
                List<ItemStack> items = new ArrayList<>();

                PreparedStatement itemStmt = con.prepareStatement(LOAD_ITEMS_STMT);
                itemStmt.setString(1, playerID);

                ResultSet itemSet = itemStmt.executeQuery();

                while (itemSet.next()) {
                    items.add(ItemUtil.deserialize(itemSet.getString("items")));
                }

                itemSet.close();

                ignoredStmt.close();
                itemStmt.close();

                // Create the auction player
                AuctionsPlayer aP = new AuctionsPlayer(UUID.fromString(playerID),
                        resultSet.getBoolean("ignoringSpam"),
                        resultSet.getBoolean("ignoringAll"),
                        resultSet.getBoolean("ignoringScoreboard"),
                        ignoredPlayers,
                        items);

                auctionsPlayers.add(aP);
            }

        } catch (SQLException | IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error loading data for SQLLite", ex);
        }

        return auctionsPlayers;
    }
}
