package me.elian.ezauctions.controller;

import me.elian.ezauctions.PluginLogger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class FileHandler {
	private final PluginLogger logger;
	private final String resourceName;
	private final Path path;

	protected FileHandler(Plugin plugin, PluginLogger logger, String resourceName) {
		this.logger = logger;
		this.resourceName = resourceName;

		this.path = Path.of(plugin.getDataFolder().getPath(), resourceName);
	}

	private void createFileFromResource() throws IOException, SecurityException {
		if (path.toFile().exists())
			return;

		try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			if (input == null)
				throw new IOException("Invalid resource name!");

			File parent = path.toFile().getParentFile();
			if (!parent.isDirectory() && !parent.mkdirs())
				throw new IOException("Could not create parent directory!");

			Files.copy(input, path);
		}
	}

	protected abstract void loadFile(@NotNull Reader reader);

	public void reload() throws IOException {
		try {
			createFileFromResource();
			File file = path.toFile();
			try (FileInputStream inputStream = new FileInputStream(file); Reader reader =
					new InputStreamReader(inputStream)) {
				loadFile(reader);
			}
		} catch (IOException | SecurityException e) {
			logger.severe("Could not create file " + resourceName + "! " +
					"Check disk space and directory permissions! Using default configuration!", e);
			loadDefaultFile();
			throw new IOException();
		}
	}

	private void loadDefaultFile() {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			if (inputStream == null)
				throw new IOException();

			try (Reader reader = new InputStreamReader(inputStream)) {
				loadFile(reader);
			}
		} catch (IOException e) {
			logger.severe("Could not load resource " + resourceName + "!!! The plugin will not work correctly.", e);
		}
	}
}
