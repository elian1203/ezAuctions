package net.urbanmc.ezauctions.gson;

import com.google.gson.*;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionsPlayerSerializer implements JsonSerializer<AuctionsPlayer>, JsonDeserializer<AuctionsPlayer> {

    public static JsonElement serializeAuctionPlayer(AuctionsPlayer player) {
        JsonObject object = new JsonObject();

        object.addProperty("id", player.getUniqueId().toString());
        object.addProperty("ignoringSpammy", player.isIgnoringSpammy());
        object.addProperty("ignoringAll", player.isIgnoringAll());
        object.addProperty("ignoringScoreboard", player.isIgnoringScoreboard());

        JsonArray ignoringPlayersArray = new JsonArray();

        for (UUID id : player.getIgnoringPlayers()) {
            ignoringPlayersArray.add(id.toString());
        }

        object.add("ignoringPlayers", ignoringPlayersArray);

        JsonArray offlineItemsArray = new JsonArray();

        for (ItemStack is : player.getOfflineItems()) {
            String serialized = ItemUtil.serialize(is);
            offlineItemsArray.add(serialized);
        }

        object.add("offlineItems", offlineItemsArray);

        return object;
    }

    @Override
    public JsonElement serialize(AuctionsPlayer player, Type type, JsonSerializationContext context) {
        return serializeAuctionPlayer(player);
    }

    public static AuctionsPlayer deserializeAuctionsPlayers(JsonElement element) {
        JsonObject object = (JsonObject) element;

        UUID id = UUID.fromString(object.get("id").getAsString());

        boolean ignoringSpammy = object.get("ignoringSpammy").getAsBoolean(), ignoringAll =
                object.get("ignoringAll").getAsBoolean(), ignoringScoreboard = false;

        if (object.has("ignoringScoreboard")) {
            ignoringScoreboard = object.get("ignoringScoreboard").getAsBoolean();
        }

        List<UUID> ignoringPlayers = new ArrayList<>();

        if (object.has("ignoringPlayers")) {
            JsonArray ignoringPlayersArray = object.getAsJsonArray("ignoringPlayers");

            for (JsonElement je : ignoringPlayersArray) {
                ignoringPlayers.add(UUID.fromString(je.getAsString()));
            }
        }

        JsonArray array = object.getAsJsonArray("offlineItems");
        List<ItemStack> offlineItems = new ArrayList<>();

        for (JsonElement je : array) {
            try {
                ItemStack is = ItemUtil.deserialize(je.getAsString());
                offlineItems.add(is);
            } catch (IOException e) {
                Bukkit.getLogger().warning("[ezAuctions] Failed to deserialize an item for player \"" + id + "\"");
            }
        }

        return new AuctionsPlayer(id, ignoringSpammy, ignoringAll, ignoringScoreboard, ignoringPlayers, offlineItems);
    }

    @Override
    public AuctionsPlayer deserialize(JsonElement element, Type type,
                                      JsonDeserializationContext context) throws JsonParseException {
        return deserializeAuctionsPlayers(element);
    }
}
