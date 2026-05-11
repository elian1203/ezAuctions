package me.elian.ezauctions.helper;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ComponentHelperTest {
	@Test
	void parsesSpigotComponentValuesAsGsonDataComponents() {
		String componentString = "[minecraft:custom_name={extra: [{bold: 0b, color: \"gold\", "
				+ "italic: 0b, obfuscated: 0b, strikethrough: 0b, text: \"My Special shovel\", "
				+ "underlined: 0b}], text: \"\"},minecraft:lore=[{extra: [\"abc\"], text: \"\"}]]";

		Map<Key, DataComponentValue> components = ComponentHelper.getComponentsFromString(componentString);
		GsonDataComponentValue customName = assertInstanceOf(GsonDataComponentValue.class,
				components.get(Key.key("minecraft:custom_name")));
		GsonDataComponentValue lore = assertInstanceOf(GsonDataComponentValue.class,
				components.get(Key.key("minecraft:lore")));
		JsonObject name = customName.element().getAsJsonObject()
				.getAsJsonArray("extra")
				.get(0)
				.getAsJsonObject();

		assertEquals("gold", name.get("color").getAsString());
		assertEquals("My Special shovel", name.get("text").getAsString());
		assertFalse(name.get("bold").getAsBoolean());
		assertFalse(name.get("italic").getAsBoolean());
		assertFalse(name.get("obfuscated").getAsBoolean());
		assertFalse(name.get("strikethrough").getAsBoolean());
		assertFalse(name.get("underlined").getAsBoolean());
		assertEquals("abc", lore.element().getAsJsonArray().get(0).getAsJsonObject()
				.getAsJsonArray("extra").get(0).getAsString());
	}

	@Test
	void parsesRemovedComponents() {
		Map<Key, DataComponentValue> components = ComponentHelper.getComponentsFromString("[!minecraft:custom_name]");

		assertSame(DataComponentValue.removed(), components.get(Key.key("minecraft:custom_name")));
	}
}
