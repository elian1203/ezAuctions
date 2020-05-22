package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DataSource {

    protected EzAuctions plugin;

    private final AtomicBoolean lock = new AtomicBoolean(false);

    public DataSource(EzAuctions plugin) {
        this.plugin = plugin;
    }

    protected abstract void save(List<AuctionsPlayer> auctionPlayers);

    public abstract List<AuctionsPlayer> load();

    public abstract boolean testAccess();

    // Default these methods to a full save
    public void updateIgnored(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncSave(list);
    }

    public void updateItems(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncSave(list);
    }

    public void updateBooleanValue(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncSave(list);
    }

    // This is for external custom data sources in order to prevent a creation of a new data source.
    public boolean preventReload() { return false; }

    // Method is called when the plugin is disabling or a datasource is changing
    public void finish() {}

    public void asyncSave(final List<AuctionsPlayer> players) {
        // Clone array list for thread-safe access
        // Note that this does not mean the inner-reads are thread-safe
        // However, since we lock the writes, it means any changes to the players will be re-saved anyway.
        List<AuctionsPlayer> cloneList = new ArrayList<>(players);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            lock();
            try {
                save(cloneList); // Create a clone of the array list
            } finally {
                unlock();
            }

            cloneList.clear(); // Help GC
        });
    }

    public void syncSave(final List<AuctionsPlayer> players) {
        lock();
        try {
            save(players);
        } finally {
            unlock();
        }
    }

    protected void runAsync(Runnable run) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, run);
    }

    protected void lock() {
        synchronized (lock) {
            while (lock.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }

            lock.set(true);
        }
    }

    protected void unlock() {
        synchronized (lock) {
            lock.set(false);
            lock.notifyAll();
        }
    }

    // This method will block the thread until data I/O is finished
    public void waitForFinish() {
        synchronized (lock) {
            while (lock.get()) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public static DataSource determineDataSource(EzAuctions plugin) {
        switch (ConfigManager.getConfig().getString("data.storage-type", "json").toLowerCase()) {
            case "json":
            case "gson":
                return new JSONStorage(plugin);
            case "sql":
            case "mysql":
                return new MySQLStorage(plugin);
            case "sqlite":
                return new SQLiteStorage(plugin);
            case "external":
                return new DummySource(plugin);
            default:
                plugin.getLogger().severe("Invalid data storage type! Please fix data storage type in the config.");
                return null;
        }
    }
}
