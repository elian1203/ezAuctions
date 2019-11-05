package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQLStorage extends SQLStorage {
    private String host, database, user, pass;
    private int port;

    private Connection con;

    public MySQLStorage(EzAuctions plugin) {
        super(plugin);

        FileConfiguration config = ConfigManager.getConfig();
        this.host = config.getString("data.mysql.host", "host");
        this.database = config.getString("data.mysql.database");
        this.user = config.getString("data.mysql.user");
        this.pass = config.getString("data.mysql.password");
        this.port = config.getInt("data.mysql.port");

        SAVE_PLAYER_STMT =  "INSERT INTO AUCTION_PLAYERS (player, ignoringSpam, ignoringAll, ignoringScoreboard)" +
                " VALUES(?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                " ignoringSpam = ?, ignoringAll = ?, ignoringScoreboard = ?";
    }

    @Override
    public boolean testAccess() {
        return createDatabase(database) && createTables();
    }

    private boolean createDatabase(String databaseName) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/", user, pass);

            Statement statement = con.createStatement();

            String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            statement.execute(sql);

            statement.close();
            con.close();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Could not connect to MySQL database!" + databaseName, e);
            return false;
        }

        return true;
    }

    @Override
    protected Connection getConnection() {
        try {
            if (con != null && !con.isClosed())
                return con;

            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, pass);

            return con;
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Could not connect to MySQL database!", e);
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] You need the MySQL JBDC library. Google it. Put it in /lib folder.");
        }

        return null;
    }
}
