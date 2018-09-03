package net.urbanmc.ezauctions.gson;

import com.google.gson.*;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

public class AuctionsPlayerSerializer implements JsonSerializer<AuctionsPlayer>, JsonDeserializer<AuctionsPlayer> {

    @Override
    public JsonElement serialize(AuctionsPlayer player, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("id", player.getUniqueId().toString());
        object.addProperty("ignoringSpammy", player.isIgnoringSpammy());
        object.addProperty("ignoringAll", player.isIgnoringAll());

        JsonArray ignoringPlayersArray = new JsonArray();

        for (UUID id : player.getIgnoringPlayers()) {
            ignoringPlayersArray.add(id.toString());
        }

        object.add("ignoringPlayers", ignoringPlayersArray);

        Gson gson = new Gson();

        JsonArray offlineItemsArray = new JsonArray();

        for (ItemStack is : player.getOfflineItems()) {
            Map<String, Object> map;

            if (is.getType() == Material.ENCHANTED_BOOK) {
                map = new HashMap<>();

                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) is.getItemMeta();

                Map<String, Integer> enchants = new HashMap<>();

                for (Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
                    enchants.put(entry.getKey().getName(), entry.getValue());
                }

                map.put("type", "ENCHANTED_BOOK");
                map.put("enchants", enchants);
            } else {
                map = is.serialize();
            }

            JsonElement je = gson.toJsonTree(map);
            offlineItemsArray.add(je);
        }

        object.add("offlineItems", offlineItemsArray);

        return object;
    }

    @Override
    public AuctionsPlayer deserialize(JsonElement element, Type type,
                                      JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = (JsonObject) element;

        UUID id = UUID.fromString(object.get("id").getAsString());

        boolean ignoringSpammy = object.get("ignoringSpammy").getAsBoolean(), ignoringAll =
                object.get("ignoringAll").getAsBoolean();

        List<UUID> ignoringPlayers = new ArrayList<>();

        if (object.has("ignoringPlayers")) {
            JsonArray ignoringPlayersArray = object.getAsJsonArray("ignoringPlayers");

            for (JsonElement je : ignoringPlayersArray) {
                ignoringPlayers.add(UUID.fromString(je.getAsString()));
            }
        }

        Gson gson = new Gson();

        JsonArray array = object.getAsJsonArray("offlineItems");
        List<ItemStack> offlineItems = new ArrayList<>();


        for (JsonElement je : array) {
            ItemStack is;

            JsonObject obj = je.getAsJsonObject();

            if (obj.get("type").getAsString().equals("ENCHANTED_BOOK")) {
                Map<Object, Double> map = gson.fromJson(obj.get("enchants"), Map.class);

                is = new ItemStack(Material.ENCHANTED_BOOK);

                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) is.getItemMeta();

                for (Entry<Object, Double> entry : map.entrySet()) {
                    Enchantment enchant = Enchantment.getByName(entry.getKey().toString());

                    meta.addStoredEnchant(enchant, entry.getValue().intValue(), true);
                }

                is.setItemMeta(meta);
            } else {
                Map<String, Object> map = gson.fromJson(je, Map.class);
                is = ItemStack.deserialize(map);
            }

            offlineItems.add(is);
        }

        return new AuctionsPlayer(id, ignoringSpammy, ignoringAll, ignoringPlayers, offlineItems);
    }
}
