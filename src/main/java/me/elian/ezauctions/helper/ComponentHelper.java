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
import java.util.Set;

// Helper to parse Spigot Item Meta
// component string into adventure compatible
// hover components.
// Supports Spigot 1.21+
public final class ComponentHelper {
	private static final Set<String> TEXT_STYLE_BOOLEAN_KEYS = Set.of("bold", "italic", "underlined", "strikethrough",
			"obfuscated");

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

		return getComponentsFromString(meta.getAsComponentString());
	}

	public static @NotNull Map<Key, DataComponentValue> getComponentsFromString(@NotNull String componentString) {
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

	// Convert Spigot's component-like value syntax into valid JSON for Gson.
	private static String toJson(String value) {
		StringBuilder json = new StringBuilder(value.length() + 16);
		QuoteContext quoteContext = new QuoteContext();

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (quoteContext.isInsideQuote()) {
				appendQuotedCharacter(json, quoteContext, c);
				continue;
			}

			if (isQuote(c)) {
				json.append('"');
				quoteContext.start(c);
			} else if (isNumberStart(c, value, i)) {
				i = appendNumberToken(json, value, i);
			} else if (isIdentifierStart(c)) {
				i = appendIdentifierToken(json, value, i);
			} else {
				json.append(c);
			}
		}

		return json.toString();
	}

	// Preserve quoted content while normalizing quote delimiters to JSON quotes.
	private static void appendQuotedCharacter(StringBuilder json, QuoteContext quoteContext, char c) {
		if (quoteContext.isEscaped()) {
			json.append(c);
			quoteContext.clearEscape();
		} else if (c == '\\') {
			json.append(c);
			quoteContext.markEscaped();
		} else if (c == quoteContext.getQuote()) {
			json.append('"');
			quoteContext.end();
		} else if (quoteContext.getQuote() == '\'' && c == '"') {
			json.append("\\\"");
		} else {
			json.append(c);
		}
	}

	// Append one identifier, quoting it if it is an object key or bare string value.
	private static int appendIdentifierToken(StringBuilder json, String value, int start) {
		int end = start + 1;
		while (end < value.length() && isIdentifierPart(value.charAt(end))) {
			end++;
		}

		String token = value.substring(start, end);
		if (isObjectKey(value, start, end) || isBareStringValue(value, start, end, token)) {
			json.append('"').append(token).append('"');
		} else {
			json.append(token);
		}

		return end - 1;
	}

	// Convert SNBT-style numeric suffixes into JSON-compatible numbers or text booleans.
	private static int appendNumberToken(StringBuilder json, String value, int start) {
		int end = start;
		if (value.charAt(end) == '-') {
			end++;
		}

		while (end < value.length() && Character.isDigit(value.charAt(end))) {
			end++;
		}

		if (end < value.length() && value.charAt(end) == '.') {
			end++;
			while (end < value.length() && Character.isDigit(value.charAt(end))) {
				end++;
			}
		}

		if (end < value.length() && isNumericSuffix(value.charAt(end))) {
			char suffix = Character.toLowerCase(value.charAt(end));
			String number = value.substring(start, end);
			if (suffix == 'b' && ("0".equals(number) || "1".equals(number))
					&& TEXT_STYLE_BOOLEAN_KEYS.contains(previousObjectKey(value, start))) {
				json.append("1".equals(number) ? "true" : "false");
			} else {
				json.append(number);
			}
			return end;
		}

		json.append(value, start, end);
		return end - 1;
	}

	// Detect unquoted object keys so they can be quoted before JSON parsing.
	private static boolean isObjectKey(String value, int start, int end) {
		int next = nextNonWhitespace(value, end);
		return next < value.length() && value.charAt(next) == ':'
				&& previousNonWhitespaceIs(value, start, '{', ',');
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
		int previous = previousNonWhitespace(value, index);
		if (previous < 0) {
			return false;
		}

		char c = value.charAt(previous);
		for (char candidate : expected) {
			if (c == candidate) {
				return true;
			}
		}

		return false;
	}

	private static String previousObjectKey(String value, int index) {
		int colon = previousNonWhitespace(value, index);
		if (colon < 0 || value.charAt(colon) != ':') {
			return "";
		}

		int end = previousNonWhitespace(value, colon);
		if (end < 0) {
			return "";
		}

		char quote = value.charAt(end);
		if (quote == '"' || quote == '\'') {
			for (int start = end - 1; start >= 0; start--) {
				if (value.charAt(start) == quote && !isEscaped(value, start)) {
					return value.substring(start + 1, end);
				}
			}
			return "";
		}

		int start = end;
		while (start >= 0 && isIdentifierPart(value.charAt(start))) {
			start--;
		}
		return value.substring(start + 1, end + 1);
	}

	private static int previousNonWhitespace(String value, int index) {
		for (int i = index - 1; i >= 0; i--) {
			if (!Character.isWhitespace(value.charAt(i))) {
				return i;
			}
		}

		return -1;
	}

	private static boolean isEscaped(String value, int index) {
		int slashCount = 0;
		for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
			slashCount++;
		}

		return slashCount % 2 == 1;
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

	// Component values can use either single or double quotes.
	private static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	private static boolean isNumberStart(char c, String value, int index) {
		if (Character.isDigit(c)) {
			return true;
		}

		return c == '-' && index + 1 < value.length() && Character.isDigit(value.charAt(index + 1));
	}

	private static boolean isNumericSuffix(char c) {
		switch (Character.toLowerCase(c)) {
			case 'b':
			case 's':
			case 'l':
			case 'f':
			case 'd':
				return true;
			default:
				return false;
		}
	}

	// Identifier starts include namespaced keys and unquoted string values.
	private static boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '-' || c == ':';
	}

	// Identifier parts allow common Minecraft key characters.
	private static boolean isIdentifierPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
	}

	// Tracks the current quoted section while converting component values.
	private static final class QuoteContext {
		private char quote;
		private boolean escaped;

		private boolean isInsideQuote() {
			return quote != 0;
		}

		private void start(char quote) {
			this.quote = quote;
		}

		private void end() {
			quote = 0;
		}

		private char getQuote() {
			return quote;
		}

		private boolean isEscaped() {
			return escaped;
		}

		private void markEscaped() {
			escaped = true;
		}

		private void clearEscape() {
			escaped = false;
		}
	}
}
