package me.elian.ezauctions.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// Helper to parse Spigot Item Meta
// component string into adventure compatible
// hover components.
// Supports Spigot 1.21+
public class ComponentHelper {
	private static Method asNMSCopyMethod;
	private static Object codecFieldInstance;
	private static Object dynamicOps;
	private static Method encodeMethod;
	private static Object cachedEmptyMap;
	private static Method getOrThrowMethod;

	static {
		try {
			String serverPackage = Bukkit.getServer().getClass().getPackage().getName();

			Class<?> craftItemStackClass = Class.forName(serverPackage + ".inventory.CraftItemStack");
			Class<?> craftRegistryClass = Class.forName(serverPackage + ".CraftRegistry");

			asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
			Object nmsCopy = asNMSCopyMethod.invoke(null, new ItemStack(Material.DIAMOND));

			Object minecraftRegistry = craftRegistryClass.getMethod("getMinecraftRegistry").invoke(null);
			Class<?> dynamicOpsClass = Class.forName("com.mojang.serialization.DynamicOps");
			Class<?> jsonOpsClass = Class.forName("com.mojang.serialization.JsonOps");
			Method getContextMethod = null;

			for (Method method : minecraftRegistry.getClass().getMethods()) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1
						&& params[0].isAssignableFrom(jsonOpsClass)
						&& dynamicOpsClass.isAssignableFrom(method.getReturnType())) {
					getContextMethod = method;
					break;
				}
			}

			if (getContextMethod != null) {
				Object jsonOpsInstance = jsonOpsClass.getField("INSTANCE").get(null);
				dynamicOps = getContextMethod.invoke(minecraftRegistry, jsonOpsInstance);

				cachedEmptyMap = dynamicOpsClass.getMethod("emptyMap").invoke(dynamicOps);

				Class<?> codecClass = Class.forName("com.mojang.serialization.Codec");
				encodeMethod = codecClass.getMethod("encode", Object.class, dynamicOpsClass, Object.class);

				for (Field field : nmsCopy.getClass().getFields()) {
					if (!field.getType().equals(codecClass)) {
						continue;
					}

					try {
						codecFieldInstance = field.get(null);
						Object dataResult = encodeMethod.invoke(codecFieldInstance, nmsCopy, dynamicOps,
								cachedEmptyMap);
						getOrThrowMethod = dataResult.getClass().getMethod("getOrThrow");

						break;
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static @NotNull Map<Key, DataComponentValue> getComponentsFromMeta(@NotNull ItemStack itemStack) {
		if (!itemStack.hasItemMeta() || itemStack.getType().isAir()) {
			return Map.of();
		}

		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			return Map.of();
		}

		try {
			JsonObject components = getItemComponentsNms(itemStack);
			return createComponentMapFromJson(components);
		} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException |
		         InstantiationException e) {
			return Map.of();
		}
	}

	private static @NotNull JsonObject getItemComponentsNms(@NotNull ItemStack item) throws NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
		try {
			if (getOrThrowMethod == null) {
				return new JsonObject();
			}

			Object nmsCopy = asNMSCopyMethod.invoke(null, item);
			Object dataResult = encodeMethod.invoke(codecFieldInstance, nmsCopy, dynamicOps, cachedEmptyMap);

			Object rawResult = getOrThrowMethod.invoke(dataResult);

			if (rawResult instanceof JsonObject data && data.has("components")) {
				return data.get("components").getAsJsonObject();
			}
		} catch (Exception ignored) {
		}

		return new JsonObject();
	}

	private static @NotNull Map<Key, DataComponentValue> createComponentMapFromJson(JsonObject components) {
		Map<Key, DataComponentValue> map = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
			Key key = Key.key(normalizeKey(entry.getKey()));
			GsonDataComponentValue value = GsonDataComponentValue.gsonDataComponentValue(entry.getValue());
			map.put(key, value);
		}
		return map;
	}

	// Data component keys without a namespace are vanilla Minecraft keys.
	private static String normalizeKey(String key) {
		return key.indexOf(':') == -1 ? "minecraft:" + key : key;
	}
}
