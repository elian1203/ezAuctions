package net.urbanmc.ezauctions.datastorage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQLStorage extends SQLStorage {

    private HikariDataSource hikariDS;
    private String database;

    public MySQLStorage(EzAuctions plugin) {
        super(plugin);

        SAVE_PLAYER_STMT =  "INSERT INTO AUCTION_PLAYERS (player, ignoringSpam, ignoringAll, ignoringScoreboard)" +
                " VALUES(?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                " ignoringSpam = ?, ignoringAll = ?, ignoringScoreboard = ?";
    }

    @Override
    public boolean testAccess() {
        return establishDB() && createDatabase(database) && createTables();
    }

    private boolean establishDB() {
        FileConfiguration config = ConfigManager.getConfig();
        String host = config.getString("data.mysql.host", "host");
        database = config.getString("data.mysql.database");
        String user = config.getString("data.mysql.user");
        String pass = config.getString("data.mysql.password");
        int port = config.getInt("data.mysql.port");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ezAuctionPool");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(pass);
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/");

        try {
            hikariDS = new HikariDataSource(hikariConfig);
        } catch (HikariPool.PoolInitializationException exception) {
            Bukkit.getLogger().severe("[ezAuctions] Error connecting to SQL Database. Please make sure everything is configured properly.");
            hikariDS = null;
            return false;
        }

        return true;
    }

    private boolean createDatabase(String databaseName) {
        try(Connection con = getConnection()) {
            Statement statement = con.createStatement();

            String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            statement.execute(sql);

            statement.close();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Could not connect to MySQL database!" + databaseName, e);
            return false;
        }

        return true;
    }

    @Override
    protected Connection getConnection() {
        try {
            return hikariDS.getConnection();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Could not connect to MySQL database!", e);
        }
        return null;
    }
}
