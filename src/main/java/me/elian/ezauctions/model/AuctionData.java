package me.elian.ezauctions.model;

import me.elian.ezauctions.PluginLogger;
import me.elian.ezauctions.controller.ConfigController;
import me.elian.ezauctions.controller.MessageController;
import me.elian.ezauctions.helper.ItemHelper;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public final class AuctionData {
	private final AuctionPlayer auctioneer;
	private final ItemStack item;
	private final String amountString;
	private final boolean isSealed;
	private final String world;
	private double startingPrice;
	private int startingAuctionTime;
	private double incrementPrice;
	private double autoBuyPrice;
	private int amount;
	private String skullOwner;
	private int repairPrice;
	private String itemNbt;
	private String minecraftName;
	private String customName;

	public AuctionData(AuctionPlayer auctioneer, ItemStack item, String amountString, int startingAuctionTime,
	                   double startingPrice, double incrementPrice, double autoBuyPrice, boolean isSealed,
	                   String world) {
		this.auctioneer = auctioneer;
		this.item = item;
		this.amountString = amountString;
		this.startingAuctionTime = startingAuctionTime;
		this.startingPrice = startingPrice;
		this.incrementPrice = incrementPrice;
		this.autoBuyPrice = autoBuyPrice;
		this.isSealed = isSealed;
		this.world = world;
	}

	public AuctionPlayer getAuctioneer() {
		return auctioneer;
	}

	public ItemStack getItem() {
		return item;
	}

	public int getAmount() {
		return amount;
	}

	public int getStartingAuctionTime() {
		return startingAuctionTime;
	}

	public double getStartingPrice() {
		return startingPrice;
	}

	public double getIncrementPrice() {
		return incrementPrice;
	}

	public double getAutoBuyPrice() {
		return autoBuyPrice;
	}

	public boolean isSealed() {
		return isSealed;
	}

	public String getWorld() {
		return world;
	}

	public String getSkullOwner() {
		return skullOwner;
	}

	public int getRepairPrice() {
		return repairPrice;
	}

	public String getItemNbt() {
		return itemNbt;
	}

	public String getMinecraftName() {
		return minecraftName;
	}

	public String getCustomName() {
		return customName;
	}

	public void gatherAdditionalData(PluginLogger logger) {
		if (item == null || item.getType() == Material.AIR)
			return;

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SkullMeta skullMeta) {
			skullOwner = skullMeta.getOwner();
		}

		if (skullOwner == null) {
			skullOwner = "";
		}

		repairPrice = ItemHelper.getXPForRepair(item);

		try {
			itemNbt = ItemHelper.getItemNbt(item).replace("minecraft:", "");
		} catch (Exception e) {
			itemNbt = "";
			logger.severe("Could not get item NBT! Item hover will not work correctly!", e);
		}

		minecraftName = ItemHelper.getMinecraftName(item);
		customName = minecraftName;
		if (meta != null && !meta.getDisplayName().isBlank()) {
			Component legacySection = LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
			customName = MiniMessage.miniMessage().serialize(legacySection);
		}
	}

	public void fillDefaults(@NotNull ConfigController configController) {
		FileConfiguration config = configController.getConfig();

		if (startingAuctionTime == 0) {
			startingAuctionTime = config.getInt("auctions.default.auction-time");
		}

		if (incrementPrice == 0) {
			incrementPrice = config.getInt("auctions.default.increment");
		}
	}

	public boolean validate(@NotNull ConfigController configController, @NotNull MessageController messages,
	                        Player player) {
		FileConfiguration config = configController.getConfig();

		truncateDecimals(config);
		return validateGameMode(config, messages, player)
				&& validateWorld(config, messages, player)
				&& validateType(config, messages, player)
				&& findAmount(messages, player)
				&& validateDamage(config, messages, player)
				&& validateStartingPrice(config, messages, player)
				&& validateIncrement(config, messages, player)
				&& validateAutoBuy(config, messages, player)
				&& validateTime(config, messages, player);
	}

	private void truncateDecimals(FileConfiguration config) {
		startingPrice = truncateToDecimalPlace(startingPrice, config.getInt("auctions.decimal.starting-price"));
		incrementPrice = truncateToDecimalPlace(incrementPrice, config.getInt("auctions.decimal.increment"));
		autoBuyPrice = truncateToDecimalPlace(autoBuyPrice, config.getInt("auctions.decimal.autobuy"));
	}

	private double truncateToDecimalPlace(double number, int decimalPlaces) {
		double pow = Math.pow(10, decimalPlaces);
		return Math.round(number * pow) / pow;
	}

	private boolean validateStartingPrice(FileConfiguration config, MessageController messages, Player player) {
		double min = config.getDouble("auctions.minimum.starting-price");
		double max = config.getDouble("auctions.maximum.starting-price");
		if (startingPrice <= 0 || startingPrice < min) {
			messages.sendMessage(player, "command.auction.start.invalid_start_price.min",
					Formatter.number("min", min),
					Formatter.number("max", max),
					Formatter.number("entered", startingPrice));
			return false;
		}

		if (startingPrice > max && max != 0) {
			messages.sendMessage(player, "command.auction.start.invalid_start_price.max",
					Formatter.number("min", min),
					Formatter.number("max", max),
					Formatter.number("entered", startingPrice));
			return false;
		}

		return true;
	}

	private boolean validateIncrement(FileConfiguration config, MessageController messages, Player player) {
		double min = config.getDouble("auctions.minimum.increment");
		double max = config.getDouble("auctions.maximum.increment");

		if (min == -1 && max == -1) {
			incrementPrice = config.getDouble("auctions.default.increment");
			return true;
		}

		if (incrementPrice <= 0 || incrementPrice < min || (incrementPrice > max && max != 0)) {
			messages.sendMessage(player, "command.auction.start.invalid-inc",
					Formatter.number("min", min),
					Formatter.number("max", max),
					Formatter.number("entered", incrementPrice));
			return false;
		}

		return true;
	}

	private boolean validateAutoBuy(FileConfiguration config, MessageController messages, Player player) {
		double min = config.getDouble("auctions.minimum.autobuy");
		double max = config.getDouble("auctions.maximum.autobuy");

		if (min == -1 && max == -1) {
			autoBuyPrice = config.getDouble("auctions.default.autobuy");
			return true;
		}

		if (autoBuyPrice < 0 || autoBuyPrice < min || (autoBuyPrice > max && max != 0)) {
			messages.sendMessage(player, "command.auction.start.invalid-buyout",
					Formatter.number("min", min),
					Formatter.number("max", max),
					Formatter.number("entered", autoBuyPrice));
			return false;
		}

		return true;
	}

	private boolean validateTime(FileConfiguration config, MessageController messages, Player player) {
		double min = config.getDouble("auctions.minimum.auction-time");
		double max = config.getDouble("auctions.maximum.auction-time");

		if (min == -1 && max == -1) {
			startingAuctionTime = config.getInt("auctions.default.auction-time");
			return true;
		}

		if (startingAuctionTime <= 0 || startingAuctionTime < min || (startingAuctionTime > max && max != 0)) {
			messages.sendMessage(player, "command.auction.start.invalid-time",
					Formatter.number("min", min),
					Formatter.number("max", max),
					Formatter.number("entered", startingAuctionTime));
			return false;
		}

		return true;
	}

	private boolean validateDamage(FileConfiguration config, MessageController messages, Player player) {
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof Damageable damageable && damageable.hasDamage()
				&& config.getBoolean("auctions.toggles.restrict-damaged")) {
			messages.sendMessage(player, "command.auction.start.damaged_item");
			return false;
		}

		return true;
	}

	private boolean validateGameMode(FileConfiguration config, MessageController messages, Player player) {
		if (player.getGameMode() == GameMode.CREATIVE && config.getBoolean("auctions.toggles.deny-creative")) {
			messages.sendMessage(player, "command.auction.start.deny-creative");
			return false;
		}

		return true;
	}

	private boolean validateWorld(FileConfiguration config, MessageController messages, Player player) {
		if (config.getStringList("auctions.blocked-worlds").stream().anyMatch(blocked -> blocked.equalsIgnoreCase(player.getWorld().getName()))) {
			messages.sendMessage(player, "command.auction.start.blocked-worlds");
			return false;
		}

		return true;
	}

	private boolean validateType(FileConfiguration config, MessageController messages, Player player) {
		if (item.getType() == Material.AIR) {
			messages.sendMessage(player, "command.auction.start.cannot_auction_air");
			return false;
		}

		String typeString = item.getType().toString();
		if (config.getStringList("auctions.blocked-materials").stream().anyMatch(blocked -> blocked.equalsIgnoreCase(typeString))) {
			messages.sendMessage(player, "command.auction.start.blocked-materials");
			return false;
		}

		return true;
	}

	private boolean findAmount(MessageController messages, Player player) {
		if (amountString.equals("h") || amountString.equals("hand")) {
			amount = item.getAmount();
		} else if (amountString.equals("a") || amountString.equals("all")) {
			amount = ItemHelper.getAmountOfItemInInventory(player, item);
		} else {
			try {
				amount = Integer.parseInt(amountString);
				double amountInInventory = ItemHelper.getAmountOfItemInInventory(player, item);

				if (amount <= 0 || amount > amountInInventory)
					throw new IllegalArgumentException();
			} catch (Exception e) {
				messages.sendMessage(player, "command.auction.start.invalid-amt");
				return false;
			}
		}

		return true;
	}

	public boolean giveItemToPlayer(AuctionPlayer auctionPlayer, TaskScheduler scheduler, ConfigController config,
	                                MessageController messages) {
		Player player = auctionPlayer.getOnlinePlayer();
		if (player == null) {
			addSavedItemToPlayer(auctionPlayer, scheduler);
			return false;
		}

		if (config.getConfig().getBoolean("auctions.per-world-auctions")
				&& !player.getWorld().getName().equals(world)) {
			messages.sendMessage(player, "reward.wrong_world", Placeholder.unparsed("itemworld", world));
			addSavedItemToPlayer(auctionPlayer, scheduler);
			return false;
		}

		if (config.getConfig().getStringList("auctions.blocked-worlds").contains(player.getWorld().getName())) {
			messages.sendMessage(player, "reward.blocked_world");
			addSavedItemToPlayer(auctionPlayer, scheduler);
			return false;
		}

		scheduler.runPlayerRegionTask(() -> {
			boolean overflow = ItemHelper.addItemToPlayerInventory(player, item, amount);
			if (overflow) {
				messages.sendMessage(player, "reward.full_inventory");
			}
		}, player);

		return true;
	}

	private void addSavedItemToPlayer(AuctionPlayer auctionPlayer, TaskScheduler scheduler) {
		scheduler.runAsyncTask(() -> {
			SavedItem savedItem = new SavedItem(auctionPlayer, item, amount, world);
			auctionPlayer.getSavedItems().add(savedItem);
		});
	}
}
