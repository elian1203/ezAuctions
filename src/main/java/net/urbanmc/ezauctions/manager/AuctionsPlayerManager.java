package net.urbanmc.ezauctions.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.urbanmc.ezauctions.gson.AuctionsPlayerSerializer;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.AuctionsPlayerList;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;

public class AuctionsPlayerManager {

	private static AuctionsPlayerManager instance = new AuctionsPlayerManager();

	private final File FILE = new File("plugins/ezAuctions", "players.json");

	private final Gson gson =
			new GsonBuilder().registerTypeAdapter(AuctionsPlayer.class, new AuctionsPlayerSerializer()).create();

	private List<AuctionsPlayer> players;

	private AuctionsPlayerManager() {
		createFile();
		loadGson();
	}

	public static AuctionsPlayerManager getInstance() {
		return instance;
	}

	private void createFile() {
		if (!FILE.getParentFile().isDirectory()) {
			FILE.getParentFile().mkdir();
		}

		if (!FILE.exists()) {
			try {
				FILE.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadGson() {
		Scanner scanner = null;

		try {
			scanner = new Scanner(FILE);

			String json = scanner.nextLine();

			players = gson.fromJson(json, AuctionsPlayerList.class).getPlayers();
		} catch (Exception ex) {
			if (!(ex instanceof NoSuchElementException)) {
				Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error loading players!", ex);
			}

			players = new ArrayList<>();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	public void saveGson() {
		try {
			PrintWriter writer = new PrintWriter(FILE);

			writer.write(gson.toJson(new AuctionsPlayerList(players)));

			writer.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	public AuctionsPlayer getPlayer(UUID id) {
		for (AuctionsPlayer ap : players) {
			if (ap.getUniqueId().equals(id))
				return ap;
		}

		return null;
	}

	public void createPlayer(UUID id) {
		AuctionsPlayer ap = getPlayer(id);

		if (ap == null) {
			players.add(new AuctionsPlayer(id, false, false, new ArrayList<>()));
		}
	}
}
