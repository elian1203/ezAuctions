package me.elian.ezauctions.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.elian.ezauctions.Logger;
import me.elian.ezauctions.scheduler.TaskScheduler;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

@Singleton
public class UpdateController implements Listener {
	private static final String GITHUB_REPO = "elian1203/ezAuctions";
	// these two fields need to be static if the plugin is reloaded using /reload or PlugMan at runtime
	private static Thread shutdownHook;
	private static Path downloadedFilePath;

	private final Plugin plugin;
	private final TaskScheduler scheduler;
	private final Logger logger;
	private final ConfigController config;
	private final MessageController messages;
	private final String serverMinecraftVersion;
	private final String serverPluginVersion;
	private final HttpClient client;

	private String latestSupportedPluginVersion;

	@Inject
	public UpdateController(Plugin plugin, TaskScheduler scheduler, Logger logger, ConfigController config,
	                        MessageController messages) {
		this.plugin = plugin;
		this.scheduler = scheduler;
		this.logger = logger;
		this.config = config;
		this.messages = messages;

		serverMinecraftVersion = getServerMinecraftVersion();
		serverPluginVersion = plugin.getDescription().getVersion();
		client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	public String getServerPluginVersion() {
		return serverPluginVersion;
	}

	public String getLatestSupportedPluginVersion() {
		return latestSupportedPluginVersion;
	}

	private String getServerMinecraftVersion() {
		try {
			// paper/folia supported
			return (String) plugin.getServer().getClass().getMethod("getMinecraftVersion").invoke(plugin.getServer());
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			// spigot takes some more work
			String version = plugin.getServer().getVersion(); // "git-Paper-153 MC: 1.13.2"
			String[] spaceSplit = version.split(" "); // { "git-Paper-153", "MC:", "1.13.2" }
			return spaceSplit[2].replace("(", "").replace(")", ""); // "1.13.2"
		}
	}

	public void checkForUpdates() {
		if (!config.getConfig().getBoolean("general.check-updates"))
			return;

		scheduler.runAsyncTask(() -> {
			try {
				fetchLatestSupportedVersion();

				if (latestSupportedPluginVersion == null || latestSupportedPluginVersion.equals(serverPluginVersion))
					return;

				logger.info("Version " + latestSupportedPluginVersion + " is available!" +
						" You are currently running version " + serverPluginVersion + ".");

				if (config.getConfig().getBoolean("general.auto-update", true)) {
					// download update when auto update enabled
					downloadLatestSupportedVersion();
				} else {
					// only show update messages if auto updates are disabled
					scheduler.runSyncTask(() -> plugin.getServer().getPluginManager().registerEvents(this, plugin));
				}
			} catch (InterruptedException | ExecutionException e) {
				logger.warning("Error while checking latest release of plugin.", e);
			}
		});
	}

	public void shutdown() {
		moveDownloadedUpdateToPluginsDir();
	}

	private boolean versionIsEqualOrNewer(String oldVersion, String newVersion) {
		if (oldVersion == null || newVersion == null)
			return false;

		var newVersionParsed = ModuleDescriptor.Version.parse(newVersion);
		var oldVersionParsed = ModuleDescriptor.Version.parse(oldVersion);

		return newVersionParsed.compareTo(oldVersionParsed) >= 0;
	}

	public void fetchLatestSupportedVersion() throws ExecutionException, InterruptedException {
		String url = "https://api.github.com/repos/" + GITHUB_REPO + "/tags";
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();

		String response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.get();

		JsonElement element = JsonParser.parseString(response);
		JsonArray array = element.getAsJsonArray();

		int index = 0;

		while (index < array.size()) {
			JsonElement tag = array.get(index);
			JsonObject object = tag.getAsJsonObject();

			String tagName = object.get("name").getAsString();

			if (versionIsEqualOrNewer(serverPluginVersion, tagName) && isTagSupported(tagName)) {
				latestSupportedPluginVersion = tagName;
				return;
			}

			index++;
		}

		latestSupportedPluginVersion = null;
	}

	private boolean isTagSupported(String tagName) throws ExecutionException, InterruptedException {
		String url = "https://raw.githubusercontent.com/"
				+ GITHUB_REPO + "/" + tagName + "/src/main/resources/plugin.yml";
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();

		String response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.get();

		StringReader reader = new StringReader(response);
		YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);

		String apiVersion = config.getString("api-version");

		return versionIsEqualOrNewer(apiVersion, serverMinecraftVersion);
	}

	public void downloadLatestSupportedVersion() throws ExecutionException, InterruptedException {
		if (downloadedFilePath != null) {
			downloadedFilePath.toFile().delete();
		}

		logger.info("Downloading plugin update...");
		String releaseUrl =
				"https://api.github.com/repos/" + GITHUB_REPO + "/releases/tags/" + latestSupportedPluginVersion;
		HttpRequest request = HttpRequest.newBuilder(URI.create(releaseUrl)).build();
		String releaseJson = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.get();

		JsonObject releaseObject = JsonParser.parseString(releaseJson).getAsJsonObject();
		JsonArray releaseAssets = releaseObject.getAsJsonArray("assets");

		JsonObject jarAsset = null;
		for (JsonElement assetElement : releaseAssets) {
			JsonObject assetObject = assetElement.getAsJsonObject();
			String name = assetObject.get("name").getAsString().toLowerCase();
			if (name.contains("ezauctions") && name.endsWith(".jar")) {
				jarAsset = assetObject;
				break;
			}
		}

		if (jarAsset == null) {
			logger.warning("Could not find release asset to download!");
			return;
		}

		String downloadUrl = jarAsset.get("url").getAsString();
		Path downloadPath = Path.of(plugin.getDataFolder().getAbsolutePath(),
				"ezAuctions_" + latestSupportedPluginVersion + ".jar");

		request = HttpRequest.newBuilder(URI.create(downloadUrl))
				.header("Accept", "application/octet-stream")
				.build();
		HttpResponse<InputStream> downloadResponse =
				client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).join();

		if (downloadResponse.statusCode() >= 200 && downloadResponse.statusCode() < 300) {
			try {
				Files.copy(downloadResponse.body(), downloadPath, StandardCopyOption.REPLACE_EXISTING);
				downloadedFilePath = downloadPath;
				logger.info("Finished downloading plugin update. Update will be applied when the server is shutdown.");
			} catch (IOException e) {
				logger.severe("Could not copy downloaded plugin update to file system!", e);
			}
		} else {
			logger.warning("Failed to download plugin update! " + downloadResponse.statusCode());
		}
	}

	private void moveDownloadedUpdateToPluginsDir() {
		if (shutdownHook != null) {
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		}

		if (downloadedFilePath == null)
			return;

		// use shutdown hook to wait until lock on jar is released
		shutdownHook = new Thread(() -> {
			Path newFilePath = Path.of(plugin.getDataFolder().getParentFile().getAbsolutePath(),
					downloadedFilePath.toFile().getName());
			try {
				Files.move(downloadedFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING);
				File existingJarFile = new File(getClass().getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.getPath());

				if (existingJarFile.exists()) {
					existingJarFile.delete();
				}
			} catch (IOException e) {
				logger.severe("Could not move new jar file!", e);
			}
		});

		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();

		if (p.hasPermission("ezauctions.updatemessage")) {
			messages.sendMessage(p, "login.updatemessage",
					Placeholder.unparsed("latestversion", latestSupportedPluginVersion),
					Placeholder.unparsed("serverversion", serverPluginVersion));
		}
	}
}
