package net.urbanmc.ezauctions.manager;

import org.apache.commons.io.IOUtils;
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
			bundle = new PropertyResourceBundle(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getStringFromBundle(String key, Object... args) {
		return format(bundle.getString(key), true, args);
	}

	public String getStringWithoutColoring(String key, Object... args) {
		return format(bundle.getString(key), false, args);
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
