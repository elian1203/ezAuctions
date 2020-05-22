package net.urbanmc.ezauctions.datastorage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.gson.AuctionsPlayerSerializer;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.AuctionsPlayerList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;

public class JSONStorage extends DataSource {

    private final File FILE;

    private final Gson gson =
            new GsonBuilder().registerTypeAdapter(AuctionsPlayer.class, new AuctionsPlayerSerializer()).create();

    public JSONStorage(EzAuctions plugin) {
        super(plugin);
        FILE = new File(EzAuctions.getDataDirectory(), "players.json");
    }

    @Override
    public boolean testAccess() {
        createFile();
        return true;
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

    /**
     * Loads the file and parses it as a json file.
     * @return parsed json element.
     */
    private JsonElement loadAsJson() {
        try (Scanner scanner = new Scanner(FILE)) {
            String json = scanner.nextLine();

            return new JsonParser().parse(json);
        } catch (Exception ex) {
            if (!(ex instanceof NoSuchElementException)) {
                plugin.getLogger().log(Level.SEVERE, "Error loading players!", ex);
            }

            return null;
        }
    }

    /**
     * Gets the players as a json array
     * @return a json array of the player data
     */
    private JsonArray getPlayersArray(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            plugin.getLogger().warning("Error loading players json file!");
            return null;
        }

        JsonArray players = element.getAsJsonObject().get("players").getAsJsonArray();

        if (players == null) {
            plugin.getLogger().warning("The players json could not be properly casted to an array!");
            return null;
        }

        return players;
    }

    /**
     *
     * @param playersArray json array of the player data
     * @param apUUID UUID of the auction player
     * @return the index of the auction player in the json array. Returns -1 if player is not found in the array.
     */
    private int getIndexOfAuctionPlayer(JsonArray playersArray, String apUUID) {
        for (int i = 0; i < playersArray.size(); i++) {
            JsonElement abstractElement = playersArray.get(i);

            if (!abstractElement.isJsonObject()) continue;

            JsonObject object = abstractElement.getAsJsonObject();

            String currentObjUUID = object.get("id").getAsString();

            if (currentObjUUID != null && currentObjUUID.equalsIgnoreCase(apUUID))
                return i;
        }

        return -1;
    }

    private void updateAuctionPlayer(AuctionsPlayer player) {
        JsonElement element = loadAsJson();

        JsonArray playersArray = getPlayersArray(element);

        if(playersArray == null) return;

        int index = getIndexOfAuctionPlayer(playersArray, player.getUniqueId().toString());

        JsonElement serializedPlayer = AuctionsPlayerSerializer.serializeAuctionPlayer(player);

        if (index == -1) {
            playersArray.add(serializedPlayer);
        }
        else {
            playersArray.set(index, serializedPlayer);
        }

        writeStringToFile(element.toString());
    }

    private void asyncUpdateAuctionPlayer(final AuctionsPlayer player) {
        runAsync(() -> {
            lock();
            try {
                updateAuctionPlayer(player);
            } finally {
                unlock();
            }
        });
    }

    // All methods basically swap the stored auction player with the passed in auction player
    @Override
    public void updateBooleanValue(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncUpdateAuctionPlayer(player);
    }

    @Override
    public void updateIgnored(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncUpdateAuctionPlayer(player);
    }

    @Override
    public void updateItems(List<AuctionsPlayer> list, AuctionsPlayer player) {
        asyncUpdateAuctionPlayer(player);
    }

    @Override
    public void save(List<AuctionsPlayer> auctionPlayers) {
        writeStringToFile(gson.toJson(new AuctionsPlayerList(auctionPlayers)));
    }

    private void writeStringToFile(String json) {
        try(PrintWriter writer = new PrintWriter(FILE)) {

            writer.write(json);

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<AuctionsPlayer> load() {
        try (Scanner scanner = new Scanner(FILE)) {
            String json = scanner.nextLine();

            return gson.fromJson(json, AuctionsPlayerList.class).getPlayers();
        } catch (Exception ex) {
            if (!(ex instanceof NoSuchElementException)) {
                plugin.getLogger().log(Level.SEVERE, "Error loading players!", ex);
            }

            return new ArrayList<>();
        }
    }
}
