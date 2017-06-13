package net.urbanmc.ezauctions.manager;

import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class ConfigManager {

	private static ConfigManager instance = new ConfigManager();

	private final File FILE = new File("plugins/ezAuctions", "config.yml");

	private FileConfiguration data;

	private ConfigManager() {
		createFile();
		loadConfiguration();
	}

	public static ConfigManager getInstance() {
		return instance;
	}

	public static FileConfiguration getConfig() {
		return instance.getData();
	}

	private void createFile() {
		if (!FILE.getParentFile().isDirectory()) {
			FILE.getParentFile().mkdir();
		}

		if (!FILE.exists()) {
			try {
				FILE.createNewFile();

				InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml");
				OutputStream output = new FileOutputStream(FILE);

				IOUtils.copy(input, output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadConfiguration() {
		data = YamlConfiguration.loadConfiguration(FILE);
	}

	private FileConfiguration getData() {
		return data;
	}

	public void reloadConfiguration() {
		createFile();
		loadConfiguration();
	}

	public Object get(String path) {
		return data.get(path);
	}
}
