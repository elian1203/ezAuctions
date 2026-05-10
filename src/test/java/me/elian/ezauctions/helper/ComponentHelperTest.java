package me.elian.ezauctions.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ComponentHelperTest {
	@Test
	void parsesSpigotTextComponentBooleansAsJsonBooleans() {
		String componentString = "[minecraft:custom_name={extra: ['\"', {bold: 0b, color: \"gold\", "
				+ "italic: 0b, obfuscated: 0b, strikethrough: 0b, text: 'My Special shovel\"', "
				+ "underlined: 0b}], text: \"\"},minecraft:lore=[{extra: [\"abc\"], text: \"\"}]]";

		Map<Key, DataComponentValue> components = ComponentHelper.getComponentsFromString(componentString);
		GsonDataComponentValue customName = assertInstanceOf(GsonDataComponentValue.class,
				components.get(Key.key("minecraft:custom_name")));

		JsonObject json = customName.element().getAsJsonObject();
		JsonArray extra = json.getAsJsonArray("extra");
		JsonObject name = extra.get(1).getAsJsonObject();

		assertEquals("\"", extra.get(0).getAsString());
		assertEquals("gold", name.get("color").getAsString());
		assertEquals("My Special shovel\"", name.get("text").getAsString());
		assertFalse(name.get("bold").getAsBoolean());
		assertFalse(name.get("italic").getAsBoolean());
		assertFalse(name.get("obfuscated").getAsBoolean());
		assertFalse(name.get("strikethrough").getAsBoolean());
		assertFalse(name.get("underlined").getAsBoolean());
	}
}
