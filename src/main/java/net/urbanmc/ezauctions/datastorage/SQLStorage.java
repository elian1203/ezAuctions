package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public abstract class SQLStorage extends DataSource{

    /*
    * This class handles the SQL Logic for both MySQL and SQLite
    * The only difference in logic between the two is the saving player statement which is set
    * by the respective classes.
    *
    * The AuctionPlayer object is divided into three tables: one that contains the boolean values, another that
    * contains a list of ignored players, and another for offline items.
    *
    * Loading:
    *   We cannot join the tables because the ignored table and the items table do not have primary keys. They are
    *   KV lists with duplicate keys and values. Hence, while loading we have to load data from each table separately.
    *   Thus, we load the ignored players and offline items first and store them in hashmaps. Then we load the boolean auction player
    *   table and create the auction players there.
    *
    * Saving:
    *   There are 4 separate save methods: three that update the specific table, and one full save method.
    *   The full save method passes through the auction player list once and uses three prepared statements,
    *   so it only has to execute three large batch queries at the end.
     */

    // All SQL statements/queries.

    // Create table queries
    private final String CREATE_PLAYER_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS " +
            "( player CHAR(36) NOT NULL PRIMARY KEY," +
            " ignoringSpam BOOLEAN, ignoringAll BOOLEAN, ignoringScoreboard BOOLEAN)";

    // SQL is a relational database storage. We have to create separate tables for list objects
    private final String CREATE_IGNORED_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_IGNORED " +
            "( player CHAR(36), ignored CHAR(36))";

    private final String CREATE_ITEMS_TABLE = "CREATE TABLE IF NOT EXISTS AUCTION_PLAYERS_ITEMS " +
            "( player CHAR(36), items TEXT)";

    // Save statements
    String SAVE_PLAYER_STMT; // Abstract field set by respective SQLStorages

    private final String SAVE_IGNORED_STMT =  "INSERT INTO AUCTION_PLAYERS_IGNORED (player, ignored) VALUES(?, ?)";

    private final String SAVE_ITEMS_STMT =  "INSERT INTO AUCTION_PLAYERS_ITEMS (player, items) VALUES(?, ?)";

    // Delete queries/statements
    private final String DELETE_IGNORED_QUERY = "DELETE FROM AUCTION_PLAYERS_IGNORED";

    private final String DELETE_ITEMS_QUERY = "DELETE FROM AUCTION_PLAYERS_ITEMS";

    private final String DELETE_IGNORED_STMT = "DELETE FROM AUCTION_PLAYERS_IGNORED WHERE player = ?";

    private final String DELETE_ITEMS_STMT = "DELETE FROM AUCTION_PLAYERS_ITEMS WHERE player = ?";

    // Load queries
    private final String LOAD_PLAYERS_QRY = "SELECT * FROM AUCTION_PLAYERS";

    private final String LOAD_ITEMS_QRY = "SELECT * FROM AUCTION_PLAYERS_ITEMS";

    private final String LOAD_IGNORED_QRY = "SELECT * FROM AUCTION_PLAYERS_IGNORED";

    public SQLStorage(EzAuctions plugin) {
        super(plugin);
    }

    protected abstract Connection getConnection();

    protected boolean createTables() {
        try (Connection con = getConnection()) {

            if (con == null) return false;

            Statement statement = con.createStatement();

            statement.execute(CREATE_PLAYER_TABLE);
            statement.execute(CREATE_IGNORED_TABLE);
            statement.execute(CREATE_ITEMS_TABLE);

            statement.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating tables for SQL", e);
            return false;
        }

        return true;
    }

    @Override
    public void save(List<AuctionsPlayer> auctionPlayers) {
        try (Connection con = getConnection()) {

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
            plugin.getLogger().log(Level.SEVERE, "Error saving data for SQL", ex);
        }
    }

    @Override
    public void updateBooleanValue(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try (Connection connection = getConnection()) {

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
                plugin.getLogger().log(Level.SEVERE, "Error updating SQL player data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    @Override
    public void updateIgnored(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try (Connection connection = getConnection()) {
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
                plugin.getLogger().log(Level.SEVERE, "Error updating SQL ignored player data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    /**
     * Updates offline items for that specific player
     * @param list Takes in an auction player list.
     * @param player Takes in an auction player
     */

    @Override
    public void updateItems(List<AuctionsPlayer> list, final AuctionsPlayer player) {
        runAsync(() -> {
            lock();

            try (Connection connection = getConnection()) {

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
                plugin.getLogger().log(Level.SEVERE, "Error updating SQL player items data for player " + player, ex);
            } finally {
                unlock();
            }
        });
    }

    @Override
    public List<AuctionsPlayer> load() {
        ArrayList<AuctionsPlayer> auctionsPlayers = new ArrayList<>();

        try (Connection con = getConnection()) {

            if (con == null) return auctionsPlayers;

            // We only need one statement for all queries.
            Statement loadQuery = con.createStatement();

            // Load ignored users
            Map<UUID, List<UUID>> ignoredMap = new HashMap<>();

            // We use try-with-resources blocks because they create the resultset only in the needed scope
            // and automatically close the resultset at the end.
            try (ResultSet ignoredPlayersRslt = loadQuery.executeQuery(LOAD_IGNORED_QRY)) {

                while (ignoredPlayersRslt.next()) {
                    UUID playerUUID = UUID.fromString(ignoredPlayersRslt.getString("player"));
                    UUID ignoredID = UUID.fromString(ignoredPlayersRslt.getString("ignored"));

                    List<UUID> ignoredList = ignoredMap.get(playerUUID);

                    if (ignoredList == null) {
                        ignoredList = new ArrayList<>();
                        ignoredList.add(ignoredID);

                        ignoredMap.put(playerUUID, ignoredList);
                    } else
                        ignoredList.add(ignoredID);
                }
            }

            // Load offline items
            Map<UUID, List<ItemStack>> itemsMap = new HashMap<>();

            try (ResultSet itemsRslt = loadQuery.executeQuery(LOAD_ITEMS_QRY)) {

                while (itemsRslt.next()) {
                    UUID playerUUID = UUID.fromString(itemsRslt.getString("player"));
                    ItemStack stack = ItemUtil.deserialize(itemsRslt.getString("items"));

                    // Get the current item stack list for the player
                    List<ItemStack> stackList = itemsMap.get(playerUUID);

                    // Create a new list if the list doesn't exist.
                    if (stackList == null) {
                        stackList = new ArrayList<>();
                        stackList.add(stack);

                        itemsMap.put(playerUUID, stackList);
                    } else
                        stackList.add(stack);
                }
            }

            // Load the players from the main player table
            try (ResultSet resultSet = loadQuery.executeQuery(LOAD_PLAYERS_QRY)) {

                while (resultSet.next()) {
                    // Get the player UUID
                    UUID playerUUID = UUID.fromString(resultSet.getString("player"));

                    // Get the ignored players for this specific player
                    List<UUID> ignoredPlayers = ignoredMap.getOrDefault(playerUUID, new ArrayList<>());

                    // Get the offline items for this player
                    List<ItemStack> items = itemsMap.getOrDefault(playerUUID, new ArrayList<>());

                    // Create the auction player
                    AuctionsPlayer aP = new AuctionsPlayer(playerUUID,
                            resultSet.getBoolean("ignoringSpam"),
                            resultSet.getBoolean("ignoringAll"),
                            resultSet.getBoolean("ignoringScoreboard"),
                            ignoredPlayers,
                            items);

                    auctionsPlayers.add(aP);
                }
            }

            loadQuery.close();

        } catch (SQLException | IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error loading data for SQL", ex);
        }

        return auctionsPlayers;
    }
}
