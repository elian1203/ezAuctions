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
	private final List<Player> executingCommands = new ArrayList<>();
	private final HashMap<Player, List<Runnable>> queuedCommands = new HashMap<>();
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
		Runnable wrapped = wrapRunnable(runnable);

		if (shuttingDown) {
			wrapped.run();
			return;
		}

		scheduleAsyncTask(plugin, () -> {
			try {
				synchronized (executingCommands) {
					if (executingCommands.contains(player)) {
						addPlayerCommandToQueue(player, runnable);
						return;
					}
				}

				wrapped.run();

				synchronized (executingCommands) {
					executingCommands.remove(player);
				}

				processNextCommandInQueueForPlayer(player);
			} catch (InterruptedException e) {
				logger.severe("Thread error occurred when attempting to process command!", e);
				player.sendMessage(Component.text("Errors occurred while processing your command. " +
						"Please contact the server administrator."));
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

	private void addPlayerCommandToQueue(Player player, Runnable runnable) throws InterruptedException {
		synchronized (queuedCommands) {
			List<Runnable> commands = queuedCommands.get(player);
			if (commands == null) {
				commands = new ArrayList<>();
			}

			commands.add(runnable);
			queuedCommands.put(player, commands);
		}
	}

	private void processNextCommandInQueueForPlayer(Player player) {
		synchronized (queuedCommands) {
			List<Runnable> commands = queuedCommands.get(player);

			Runnable nextRun = null;

			if (commands != null) {
				nextRun = commands.remove(0);

				if (commands.size() == 0) {
					queuedCommands.remove(player);
				}
			}

			if (nextRun != null) {
				runAsyncPlayerCommandTask(player, nextRun);
			}
		}
	}

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
