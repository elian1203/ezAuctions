package me.elian.ezauctions.data;

import com.google.inject.ImplementedBy;
import me.elian.ezauctions.model.AuctionPlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ImplementedBy(OrmLiteDatabase.class)
public interface Database {
	@NotNull CompletableFuture<AuctionPlayer> getAuctionPlayer(@NotNull UUID id);

	void saveAuctionPlayer(@NotNull AuctionPlayer ap);

	void reconnect() throws SQLException;

	void shutdown();
}
