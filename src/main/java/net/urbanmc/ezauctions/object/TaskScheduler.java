package net.urbanmc.ezauctions.object;

import net.urbanmc.ezauctions.EzAuctions;
import org.bukkit.Bukkit;

// A simple scheduler wrapper that keeps track of when the plugin is shutting down
// to avoid scheduling tasks on shutdown.
// Without this wrapper, scheduling tasks on shutdown will throw errors.
public class TaskScheduler {

    private final EzAuctions plugin;
    private boolean shuttingDown = false;

    public TaskScheduler(EzAuctions plugin) {
        this.plugin = plugin;
    }

    public void markShutdown() {
        this.shuttingDown = true;
    }

    public void runSyncTask(Runnable run) {
        if (shuttingDown) {
            run.run();
        }
        else {
            Bukkit.getScheduler().runTask(plugin, run);
        }
    }

    public void runSyncDelayedTask(Runnable run, long delay) {
        if (shuttingDown) {
            run.run();
        }
        else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, run, delay);
        }
    }

    public void runAsyncTask(Runnable run) {
        if (shuttingDown) {
            run.run();
        }
        else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, run);
        }
    }

}
