package net.urbanmc.ezauctions.util;

import net.urbanmc.ezauctions.object.Auction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUtil {

    public static void removeItemsFromInv(Auction auc, Player p) {
        int amount = auc.getAmount();
        ItemStack auctionItem = auc.getItem();

        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack is = p.getInventory().getItem(i);

            if (is == null)
                continue;

            if (!auctionItem.isSimilar(is))
                continue;

            if (is.getAmount() > amount) {
                is.setAmount(is.getAmount() - amount);
                p.getInventory().setItem(i, is);

                break;
            }

            amount -= is.getAmount();
            p.getInventory().setItem(i, null);

            if (amount == 0)
                break;
        }
    }

    static Material getMaterial(String type) {
        return Material.getMaterial(type.toUpperCase());
    }

    /**
     * @return true if there is overflow, false if not
     */
    static boolean addItemToInventory(Player p, ItemStack is, int amount, boolean message) {
        List<ItemStack> items = new ArrayList<>();

        int maxStackSize = is.getMaxStackSize();

        while (amount > maxStackSize) {
            ItemStack cloned = is.clone();
            cloned.setAmount(maxStackSize);

            items.add(cloned);
            amount -= maxStackSize;
        }

        if (amount != 0) {
            ItemStack cloned = is.clone();
            cloned.setAmount(amount);

            items.add(cloned);
        }

        ItemStack[] array = new ItemStack[items.size()];
        array = items.toArray(array);

        Map<Integer, ItemStack> leftover = p.getInventory().addItem(array);

        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));

            if (message) {
                MessageUtil.privateMessage(p, "reward.full_inventory");
            }
        }

        return !leftover.isEmpty();
    }

    /**
     * Taken from https://gist.github.com/graywolf336/8153678#file-bukkitserialization-java
     */
    public static String serialize(ItemStack... items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Taken from https://gist.github.com/graywolf336/8153678#file-bukkitserialization-java
     */
    public static ItemStack deserialize(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items[0];
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
