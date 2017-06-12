package net.urbanmc.ezauctions.manager;

import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Messages {

    private static Messages instance = new Messages();

    private ResourceBundle bundle;

    public static Messages getInstance() {
        return instance;
    }

    private Messages() {
        loadBundle();
    }

    private void loadBundle() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public String getString(String key, Object... args) {
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
