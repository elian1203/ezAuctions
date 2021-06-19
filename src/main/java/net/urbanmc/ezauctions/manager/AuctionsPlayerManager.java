package net.urbanmc.ezauctions.manager;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.datastorage.DataSource;
import net.urbanmc.ezauctions.object.AuctionsPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class AuctionsPlayerManager {

    private static AuctionsPlayerManager instance = new AuctionsPlayerManager();

    private DataSource dataSource;

    private Map<UUID, AuctionsPlayer> players;

    public void setDataSource(DataSource source) {
        this.dataSource = source;
    }

    public static AuctionsPlayerManager getInstance() {
        return instance;
    }

    public void loadData() {
        players = dataSource.load();
    }

    public void saveAndDisable() {
        if (dataSource != null) {
            if (ConfigManager.getConfig().getBoolean("data.save-on-disable", true))
                // The reason it is using async save is to push a save task to the executor
                // which is then blocked by the finish method.
                dataSource.asyncSave(getPlayers());

            dataSource.finish();
        }
    }

    public void asyncSaveData() {
        dataSource.asyncSave(getPlayers());
    }

    public void saveBooleans(AuctionsPlayer player) {
        dataSource.updateBooleanValue(player.clone());
    }

    public void saveIgnored(AuctionsPlayer player) {
        dataSource.updateIgnored(player.clone());
    }

    public void saveItems(AuctionsPlayer player) {
        dataSource.updateItems(player.clone());
    }

    public void reloadDataSource(EzAuctions plugin) {
        if (dataSource.preventReload()) return;

        DataSource newDataSource = DataSource.determineDataSource(plugin);

        // A new data source will only be loaded if the new data source is valid, not the same as the old one
        // and can establish proper access.
        if (newDataSource != null) {
            if (!dataSource.getClass().isInstance(newDataSource)
                    && newDataSource.testAccess()) {
                dataSource.finish();
                dataSource = newDataSource;
            }
            // Properly shutdown the new data source, even if it's invalid.
            else {
                newDataSource.finish();
            }
        }
    }

    public AuctionsPlayer getPlayer(UUID id) {
        if (!players.containsKey(id)) {
            createPlayer(id);
        }

        return players.get(id);
    }

    public void createPlayer(UUID id) {
        AuctionsPlayer ap = players.get(id);

        if (ap == null) {
            ap = new AuctionsPlayer(id, false, false, false, new ArrayList<>(), new ArrayList<>());
            players.put(id, ap);
            saveBooleans(ap);
        }
    }

    public Collection<AuctionsPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }
}
