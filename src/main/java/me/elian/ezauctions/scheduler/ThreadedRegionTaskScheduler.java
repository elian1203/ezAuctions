package me.elian.ezauctions.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.elian.ezauctions.PluginLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@Singleton
public class ThreadedRegionTaskScheduler extends TaskSchedulerBase {
	@Inject
	public ThreadedRegionTaskScheduler(@NotNull Plugin plugin, @NotNull PluginLogger logger) {
		super(plugin, logger);
	}

	@Override
	protected void schedulePlayerRegionTask(Plugin plugin, Runnable runnable, Player player) {
		player.getScheduler().run(plugin, task -> {
			if (!task.isCancelled()) {
				runnable.run();
			}
		}, null);
	}

	@Override
	protected void scheduleGlobalSyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
		plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
			if (!task.isCancelled()) {
				runnable.run();
			}
		});
	}

	@Override
	protected void scheduleAsyncTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
		plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
			if (!task.isCancelled()) {
				runnable.run();
			}
		});
	}

	@Override
	protected void scheduleAsyncDelayedTask(@NotNull Plugin plugin, @NotNull Runnable runnable, long delaySeconds) {
		plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> {
			if (!task.isCancelled()) {
				runnable.run();
			}
		}, delaySeconds, TimeUnit.SECONDS);
	}

	@Override
	protected CancellableTask scheduleAsyncRepeatingTask(@NotNull Plugin plugin, @NotNull Runnable runnable,
	                                                     long initialDelaySeconds, long intervalSeconds) {
		ScheduledTask scheduledTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> {
			if (!task.isCancelled()) {
				runnable.run();
			}
		}, initialDelaySeconds, intervalSeconds, TimeUnit.SECONDS);
		return scheduledTask::cancel;
	}
}
