package me.elian.ezauctions.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.PluginLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

@Singleton
public class BukkitTaskScheduler extends TaskSchedulerBase {

	@Inject
	public BukkitTaskScheduler(@NotNull Plugin plugin, @NotNull PluginLogger logger) {
		super(plugin, logger);
	}

	@Override
	protected void schedulePlayerRegionTask(Plugin plugin, Runnable runnable, Player player) {
		plugin.getServer().getScheduler().runTask(plugin, runnable);
	}

	@Override
	protected void scheduleGlobalSyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
		plugin.getServer().getScheduler().runTask(plugin, runnable);
	}

	@Override
	protected void scheduleAsyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
	}

	@Override
	protected void scheduleAsyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delaySeconds) {
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delaySeconds * 20L);
	}

	@Override
	protected CancellableTask scheduleAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                                     long initialDelaySeconds, long intervalSeconds) {
		BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable,
				initialDelaySeconds * 20L, intervalSeconds * 20L);
		return task::cancel;
	}
}
