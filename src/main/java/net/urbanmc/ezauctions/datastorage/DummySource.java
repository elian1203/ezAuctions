package net.urbanmc.ezauctions.datastorage;

import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.object.AuctionsPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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
    protected void save(Collection<AuctionsPlayer> auctionPlayers) {
        throw new UnsupportedOperationException("Save operation not supported while using Dummy Data Source!");
    }

    @Override
    public Map<UUID, AuctionsPlayer> load() {
        return Collections.emptyMap();
    }

    @Override
    public boolean testAccess() {
        return true;
    }
}
