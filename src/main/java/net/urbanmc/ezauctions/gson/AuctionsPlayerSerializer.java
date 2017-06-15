package net.urbanmc.ezauctions.gson;

import com.google.gson.*;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionsPlayerSerializer implements JsonSerializer<AuctionsPlayer>, JsonDeserializer<AuctionsPlayer> {

	@Override
	public JsonElement serialize(AuctionsPlayer player, Type type, JsonSerializationContext context) {
		JsonObject object = new JsonObject();

		object.addProperty("id", player.getUniqueId().toString());
		object.addProperty("ignoringSpammy", player.isIgnoringSpammy());
		object.addProperty("ignoringAll", player.isIgnoringAll());

		JsonArray array = new JsonArray();
		Gson gson = new Gson();

		for (ItemStack is : player.getOfflineItems()) {
			JsonElement je = gson.toJsonTree(is.serialize());
			array.add(je);
		}

		object.add("offlineItems", array);

		return object;
	}

	@Override
	public AuctionsPlayer deserialize(JsonElement element, Type type,
	                                  JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = (JsonObject) element;

		UUID id = UUID.fromString(object.get("id").getAsString());

		boolean ignoringSpammy = object.get("ignoringSpammy").getAsBoolean(), ignoringAll =
				object.get("ignoringAll").getAsBoolean();

		JsonArray array = object.getAsJsonArray("offlineItems");
		List<ItemStack> offlineItems = new ArrayList<>();

		Gson gson = new Gson();

		for (JsonElement je : array) {
			Map<String, Object> map = gson.fromJson(je, Map.class);
			ItemStack is = ItemStack.deserialize(map);

			offlineItems.add(is);
		}

		return new AuctionsPlayer(id, ignoringSpammy, ignoringAll, offlineItems);
	}
}
