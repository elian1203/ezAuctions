package net.urbanmc.ezauctions.datastorage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQLStorage extends SQLStorage {

    private HikariDataSource hikariDS;

    public MySQLStorage(EzAuctions plugin) {
        super(plugin);

        SAVE_PLAYER_STMT =  "INSERT INTO AUCTION_PLAYERS (player, ignoringSpam, ignoringAll, ignoringScoreboard)" +
                " VALUES(?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                " ignoringSpam = ?, ignoringAll = ?, ignoringScoreboard = ?";
        HAS_SETTINGS_TABLE = "SHOW TABLES LIKE 'EZAUC_SETTINGS'";
    }

    @Override
    public boolean testAccess() {
        return establishDB() && runVersioning();
    }

    private boolean establishDB() {
        FileConfiguration config = ConfigManager.getConfig();
        String host = config.getString("data.mysql.host", "host");
        String database = config.getString("data.mysql.database");
        String user = config.getString("data.mysql.username");
        String pass = config.getString("data.mysql.password");
        int port = config.getInt("data.mysql.port");
        boolean ssl = config.getBoolean("data.mysql.useSSL", false);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ezAuctionPool");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(pass);
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl);

        try {
            hikariDS = new HikariDataSource(hikariConfig);
        } catch (HikariPool.PoolInitializationException exception) {
            plugin.getLogger().severe("[ezAuctions] Error connecting to SQL Database. Please make sure everything is configured properly.");
            hikariDS = null;
            return false;
        }

        return true;
    }

    @Override
    protected Connection getConnection() {
        try {
            return hikariDS.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[ezAuctions] Could not connect to MySQL database!", e);
        }
        return null;
    }

    @Override
    public void finish() {
        if (hikariDS != null) hikariDS.close();
    }
}
