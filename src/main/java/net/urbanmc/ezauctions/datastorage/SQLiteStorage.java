package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLiteStorage extends SQLStorage {

    private String filePath;

    public SQLiteStorage(EzAuctions plugin) {
        super(plugin);

        SAVE_PLAYER_STMT =  "INSERT INTO AUCTION_PLAYERS (player, ignoringSpam, ignoringAll, ignoringScoreboard)" +
                " VALUES(?, ?, ?, ?)" +
                " ON CONFLICT(player)" +
                " DO UPDATE SET ignoringSpam = ?, ignoringAll = ?, ignoringScoreboard = ?";

        HAS_SETTINGS_TABLE = "SELECT * FROM sqlite_master WHERE name ='SETTINGS' and type='table'";
    }

    @Override
    public boolean testAccess() {
        return createFile() && runVersioning();
    }

    private boolean createFile() {
        final File file = new File(plugin.getDataFolder(), "auctionplayers.db");

        if (!file.getParentFile().isDirectory())
            file.getParentFile().mkdir();

        if (!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create database file.", e);
                return false;
            }
        }

        filePath = file.getPath();

        return true;
    }

    protected Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + filePath);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }

        return null;
    }

}
