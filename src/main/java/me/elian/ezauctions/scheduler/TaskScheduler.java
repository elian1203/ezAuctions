package me.elian.ezauctions.scheduler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface TaskScheduler {
	void shutdown();

	void runPlayerRegionTask(@NotNull Runnable runnable, @NotNull Player player);

	void runAsyncPlayerCommandTask(@NotNull Player player, @NotNull Runnable runnable);

	void runSyncTask(@NotNull Runnable runnable);

	void runAsyncTask(@NotNull Runnable runnable);

	void runAsyncDelayedTask(@NotNull Runnable runnable, long delaySeconds);

	CancellableTask runAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                      long initialDelaySeconds, long intervalSeconds);
}
