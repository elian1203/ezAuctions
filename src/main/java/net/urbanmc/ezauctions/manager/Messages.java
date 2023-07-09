package net.urbanmc.ezauctions.manager;

import net.md_5.bungee.api.ChatColor;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.util.ReflectionUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Messages {

    private static final Messages instance = new Messages();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    private final File FILE = new File(EzAuctions.getDataDirectory(), "messages.properties");

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

                EzAuctions.copy(input, output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadBundle() {
        try {
            InputStream input = new FileInputStream(FILE);
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);

            bundle = new PropertyResourceBundle(reader);

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getStringFromBundle(String key, Object... args) {
        try {
            return format(bundle.getString(key), true, args);
        } catch (Exception e) {
            if (e instanceof MissingResourceException) {
                EzAuctions.getPluginLogger().warning("Missing message in message.properties! Message key: " + key
                        + " Attempting to add automatically...");
                addDefaultKeyToFile(key);
                EzAuctions.getPluginLogger().info("Successfully added key " + key);
                return format(bundle.getString(key), true, args);
            }
            else {
                EzAuctions.getPluginLogger().log(Level.SEVERE, "Error fetching key '" + key + "' in message.properties!", e);
            }
            return key;
        }
    }

    public String getStringWithoutColoring(String key, Object... args) {
        try {
            return format(bundle.getString(key), false, args);
        } catch (Exception e) {
            if (e instanceof MissingResourceException) {
                EzAuctions.getPluginLogger().warning("Missing message in message.properties! Message key: " + key
                + " Attempting to add automatically...");
                addDefaultKeyToFile(key);
                EzAuctions.getPluginLogger().info("Successfully added key " + key);
                return format(bundle.getString(key), false, args);
            }
            else {
                EzAuctions.getPluginLogger().log(Level.SEVERE, "Error fetching key '" + key + "' in message.properties!", e);
            }
            return key;
        }
    }

    private String format(String message, boolean color, Object... args) {
        message = message.replace("{prefix}", bundle.getString("prefix"));

        if (args != null) {
            message = MessageFormat.format(message, args);
        }

        if (color) {
            message = translateAllColors(message);
        }

        return message;
    }

    private void addDefaultKeyToFile(String key) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("messages.properties")) {
            // open default message file to get default value
            ResourceBundle defaultBundle = new PropertyResourceBundle(input);

            String value = defaultBundle.getString(key);

            // open printwriter in append mode to add to end of file
            PrintWriter writer = new PrintWriter(new FileOutputStream(FILE, true));

            // ensure we are printing on the next line
            writer.write("\n");

            String line = String.format("%s=%s\n", key, value);

            writer.write(line);
            writer.flush();
            writer.close();

            loadBundle();
        } catch (IOException e) {
            EzAuctions.getPluginLogger().severe("Error updating messages.properties with missing key!");
            e.printStackTrace();
        }
    }

    public static String translateAllColors(String message) {
        String hexReplaced = replaceHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', hexReplaced);
    }

    private static String replaceHexColors(String message) {
        // if not supported, return now
        if (ReflectionUtil.getVersion() < 1.16)
            return message;

        Matcher matcher = HEX_PATTERN.matcher(message);
        String replaced = message;

        while (matcher.find()) {
            // this will look like $#FF0000
            String pattern = message.substring(matcher.start(), matcher.end());
            // this will look like #FF00000
            String color = pattern.substring(1);
            // replace the pattern with the color's string value
            replaced = replaced.replace(pattern, ChatColor.of(color).toString());
        }

        return replaced;
    }

    public void reload() {
        createFile();
        loadBundle();
    }
}
