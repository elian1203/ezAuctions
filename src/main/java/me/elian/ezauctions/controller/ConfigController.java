package me.elian.ezauctions.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.PluginLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;

@Singleton
public class ConfigController extends FileHandler {
	private static final String RESOURCE_NAME = "config.yml";
	private FileConfiguration fileConfiguration;

	@Inject
	public ConfigController(Plugin plugin, PluginLogger logger) {
		super(plugin, logger, RESOURCE_NAME);

		try {
			reload();
		} catch (IOException e) {
			logger.severe("Could not load config file!", e);
		}
	}

	@Override
	protected void loadFile(@NotNull Reader reader) {
		fileConfiguration = YamlConfiguration.loadConfiguration(reader);
	}

	public @NotNull FileConfiguration getConfig() {
		return fileConfiguration;
	}
}
