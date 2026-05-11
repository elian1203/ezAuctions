package me.elian.ezauctions.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
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
		if (!componentString.startsWith("[") || !componentString.endsWith("]")) {
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

		int separator = findTopLevel(component, '=');
		if (separator <= 0) {
			return;
		}

		String key = normalizeKey(component.substring(0, separator).trim());
		String value = component.substring(separator + 1).trim();
		if (!Key.parseable(key) || value.isEmpty()) {
			return;
		}

		try {
			BinaryTag tag = TagStringIO.tagStringIO().asTag(value);
			components.put(Key.key(key), GsonDataComponentValue.gsonDataComponentValue(toJson(tag, "")));
		} catch (Exception ignored) {
		}
	}

	// Convert binary tags to JSON.
	private static JsonElement toJson(BinaryTag tag, String parentKey) {
		if (tag instanceof CompoundBinaryTag compound) {
			JsonObject json = new JsonObject();
			for (String key : compound.keySet()) {
				json.add(key, toJson(compound.get(key), key));
			}
			return json;
		}

		if (tag instanceof ListBinaryTag list) {
			JsonArray json = new JsonArray();
			list.stream().map(entry -> toJson(entry, parentKey)).forEach(json::add);
			return json;
		}

		if (tag instanceof ByteBinaryTag byteTag) {
			byte value = byteTag.value();
			if (TEXT_STYLE_BOOLEAN_KEYS.contains(parentKey) && (value == 0 || value == 1)) {
				return new JsonPrimitive(value == 1);
			}
			return new JsonPrimitive(value);
		}

		if (tag instanceof ShortBinaryTag shortTag) {
			return new JsonPrimitive(shortTag.value());
		}

		if (tag instanceof IntBinaryTag intTag) {
			return new JsonPrimitive(intTag.value());
		}

		if (tag instanceof LongBinaryTag longTag) {
			return new JsonPrimitive(longTag.value());
		}

		if (tag instanceof FloatBinaryTag floatTag) {
			return new JsonPrimitive(floatTag.value());
		}

		if (tag instanceof DoubleBinaryTag doubleTag) {
			return new JsonPrimitive(doubleTag.value());
		}

		if (tag instanceof StringBinaryTag stringTag) {
			return new JsonPrimitive(stringTag.value());
		}

		if (tag instanceof ByteArrayBinaryTag byteArrayTag) {
			JsonArray json = new JsonArray();
			for (byte value : byteArrayTag.value()) {
				json.add(value);
			}
			return json;
		}

		if (tag instanceof IntArrayBinaryTag intArrayTag) {
			JsonArray json = new JsonArray();
			for (int value : intArrayTag.value()) {
				json.add(value);
			}
			return json;
		}

		if (tag instanceof LongArrayBinaryTag longArrayTag) {
			JsonArray json = new JsonArray();
			for (long value : longArrayTag.value()) {
				json.add(value);
			}
			return json;
		}

		return JsonNull.INSTANCE;
	}

	// Data component keys without a namespace are vanilla Minecraft keys.
	private static String normalizeKey(String key) {
		return key.indexOf(':') == -1 ? "minecraft:" + key : key;
	}

	// Split separators only when they are outside nested structures and quoted
	// text.
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

	// Find separators only when they are outside nested structures and quoted text.
	private static int findTopLevel(String value, char target) {
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
			} else if (c == target && depth == 0) {
				return i;
			}
		}

		return -1;
	}
}
