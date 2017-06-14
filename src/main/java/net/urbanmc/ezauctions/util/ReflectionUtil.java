package net.urbanmc.ezauctions.util;

import mkremins.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;

@SuppressWarnings("ConstantConditions")
public class ReflectionUtil {

	public static String getFriendlyName(ItemStack is) {
		try {
			Object nmsStack = asNMSCopy(is);

			return nmsStack.getClass().getMethod("getName").invoke(nmsStack).toString();
		} catch (Exception ex) {
			Bukkit.getLogger()
					.log(Level.WARNING, "[ezAuctions] Error getting friendly name for " + is.getType().toString(), ex);
			return "";
		}
	}

	/**
	 * @return 0 if the item has never been repaired or -1 if it is no longer repairable.
	 */
	public static int getXPForRepair(ItemStack is) {
		try {
			Object nmsStack = asNMSCopy(is);

			boolean hasTag = (boolean) nmsStack.getClass().getMethod("hasTag").invoke(nmsStack);

			if (!hasTag)
				return 0;

			Object tag = nmsStack.getClass().getMethod("getTag").invoke(nmsStack);

			boolean hasKey = (boolean) tag.getClass().getMethod("hasKey", String.class).invoke(tag, "RepairCost");

			if (!hasKey)
				return 0;

			int cost = (int) tag.getClass().getMethod("getInt", String.class).invoke(tag, "RepairCost");

			boolean repairable = cost <= 40;

			if (repairable)
				return cost;
			else
				return -1;
		} catch (Exception ex) {
			Bukkit.getLogger().log(Level.WARNING,
			                       "[ezAuctions] Error getting xp needed for repair for " + is.getType().toString(),
			                       ex);
			return 0;
		}
	}

	static int getMaxStackSize(ItemStack is) {
		try {
			Object nmsStack = asNMSCopy(is);

			return (int) nmsStack.getClass().getMethod("getMaxStackSize").invoke(nmsStack);
		} catch (Exception ex) {
			Bukkit.getLogger()
					.log(Level.WARNING, "[ezAuctions] Error getting max stack size for " + is.getType().toString(),
					     ex);
			return 0;
		}
	}

	public static void addItemHover(FancyMessage fancy, ItemStack is) {
		try {
			String json = getItemAsJson(is);

			Class<?> jsonStringClazz = Class.forName("mkremins.fanciful.JsonString");
			Constructor<?> constructor = jsonStringClazz.getConstructor(String.class);

			Object jsonStringInstance = constructor.newInstance(json);

			Method latestMethod = fancy.getClass().getMethod("latest");
			Object latest = latestMethod.invoke(fancy);

			latest.getClass().getDeclaredField("hoverActionName").set(latest, jsonStringInstance);
		} catch (Exception ex) {
			Bukkit.getLogger().log(Level.WARNING,
			                       "[ezAuctions] Error adding hover to item. Item: " + is.getType().toString(),
			                       ex);
		}
	}

	private static String getItemAsJson(ItemStack is) {
		try {
			Object nmsStack = asNMSCopy(is);

			Class<?> nbtTagCompoundClazz = getNMSClass("NBTTagCompound");
			Method saveMethod = nmsStack.getClass().getMethod("save", nbtTagCompoundClazz);

			Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
			Object jsonItem = saveMethod.invoke(nmsStack, nmsNbtTagCompoundObj);

			return jsonItem.toString();
		} catch (Exception ex) {
			Bukkit.getLogger().log(Level.WARNING,
			                       "[ezAuctions] Error getting item as json. Item: " + is.getType().toString(),
			                       ex);
			return "";
		}
	}

	private static Object asNMSCopy(ItemStack is) {
		try {
			return getCraftItemStackClass().getMethod("asNMSCopy", ItemStack.class).invoke(null, is);
		} catch (Exception ex) {
			Bukkit.getLogger().log(Level.WARNING,
			                       "[ezAuctions] Error getting item as NMS copy. Item: " + is.getType().toString(),
			                       ex);
			return null;
		}
	}

	private static Class<?> getCraftItemStackClass() {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

		try {
			return Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static Class<?> getNMSClass(String name) {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

		try {
			return Class.forName("net.minecraft.server." + version + "." + name);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
