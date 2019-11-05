package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLLiteStorage extends SQLStorage {

    private final File file;

    private Connection con;

    public SQLLiteStorage(EzAuctions plugin) {
        super(plugin);

        this.file = new File(plugin.getDataFolder(), "auctionplayers.db");

        SAVE_PLAYER_STMT =  "INSERT INTO AUCTION_PLAYERS (player, ignoringSpam, ignoringAll, ignoringScoreboard)" +
                " VALUES(?, ?, ?, ?)" +
                " ON CONFLICT(player)" +
                " DO UPDATE SET ignoringSpam = ?, ignoringAll = ?, ignoringScoreboard = ?";
    }

    @Override
    public boolean testAccess() {
        return createFile() && createTables();
    }

    private boolean createFile() {
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

        return true;
    }

    protected Connection getConnection() {
        try {
            if (con != null && !con.isClosed())
                return con;

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + file);

            return con;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }

        return null;
    }

}
