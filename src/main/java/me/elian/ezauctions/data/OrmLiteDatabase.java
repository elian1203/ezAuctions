package me.elian.ezauctions.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.controller.ConfigController;
import me.elian.ezauctions.model.AuctionPlayer;
import me.elian.ezauctions.model.AuctionPlayerIgnore;
import me.elian.ezauctions.model.SavedItem;
import me.elian.ezauctions.scheduler.TaskScheduler;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Singleton
public class OrmLiteDatabase implements Database {
	private final Logger logger;
	private final TaskScheduler scheduler;
	private final ConfigController config;
	private final Object connectingMonitor = new Object();
	private boolean connecting;
	private ConnectionSource connectionSource;
	private Dao<AuctionPlayer, UUID> auctionPlayerDao;

	@Inject
	public OrmLiteDatabase(Logger logger, ConfigController config, TaskScheduler scheduler) {
		this.logger = logger;
		this.config = config;
		this.scheduler = scheduler;
		scheduler.runAsyncTask(() -> {
			try {
				reconnect();
			} catch (Exception e) {
				logger.severe("Could not connect to database! Check your connection string!", e);
			}
		});
	}

	@Override
	public @NotNull CompletableFuture<AuctionPlayer> getAuctionPlayer(@NotNull UUID id) {
		CompletableFuture<AuctionPlayer> future = new CompletableFuture<>();

		scheduler.runAsyncTask(() -> {
			if (connectionSource != null) {
				try {
					AuctionPlayer auctionPlayer = auctionPlayerDao.queryForId(id);
					if (auctionPlayer == null) {
						auctionPlayer = new AuctionPlayer(id);
						auctionPlayerDao.create(auctionPlayer);
						auctionPlayer = auctionPlayerDao.queryForId(id);
					}

					future.complete(auctionPlayer);
					return;
				} catch (Exception e) {
					logger.severe("Could not get auction player!", e);
					future.complete(new AuctionPlayer(id));
					return;
				}
			}

			if (connecting) {
				try {
					synchronized (connectingMonitor) {
						if (connecting) {
							connectingMonitor.wait();
						}
					}

					future.complete(getAuctionPlayer(id).get());
				} catch (InterruptedException | ExecutionException e) {
					logger.severe("Exception when waiting for database connection", e);
				}
			} else {
				logger.severe("Could not get auction player due to database connection failure!");
			}

			future.complete(new AuctionPlayer(id));
		});

		return future;
	}

	@Override
	public void saveAuctionPlayer(@NotNull AuctionPlayer ap) {
		scheduler.runAsyncTask(() -> {
			if (connectionSource != null) {
				try {
					auctionPlayerDao.createOrUpdate(ap);
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}

			if (connecting) {
				try {
					synchronized (connectingMonitor) {
						if (connecting) {
							connectingMonitor.wait();
						}
					}

					saveAuctionPlayer(ap);
				} catch (InterruptedException e) {
					logger.severe("Could not wait");
				}
			} else {
				logger.severe("Could not get auction player due to database connection failure!");
			}
		});
	}

	public void reconnect() throws SQLException {
		synchronized (connectingMonitor) {
			connecting = true;
			if (connectionSource != null) {
				ConnectionSource connectionSourceTemp = connectionSource;
				connectionSource = null;
				try {
					connectionSourceTemp.close();
				} catch (Exception ignored) {
				}
			}

			try {
				String connectionString = config.getConfig().getString("data.connection-string");
				String user = config.getConfig().getString("data.username");
				if (user != null && !user.isBlank()) {
					String pass = config.getConfig().getString("data.password");
					connectionSource = new JdbcPooledConnectionSource(connectionString, user, pass);
				} else {
					connectionSource = new JdbcPooledConnectionSource(connectionString);
				}

				auctionPlayerDao = DaoManager.createDao(connectionSource, AuctionPlayer.class);

				try {
					auctionPlayerDao.queryForId(UUID.randomUUID());
				} catch (SQLException e) {
					// create tables if an exception is generated
					TableUtils.createTableIfNotExists(connectionSource, AuctionPlayer.class);
					TableUtils.createTableIfNotExists(connectionSource, AuctionPlayerIgnore.class);
					TableUtils.createTableIfNotExists(connectionSource, SavedItem.class);
				}

				connectingMonitor.notifyAll();
			} catch (Exception e) {
				connecting = false;
				connectingMonitor.notifyAll();
				throw e;
			}
		}
	}

	@Override
	public void shutdown() {
		if (connectionSource != null) {
			try {
				connectionSource.close();
				connectionSource = null;
			} catch (Exception ignored) {
			}
		}
	}
}
