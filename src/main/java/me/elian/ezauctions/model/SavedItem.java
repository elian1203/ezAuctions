package me.elian.ezauctions.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.elian.ezauctions.helper.ItemHelper;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Base64;

@DatabaseTable(tableName = "ezAuctions_SavedItem")
public class SavedItem {
	@DatabaseField(generatedId = true)
	private int id;

	@DatabaseField(foreign = true)
	private AuctionPlayer auctionPlayer;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	private byte[] serializedItemBytes;

	@DatabaseField
	private int amount;

	@DatabaseField
	private String world;

	public SavedItem() {
	}

	public SavedItem(@NotNull AuctionPlayer auctionPlayer, @NotNull ItemStack itemStack, int amount,
	                 @NotNull String world) {
		this.auctionPlayer = auctionPlayer;
		this.serializedItemBytes = ItemHelper.serialize(itemStack);
		this.amount = amount;
		this.world = world;
	}

	@NotNull
	public String getSerializedItemJson() {
		return new String(Base64.getDecoder().decode(serializedItemBytes));
	}

	@NotNull
	public ItemStack getItemStack() throws IOException {
		return ItemHelper.deserialize(serializedItemBytes);
	}

	public int getAmount() {
		return amount;
	}

	@NotNull
	public String getWorld() {
		return world;
	}
}