package net.urbanmc.ezauctions.manager;

import org.bukkit.ChatColor;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Messages {

	private static Messages instance = new Messages();

	private ResourceBundle bundle;

	private Messages() {
		loadBundle();
	}

	public static String getString(String key, Object... args) {
		return instance.getStringFromBundle(key, args);
	}

	private void loadBundle() {
		bundle = ResourceBundle.getBundle("messages");
	}

	private String getStringFromBundle(String key, Object... args) {
		return format(bundle.getString(key), args);
	}

	private String format(String message, Object... args) {
		message = message.replace("{prefix}", bundle.getString("prefix"));

		if (args != null) {
			message = MessageFormat.format(message, args);
		}

		message = ChatColor.translateAlternateColorCodes('&', message);

		return message;
	}

}
