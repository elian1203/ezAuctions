package me.elian.ezauctions.scheduler;

import me.elian.ezauctions.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class TaskSchedulerBase implements TaskScheduler {
	private final Plugin plugin;
	private final Logger logger;
	private boolean shuttingDown;

	protected TaskSchedulerBase(@NotNull Plugin plugin, @NotNull Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	public void shutdown() {
		shuttingDown = true;
	}

	public void runPlayerRegionTask(@NotNull Runnable runnable, @NotNull Player player) {
		Runnable wrapped = wrapRunnable(runnable);
		if (shuttingDown) {
			wrapped.run();
		} else {
			schedulePlayerRegionTask(plugin, wrapped, player);
		}
	}

	public void runAsyncPlayerCommandTask(@NotNull Player player, @NotNull Runnable runnable) {
		if (shuttingDown) {
			runnable.run();
			return;
		}

		scheduleAsyncTask(plugin, () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				logger.severe("Exception occurred while processing command!", e);
				player.sendMessage(Component.text("Errors occurred while processing your command. " +
						"Please contact the server administrator."));
				throw e;
			}
		});
	}

	public void runSyncTask(@NotNull Runnable runnable) {
		Runnable wrapped = wrapRunnable(runnable);
		if (shuttingDown) {
			wrapped.run();
		} else {
			scheduleGlobalSyncTask(plugin, wrapped);
		}
	}

	public void runAsyncTask(@NotNull Runnable runnable) {
		Runnable wrapped = wrapRunnable(runnable);
		if (shuttingDown) {
			wrapped.run();
		} else {
			scheduleAsyncTask(plugin, wrapped);
		}
	}

	public void runAsyncDelayedTask(@NotNull Runnable runnable, long delaySeconds) {
		Runnable wrapped = wrapRunnable(runnable);
		if (shuttingDown) {
			wrapped.run();
		} else {
			scheduleAsyncDelayedTask(plugin, wrapped, delaySeconds);
		}
	}

	public CancellableTask runAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                             long initialDelaySeconds, long intervalSeconds) {
		if (shuttingDown) {
			throw new IllegalStateException("Cannot schedule a repeating task while server shutting down!");
		} else {
			return scheduleAsyncRepeatingTask(plugin, runnable, initialDelaySeconds, intervalSeconds);
		}
	}

	protected abstract void schedulePlayerRegionTask(Plugin plugin, Runnable runnable, Player player);

	protected abstract void scheduleGlobalSyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable);

	protected abstract void scheduleAsyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable);

	protected abstract void scheduleAsyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                                 long delaySeconds);

	protected abstract CancellableTask scheduleAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                                              long initialDelaySeconds, long intervalSeconds);


	private Runnable wrapRunnable(Runnable runnable) {
		return () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				logger.severe("Exception occurred while processing task!", e);
				throw e;
			}
		};
	}
}
