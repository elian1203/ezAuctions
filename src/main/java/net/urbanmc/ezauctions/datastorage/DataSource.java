package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.manager.ConfigManager;
import net.urbanmc.ezauctions.object.AuctionsPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class DataSource {

    protected EzAuctions plugin;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DataSource(EzAuctions plugin) {
        this.plugin = plugin;
    }

    protected abstract void save(Collection<AuctionsPlayer> auctionPlayers);

    public abstract Map<UUID, AuctionsPlayer> load();

    public abstract boolean testAccess();

    // Default these methods to a full save
    public void updateIgnored(AuctionsPlayer player) {
        asyncSave(AuctionsPlayerManager.getInstance().getPlayers());
    }

    public void updateItems(AuctionsPlayer player) {
        asyncSave(AuctionsPlayerManager.getInstance().getPlayers());
    }

    public void updateBooleanValue(AuctionsPlayer player) {
        asyncSave(AuctionsPlayerManager.getInstance().getPlayers());
    }

    // This is for external custom data sources in order to prevent a creation of a new data source.
    public boolean preventReload() { return false; }

    // Method is called when the plugin is disabling or a datasource is changing
    public void finish() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            plugin.getLogger().warning("The storage executor was forcefully interrupted!");
        }
    }

    public void asyncSave(final Collection<AuctionsPlayer> players) {
        // Clone array list for thread-safe access
        // Note that this does not mean the inner-reads are thread-safe
        // However, since we lock the writes, it means any changes to the players will be re-saved anyway.
        List<AuctionsPlayer> cloneList = new ArrayList<>(players);

        runAsync(() -> {
            save(cloneList); // Create a clone of the array list
            cloneList.clear(); // Help GC
        });
    }

    protected void runAsync(Runnable run) {
        executor.submit(run);
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
