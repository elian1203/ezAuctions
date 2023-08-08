package me.elian.ezauctions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

@Singleton
public class PluginLogger implements Logger {
	private final Plugin plugin;

	@Inject
	public PluginLogger(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void info(String message) {
		plugin.getLogger().info(message);
	}

	@Override
	public void warning(String message, Exception exception) {
		plugin.getLogger().log(Level.WARNING, message, exception);
	}

	@Override
	public void severe(String message) {
		plugin.getLogger().severe(message);
	}

	@Override
	public void severe(String message, Exception exception) {
		plugin.getLogger().log(Level.SEVERE, message, exception);
	}
}
