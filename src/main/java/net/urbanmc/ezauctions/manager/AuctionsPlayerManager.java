package net.urbanmc.ezauctions.manager;

import net.urbanmc.ezauctions.datastorage.DataSource;
import net.urbanmc.ezauctions.object.AuctionsPlayer;

import java.util.*;

public class AuctionsPlayerManager {

    private static AuctionsPlayerManager instance = new AuctionsPlayerManager();

    private DataSource dataSource;

    private List<AuctionsPlayer> players;

    public void setDataSource(DataSource source) {
        this.dataSource = source;
    }

    public static AuctionsPlayerManager getInstance() {
        return instance;
    }

    public void loadData() {
        players = dataSource.load();
    }

    public void syncFullSaveData() {
        dataSource.syncSave(players);
    }

    public void saveBooleans(AuctionsPlayer player) {
        dataSource.updateBooleanValue(players, player.clone());
    }

    public void saveIgnored(AuctionsPlayer player) {
        dataSource.updateIgnored(players, player.clone());
    }

    public void saveItems(AuctionsPlayer player) {
        dataSource.updateItems(players, player.clone());
    }

    public AuctionsPlayer getPlayer(UUID id) {
        for (AuctionsPlayer ap : players) {
            if (ap.getUniqueId().equals(id))
                return ap;
        }

        return null;
    }

    public void createPlayer(UUID id) {
        AuctionsPlayer ap = getPlayer(id);

        if (ap == null) {
            ap = new AuctionsPlayer(id, false, false, false, new ArrayList<>(), new ArrayList<>());
            players.add(ap);
            saveBooleans(ap);
        }
    }
}
