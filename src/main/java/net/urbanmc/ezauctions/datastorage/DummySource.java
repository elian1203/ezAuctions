package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.AuctionsPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary DataSource implementation meant for plugins that
 * are going to implement their own datasource later in the load
 * process.
 *
 * This does not load anything and will throw errors on save calls.
 *
 * NOTE: PLUGIN DATASOURCES SHOULD EXTEND {@link DataSource}, NOT THIS CLASS.
 */
public class DummySource extends DataSource {

    public DummySource(EzAuctions plugin) {
        super(plugin);
    }

    @Override
    protected void save(List<AuctionsPlayer> auctionPlayers) {
        throw new UnsupportedOperationException("Save operation not supported while using Dummy Data Source!");
    }

    @Override
    public List<AuctionsPlayer> load() {
        return new ArrayList<>();
    }

    @Override
    public boolean testAccess() {
        return true;
    }
}
