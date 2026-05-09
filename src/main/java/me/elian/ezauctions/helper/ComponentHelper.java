package me.elian.ezauctions.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// Helper to parse Spigot Item Meta
// component string into adventure compatible
// hover components.
// Supports Spigot 1.21+
public final class ComponentHelper {
	private ComponentHelper() {
	}

	public static @NotNull Map<Key, DataComponentValue> getComponentsFromMeta(@NotNull ItemStack itemStack) {
		if (!itemStack.hasItemMeta()) {
			return Map.of();
		}

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			return Map.of();
		}

		String componentString = meta.getAsComponentString();
		// Ex. [minecraft:custom_data={foo: [1, 2, 3]}, minecraft:damage=5]
		if (!componentString.startsWith("[") || !componentString.endsWith("]") || !componentString.contains("=")) {
			return Map.of();
		}

		Map<Key, DataComponentValue> components = new LinkedHashMap<>();
		String content = componentString.substring(1, componentString.length() - 1);
		for (String rawComponent : splitTopLevel(content, ',')) {
			addComponent(components, rawComponent);
		}

		return components;
	}

	private static void addComponent(Map<Key, DataComponentValue> components, String rawComponent) {
		String component = rawComponent.trim();
		if (component.isEmpty()) {
			return;
		}

		if (component.charAt(0) == '!') {
			String key = normalizeKey(component.substring(1).trim());
			if (Key.parseable(key)) {
				components.put(Key.key(key), DataComponentValue.removed());
			}
			return;
		}

		int separator = component.indexOf('=');
		if (separator <= 0) {
			return;
		}

		String key = normalizeKey(component.substring(0, separator).trim());
		String value = component.substring(separator + 1).trim();
		if (!Key.parseable(key) || value.isEmpty()) {
			return;
		}

		try {
			JsonElement json = JsonParser.parseString(toJson(value));
			components.put(Key.key(key), GsonDataComponentValue.gsonDataComponentValue(json));
		} catch (Exception ignored) {
		}
	}

	// Data component keys without a namespace are vanilla Minecraft keys.
	private static String normalizeKey(String key) {
		return key.indexOf(':') == -1 ? "minecraft:" + key : key;
	}

	// Split separators only when they are outside nested structures and quoted text.
	private static ArrayList<String> splitTopLevel(String value, char separator) {
		ArrayList<String> parts = new ArrayList<>();
		int start = 0;
		int depth = 0;
		char quote = 0;
		boolean escaped = false;

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (quote != 0) {
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}

			if (c == '"' || c == '\'') {
				quote = c;
			} else if (c == '[' || c == '{' || c == '(') {
				depth++;
			} else if (c == ']' || c == '}' || c == ')') {
				depth--;
			} else if (c == separator && depth == 0) {
				parts.add(value.substring(start, i));
				start = i + 1;
			}
		}

		parts.add(value.substring(start));
		return parts;
	}

	// Find a character only when it appears outside nested structures and quoted text.
	private static int findTopLevel(String value, char searched) {
		int depth = 0;
		char quote = 0;
		boolean escaped = false;

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (quote != 0) {
				if (escaped) {
					escaped = false;
				} else if (c == '\\') {
					escaped = true;
				} else if (c == quote) {
					quote = 0;
				}
				continue;
			}

			if (c == '"' || c == '\'') {
				quote = c;
			} else if (c == '[' || c == '{' || c == '(') {
				depth++;
			} else if (c == ']' || c == '}' || c == ')') {
				depth--;
			} else if (c == searched && depth == 0) {
				return i;
			}
		}

		return -1;
	}

	// Convert Spigot's component-like value syntax into valid JSON for Gson.
	private static String toJson(String value) {
		StringBuilder json = new StringBuilder(value.length() + 16);
		char quote = 0;
		boolean escaped = false;

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (quote != 0) {
				if (escaped) {
					json.append(c);
					escaped = false;
				} else if (c == '\\') {
					json.append(c);
					escaped = true;
				} else if (c == quote) {
					json.append('"');
					quote = 0;
				} else if (quote == '\'' && c == '"') {
					json.append("\\\"");
				} else {
					json.append(c);
				}
				continue;
			}

			if (c == '"' || c == '\'') {
				json.append('"');
				quote = c;
			} else if (isIdentifierStart(c)) {
				int end = i + 1;
				while (end < value.length() && isIdentifierPart(value.charAt(end))) {
					end++;
				}

				String token = value.substring(i, end);
				int next = nextNonWhitespace(value, end);
				if (next < value.length() && value.charAt(next) == ':'
						&& previousNonWhitespaceIs(value, i, '{', ',')) {
					json.append('"').append(token).append('"');
				} else if (isBareStringValue(value, i, end, token)) {
					json.append('"').append(token).append('"');
				} else {
					json.append(token);
				}
				i = end - 1;
			} else {
				json.append(c);
			}
		}

		return json.toString();
	}

	// Detect unquoted scalar strings so they can be quoted before JSON parsing.
	private static boolean isBareStringValue(String value, int start, int end, String token) {
		if ("true".equals(token) || "false".equals(token) || "null".equals(token)) {
			return false;
		}

		if (!previousNonWhitespaceIs(value, start, ':', '[', ',', '=')) {
			return false;
		}

		int next = nextNonWhitespace(value, end);
		return next == value.length() || value.charAt(next) == ',' || value.charAt(next) == ']'
				|| value.charAt(next) == '}';
	}

	// Check the nearest previous non-space character against a small expected set.
	private static boolean previousNonWhitespaceIs(String value, int index, char... expected) {
		for (int i = index - 1; i >= 0; i--) {
			char c = value.charAt(i);
			if (!Character.isWhitespace(c)) {
				for (char candidate : expected) {
					if (c == candidate) {
						return true;
					}
				}
				return false;
			}
		}

		return false;
	}

	// Return the next non-space character index, or the string length if none exists.
	private static int nextNonWhitespace(String value, int index) {
		for (int i = index; i < value.length(); i++) {
			if (!Character.isWhitespace(value.charAt(i))) {
				return i;
			}
		}

		return value.length();
	}

	private static boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '-' || c == ':';
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
	}
}
