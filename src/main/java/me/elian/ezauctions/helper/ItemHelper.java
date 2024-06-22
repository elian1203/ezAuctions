package me.elian.ezauctions.helper;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class ItemHelper {
	// Memoize reflection operations
	// Valid because both methods are Server API so
	// the values will not change during runtime.
	private static Optional<Boolean> hasAsHoverEvent = Optional.empty();
	private static Optional<Boolean> hasItemMetaAsString = Optional.empty();

	public static byte[] serialize(@NotNull ItemStack item) throws IllegalStateException {
		try (var outputStream = new ByteArrayOutputStream();
		     var dataOutput = new BukkitObjectOutputStream(outputStream)) {
			dataOutput.writeObject(item);
			dataOutput.flush();
			return Base64.getEncoder().encode(outputStream.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}

	public static @NotNull ItemStack deserialize(byte[] data) throws IOException {
		try (var inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
		     var dataInput = new BukkitObjectInputStream(inputStream)) {
			return (ItemStack) dataInput.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to decode class type.", e);
		}
	}

	public static boolean addItemToPlayerInventory(@NotNull Player player, @NotNull ItemStack itemStack, int amount) {
		ArrayList<ItemStack> items = new ArrayList<>();
		int maxStackSize = itemStack.getMaxStackSize();
		while (amount > maxStackSize) {
			ItemStack clone = itemStack.clone();
			clone.setAmount(maxStackSize);
			items.add(clone);
			amount -= maxStackSize;
		}

		if (amount != 0) {
			ItemStack clone = itemStack.clone();
			clone.setAmount(amount);
			items.add(clone);
		}

		ItemStack[] array = new ItemStack[items.size()];
		array = items.toArray(array);

		HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(array);

		if (leftover.isEmpty())
			return false;

		Location location = player.getLocation();
		World world = player.getWorld();
		for (ItemStack item : leftover.values()) {
			world.dropItem(location, item);
		}

		return true;
	}

	public static void removeItemFromPlayerInventory(@NotNull Player player, @NotNull ItemStack itemStack,
	                                                 int amount) {
		int remainingAmount = amount;
		PlayerInventory inventory = player.getInventory();

		ItemStack mainHand = inventory.getItemInMainHand();

		if (mainHand.isSimilar(itemStack)) {
			int itemAmount = mainHand.getAmount();
			if (itemAmount <= remainingAmount) {
				inventory.setItemInMainHand(null);
				remainingAmount -= itemAmount;
			} else {
				mainHand.setAmount(itemAmount - remainingAmount);
				inventory.setItemInMainHand(mainHand);
				return;
			}
		}

		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack is = inventory.getItem(i);

			if (is == null || !itemStack.isSimilar(is))
				continue;

			if (is.getAmount() > remainingAmount) {
				is.setAmount(is.getAmount() - remainingAmount);
				inventory.setItem(i, is);

				break;
			}

			remainingAmount -= is.getAmount();
			inventory.setItem(i, null);

			if (remainingAmount == 0)
				break;
		}
	}

	public static int getAmountOfItemInInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
		int amountInInventory = 0;

		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack is = inventory.getItem(i);

			if (is == null || !itemStack.isSimilar(is))
				continue;

			amountInInventory += is.getAmount();
		}

		return amountInInventory;
	}

	private static boolean hasAsHoverEventMethod() {
		if (hasAsHoverEvent.isPresent()) {
			return hasAsHoverEvent.get();
		}

		Boolean methodExists = Boolean.FALSE;
		try {
			// Check if ItemStack has asHoverEvent method exists
			ItemStack.class.getMethod("asHoverEvent", UnaryOperator.class);
			methodExists = Boolean.TRUE;
		} catch (NoSuchMethodException ignored) {
		}
		hasAsHoverEvent = Optional.of(methodExists);
		return methodExists;
	}

	@Nullable
	public static HoverEvent<HoverEvent.ShowItem> getItemHover(@NotNull ItemStack itemStack,
	                                                           UnaryOperator<HoverEvent.ShowItem> transform) {
		// Supported by PaperMC (and forks) servers
		if (hasAsHoverEventMethod()) {
			return itemStack.asHoverEvent(transform);
		}

		// Try to get NBT
		String itemNBT = null;
		try {
			itemNBT = getItemNBT(itemStack).replace("minecraft:", "");
		} catch (Exception e) {
		}

		if (itemNBT == null) {
			return null;
		}

		NamespacedKey typeKey = itemStack.getType().getKey();
		Key itemKey = Key.key(typeKey.getNamespace(), typeKey.getKey());
		return HoverEvent.showItem(itemKey, itemStack.getAmount(),
						BinaryTagHolder.binaryTagHolder(itemNBT))
				.asHoverEvent(transform);
	}

	private static boolean hasItemMetaGetAsStringMethod() {
		if (hasItemMetaAsString.isPresent()) {
			return hasItemMetaAsString.get();
		}

		Boolean methodExists = Boolean.FALSE;
		try {
			// Check if ItemStack has asHoverEvent method exists
			ItemMeta.class.getMethod("getAsString");
			methodExists = Boolean.TRUE;
		} catch (NoSuchMethodException ignored) {
		}
		hasItemMetaAsString = Optional.of(methodExists);
		return methodExists;
	}

	public static @NotNull String getItemNBT(@NotNull ItemStack itemStack)
			throws Exception {
		// Supported by Spigot 1.18+
		if (hasItemMetaGetAsStringMethod()) {
			return itemStack.hasItemMeta() ? itemStack.getItemMeta().getAsString() : "{}";
		}

		return getItemNbtNms(itemStack);
	}

	private static @NotNull String getItemNbtNms(@NotNull ItemStack itemStack) throws NoSuchMethodException,
			InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
		Class<? extends ItemStack> itemStackClass = itemStack.getClass();
		Class<?> nbtTagCompoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");

		// get the nms copy
		Object nmsStack = itemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, itemStack);

		// find the save method from the nms stack
		Method getNbtMethod = null;
		for (Method method : nmsStack.getClass().getMethods()) {
			if (method.getReturnType().equals(nbtTagCompoundClass) && method.getParameterCount() == 0) {
				getNbtMethod = method;
				break;
			}
		}

		if (getNbtMethod == null)
			throw new NoSuchMethodException("Could not save item to nbt! getNbtMethod not found!");

		// get item tag
		Object tag = getNbtMethod.invoke(nmsStack);
		// if no tag, create empty tag
		if (tag == null) {
			tag = nbtTagCompoundClass.getConstructor().newInstance();
		}
		// get string form of nbttagcompound
		return tag.toString();
	}

	public static @NotNull String getMinecraftName(ItemStack is) {
		Material material = is.getType();
		return (material.isBlock() ? "block" : "item") + ".minecraft." + material.toString().toLowerCase();
	}

	/**
	 * @return 0 if the item has never been repaired or -1 if it is no longer repairable.
	 */
	public static int getXPForRepair(ItemStack is) {
		int cost = (int) is.getItemMeta().serialize().getOrDefault("repair-cost", 0);
		boolean repairable = cost <= 40;
		return repairable ? cost : -1;
	}
}
