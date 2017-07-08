package net.urbanmc.ezauctions.manager;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Messages {

	private static Messages instance = new Messages();

	private final File FILE = new File("plugins/ezAuctions", "messages.properties");

	private ResourceBundle bundle;

	private Messages() {
		createFile();
		loadBundle();
	}

	public static Messages getInstance() {
		return instance;
	}

	public static String getString(String key, Object... args) {
		return instance.getStringFromBundle(key, args);
	}

	private void createFile() {
		if (!FILE.getParentFile().isDirectory()) {
			FILE.getParentFile().mkdir();
		}

		if (!FILE.exists()) {
			try {
				FILE.createNewFile();

				InputStream input = getClass().getClassLoader().getResourceAsStream("messages.properties");
				OutputStream output = new FileOutputStream(FILE);

				IOUtils.copy(input, output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadBundle() {
		try {
			InputStream input = new FileInputStream(FILE);
			Reader reader = new InputStreamReader(input, "UTF-8");

			bundle = new PropertyResourceBundle(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getStringFromBundle(String key, Object... args) {
		try {
			return format(bundle.getString(key), true, args);
		} catch (Exception e) {
			Bukkit.getLogger().severe("[ezAuctions] Missing message in message.properties! Message key: " + key);
			return key;
		}
	}

	public String getStringWithoutColoring(String key, Object... args) {
		try {
			return format(bundle.getString(key), false, args);
		} catch (Exception e) {
			Bukkit.getLogger().severe("[ezAuctions] Missing message in message.properties! Message key: " + key);
			return key;
		}
	}

	private String format(String message, boolean color, Object... args) {
		message = message.replace("{prefix}", bundle.getString("prefix"));

		if (args != null) {
			message = MessageFormat.format(message, args);
		}

		if (color) {
			message = ChatColor.translateAlternateColorCodes('&', message);
		}

		return message;
	}

	public void reload() {
		createFile();
		loadBundle();
	}
}
