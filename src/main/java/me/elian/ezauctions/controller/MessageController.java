package me.elian.ezauctions.controller;

import co.aikar.commands.PaperCommandManager;
import co.aikar.locales.MessageKey;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.helper.ItemHelper;
import me.elian.ezauctions.model.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class MessageController extends FileHandler {
	private static final String RESOURCE_NAME = "messages.properties";
	private static final Pattern HAS_CUSTOM_NAME_PATTERN = Pattern.compile("<hascustomname>(.*)</hascustomname>",
			Pattern.DOTALL);
	private static final Pattern NO_CUSTOM_NAME_PATTERN = Pattern.compile("<nocustomname>(.*)</nocustomname>",
			Pattern.DOTALL);
	private static final Pattern SKULL_PATTERN = Pattern.compile("<skull>(.*)</skull>", Pattern.DOTALL);
	private static final Pattern AUTOBUY_PATTERN = Pattern.compile("<autobuy>(.*)</autobuy>", Pattern.DOTALL);
	private static final Pattern SEALED_PATTERN = Pattern.compile("<sealed>(.*)</sealed>", Pattern.DOTALL);
	private static final Pattern REPAIR_PATTERN = Pattern.compile("<repair>(.*)</repair>", Pattern.DOTALL);
	private static final Pattern UNREPAIRABLE_PATTERN = Pattern.compile("<unrepairable>(.*)</unrepairable>",
			Pattern.DOTALL);
	private final Plugin plugin;
	private final Logger logger;
	private final PaperCommandManager commandManager;
	private final ConfigController config;
	private final Economy economy;
	private ResourceBundle bundle;
	private BukkitAudiences audiences;
	private String prefixRaw;

	@Inject
	public MessageController(Plugin plugin, Logger logger, PaperCommandManager commandManager,
	                         ConfigController config, Economy economy) {
		super(plugin, logger, RESOURCE_NAME);
		this.plugin = plugin;
		this.logger = logger;
		this.commandManager = commandManager;
		this.config = config;
		this.economy = economy;

		try {
			reload();
		} catch (IOException e) {
			logger.severe("Could not load messages file!", e);
		}

		try {
			Player.class.getMethod("sendMessage", Component.class);
		} catch (NoSuchMethodException e) {
			audiences = BukkitAudiences.create(plugin);
		}
	}

	public void shutdown() {
		if (audiences != null) {
			audiences.close();
		}
	}

	public void sendMessage(@Nullable CommandSender target, @NotNull String key, @Nullable TagResolver... resolvers) {
		if (target == null)
			return;

		Component message = getComponentMessage(key, resolvers);
		sendComponentToSender(target, message);
	}

	public void sendAuctionMessage(@Nullable CommandSender target, @NotNull String key, @NotNull Auction auction,
	                               @Nullable TagResolver... resolvers) {
		if (target == null)
			return;

		Component message = getAuctionComponent(getRawMessage(key), auction.getAuctionData(), auction.getBidList(),
				auction.getRemainingSeconds(), resolvers);
		sendComponentToSender(target, message);
	}

	public void sendAuctionMessage(@Nullable CommandSender target, @NotNull String key, @NotNull AuctionData data,
	                               @Nullable TagResolver... resolvers) {
		if (target == null)
			return;

		Component message = getAuctionComponent(getRawMessage(key), data, null, data.getStartingAuctionTime(),
				resolvers);
		sendComponentToSender(target, message);
	}

	public void broadcastAuctionMessage(@NotNull Set<AuctionPlayer> onlinePlayers, @NotNull Auction auction,
	                                    boolean spammy, @NotNull String key, @Nullable TagResolver... resolvers) {
		UUID auctioneerId = auction.getAuctionData().getAuctioneer().getUniqueId();
		String world = auction.getAuctionData().getWorld();

		Component message = getAuctionComponent(getRawMessage(key), auction.getAuctionData(), auction.getBidList(),
				auction.getRemainingSeconds(), resolvers);
		for (AuctionPlayer ap : onlinePlayers) {
			// ensure player isn't ignoring all messages
			if (ap.isIgnoringAll())
				continue;

			// ensure player isn't ignoring spammy messages
			if (spammy && ap.isIgnoringSpammy())
				continue;

			// ensure player isn't ignoring auctioneer
			if (ap.getIgnoredPlayers() != null
					&& ap.getIgnoredPlayers().stream().anyMatch(ip -> ip.getIgnored().equals(auctioneerId)))
				continue;

			Player target = ap.getOnlinePlayer();

			if (target == null)
				continue;

			// ensure player in right world if per-world-broadcast enabled
			String playerWorld = target.getWorld().getName();
			if (config.getConfig().getBoolean("auctions.per-world-broadcast")
					&& world != null && !world.equals(playerWorld))
				continue;

			sendComponentToSender(target, message);
		}

		sendComponentToSender(plugin.getServer().getConsoleSender(), message);
	}

	public List<Component> getAuctionComponentLines(@NotNull String key, @NotNull Auction auction,
	                                                @Nullable TagResolver... extraResolvers) {
		String raw = getRawMessage(key);
		if (raw == null)
			return new ArrayList<>();

		String booleansReplaced = replaceAuctionPatterns(raw, auction.getAuctionData());
		TagResolver[] tagResolvers = getAuctionTagResolvers(auction.getAuctionData(), auction.getBidList(),
				auction.getRemainingSeconds());
		TagResolver[] mergedResolvers = ArrayUtils.addAll(tagResolvers, extraResolvers);

		return Arrays.stream(booleansReplaced.split("\n"))
				.map(s -> {
					try {
						return MiniMessage.miniMessage().deserialize(s, mergedResolvers);
					} catch (Exception e) {
						logger.severe("Error parsing message! Auction broadcast will not work correctly! " + key, e);
						return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
					}
				})
				.collect(Collectors.toList());
	}

	private Component getAuctionComponent(String rawMessage, AuctionData data, @Nullable BidList bidList,
	                                      int remainingSeconds, TagResolver... extraResolvers) {
		if (rawMessage == null)
			return Component.empty();

		String message = replaceAuctionPatterns(rawMessage, data);
		TagResolver[] tagResolvers = getAuctionTagResolvers(data, bidList, remainingSeconds);
		TagResolver[] mergedResolvers = ArrayUtils.addAll(tagResolvers, extraResolvers);

		Component parsed;
		try {
			parsed = MiniMessage.miniMessage().deserialize(message, mergedResolvers);
			parsed = reconstructAuctionComponentRecursive(parsed, data);
		} catch (Exception e) {
			logger.severe("Error parsing message! Auction broadcast will not work correctly!\n" + rawMessage, e);
			parsed = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
		}

		return parsed;
	}

	private Component reconstructAuctionComponentRecursive(Component component, AuctionData data) {
		Component returnComponent = component;

		List<Component> childrenUnmodifiable = component.children();
		if (!childrenUnmodifiable.isEmpty()) {
			List<Component> children = new ArrayList<>(childrenUnmodifiable.size());
			for (Component child : childrenUnmodifiable) {
				children.add(reconstructAuctionComponentRecursive(child, data));
			}

			returnComponent = returnComponent.children(children);
		}

		HoverEvent<?> hoverEvent = component.hoverEvent();
		if (hoverEvent != null && hoverEvent.action().type() == HoverEvent.ShowItem.class) {
			HoverEvent.ShowItem showItem = (HoverEvent.ShowItem) hoverEvent.value();
			if (showItem.item().compareTo(data.getItemKey()) == 0
					&& (showItem.nbt() == null || showItem.nbt().string().isEmpty())) {
				HoverEvent<HoverEvent.ShowItem> newHoverEvent = ItemHelper.getItemHover(data.getItem(),
						initialEvent -> initialEvent.item(data.getItemKey())
													.count(Math.min(data.getAmount(), data.getItem().getMaxStackSize()))); // if amount greater than max stack size, use max stack size to prevent error
				returnComponent = returnComponent.hoverEvent(newHoverEvent);
			}
		}

		return returnComponent;
	}

	private TagResolver[] getAuctionTagResolvers(AuctionData data, BidList bidList, int remainingSeconds) {
		ItemStack item = data.getItem();

		String auctioneerName = data.getAuctioneer().getOfflinePlayer().getName();
		if (auctioneerName == null) {
			auctioneerName = "";
		}

		double highestBidAmount = data.getStartingPrice();
		String highestBidderName = null;
		String highestBidderUniqueId = "";
		if (bidList != null) {
			Bid highestBid = bidList.getHighestBid();
			if (highestBid != null && !data.isSealed()) {
				highestBidAmount = highestBid.amount();
				highestBidderName = highestBid.auctionPlayer().getOfflinePlayer().getName();
				highestBidderUniqueId = highestBid.auctionPlayer().getUniqueId().toString();
			}
		}

		if (highestBidderName == null) {
			highestBidderName = "";
		}

		return new TagResolver[]{
				Placeholder.unparsed("auctioneer", auctioneerName),
				Placeholder.unparsed("auctioneeruuid", data.getAuctioneer().getUniqueId().toString()),
				Formatter.number("itemamount", data.getAmount()),
				Placeholder.unparsed("minecraftname", data.getMinecraftName()),
				Placeholder.unparsed("customname", data.getCustomName()),
				Placeholder.unparsed("materialtype", item.getType().toString().toLowerCase()),
				Formatter.number("startingprice", data.getStartingPrice()),
				Formatter.number("highestbidamount", highestBidAmount),
				Placeholder.unparsed("highestbidder", highestBidderName),
				Placeholder.unparsed("highestbidderuuid", highestBidderUniqueId),
				Formatter.number("increment", data.getIncrementPrice()),
				Formatter.number("starttime", data.getStartingAuctionTime()),
				Formatter.number("remainingtime", remainingSeconds),
				Formatter.number("autobuy", data.getAutoBuyPrice()),
				Placeholder.unparsed("world", data.getWorld()),
				Placeholder.unparsed("skullowner", data.getSkullOwner()),
				Formatter.number("repairprice", data.getRepairPrice()),
				Formatter.number("antisnipetime", config.getConfig().getInt("antisnipe.time")),
				Placeholder.unparsed("currencynameplural", economy.currencyNamePlural()),
				Placeholder.unparsed("currencynamesingular", economy.currencyNameSingular()),
		};
	}

	private Component getComponentMessage(String key, TagResolver... resolvers) {
		String message = getRawMessage(key);
		if (message == null)
			return Component.text().build();

		Component parsed;
		try {
			parsed = MiniMessage.miniMessage().deserialize(message, resolvers);
		} catch (Exception e) {
			logger.warning("Message key " + key + " not in valid MiniMessage format!" +
					" Falling back to legacy text.", e);
			parsed = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
		}

		return parsed;
	}

	private String getLegacyMessage(String key, TagResolver... resolvers) {
		Component message = getComponentMessage(key, resolvers);
		return LegacyComponentSerializer.legacyAmpersand().serialize(message);
	}

	private @Nullable String getRawMessage(String key) {
		try {
			String message = getStringFromBundle(key);
			if (message == null)
				return null;

			return message.replace("{prefix}", prefixRaw);
		} catch (Exception e) {
			logger.severe("Exception when attempting to get message! " + key);
			return null;
		}
	}

	private String getStringFromBundle(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException resourceException) {
			logger.warning("Missing message key in messages file! (" + key + ") " +
					"Loading from default messages.properties. This will make the plugin slower!", resourceException);
			try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
				if (inputStream == null)
					throw new IOException();

				try (Reader reader = new InputStreamReader(inputStream)) {
					ResourceBundle defaultBundle = new PropertyResourceBundle(reader);
					return defaultBundle.getString(key);
				}
			} catch (IOException e) {
				logger.severe("Critical error when loading default messages.properties!", e);
				return null;
			}
		}
	}

	private String replaceAuctionPatterns(String raw, AuctionData data) {
		String replaced = raw;
		replaced = replaceAuctionPattern(replaced, HAS_CUSTOM_NAME_PATTERN,
				!data.getCustomName().equals(data.getMinecraftName()));
		replaced = replaceAuctionPattern(replaced, NO_CUSTOM_NAME_PATTERN,
				data.getCustomName().equals(data.getMinecraftName()));
		replaced = replaceAuctionPattern(replaced, SKULL_PATTERN, !data.getSkullOwner().isEmpty());
		replaced = replaceAuctionPattern(replaced, AUTOBUY_PATTERN, data.getAutoBuyPrice() != 0);
		replaced = replaceAuctionPattern(replaced, SEALED_PATTERN, data.isSealed());
		replaced = replaceAuctionPattern(replaced, REPAIR_PATTERN, data.getRepairPrice() > 0);
		replaced = replaceAuctionPattern(replaced, UNREPAIRABLE_PATTERN, data.getRepairPrice() == -1);
		replaced = replaced.replace("<auctioneeruuid>", data.getAuctioneer().getUniqueId().toString());
		replaced = replaced.replace("<highestbidderuuid>", data.getAuctioneer().getUniqueId().toString());
		replaced = replaced.replace("<materialtype>", data.getItem().getType().toString().toLowerCase());
		replaced = replaced.replace("<itemamount>", Integer.toString(data.getAmount()));
		replaced = replaced.replace("<minecraftname>", data.getMinecraftName());
		replaced = replaced.replace("<customname>", data.getCustomName());
		replaced = replaced.replace("<itemnbt>", "");
		return replaced;
	}

	private String replaceAuctionPattern(String original, Pattern pattern, boolean show) {
		Matcher matcher = pattern.matcher(original);
		String replaced = original;

		while (matcher.find()) {
			String match = original.substring(matcher.start(), matcher.end());
			if (show) {
				replaced = replaced.replace(match, matcher.group(1));
			} else {
				replaced = replaced.replace(match, "");
			}
		}

		return replaced;
	}

	private void sendComponentToSender(CommandSender target, Component component) {
		if (audiences == null) {
			target.sendMessage(component);
		} else {
			audiences.sender(target).sendMessage(component);
		}
	}

	@Override
	protected void loadFile(@NotNull Reader reader) {
		try {
			bundle = new PropertyResourceBundle(reader);

			// load the prefix once rather than every time a message is sent
			prefixRaw = getStringFromBundle("prefix");
			if (prefixRaw == null) {
				prefixRaw = "";
			}

			// Replace ACF messages with our own
			commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.permission_denied"),
					getLegacyMessage("command.no_perm"));
			commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core" +
							".permission_denied_parameter"),
					getLegacyMessage("command.no_perm"));
			commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.error_prefix"),
					getLegacyMessage("command.error_prefix"));
			commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKey.of("acf-core.invalid_syntax"),
					getLegacyMessage("command.usage"));
		} catch (IOException e) {
			logger.severe("Could not load resource " + RESOURCE_NAME + "! Check file permissions.");
		}
	}
}
