package net.urbanmc.ezauctions.datastorage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.urbanmc.ezauctions.EzAuctions;
import net.urbanmc.ezauctions.gson.AuctionsPlayerSerializer;
import net.urbanmc.ezauctions.object.AuctionsPlayer;
import net.urbanmc.ezauctions.object.AuctionsPlayerList;
import org.bukkit.Bukkit;

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

    private final File FILE = new File("plugins/ezAuctions", "players.json");

    private final Gson gson =
            new GsonBuilder().registerTypeAdapter(AuctionsPlayer.class, new AuctionsPlayerSerializer()).create();

    public JSONStorage(EzAuctions plugin) {
        super(plugin);
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

    @Override
    public void save(List<AuctionsPlayer> auctionPlayers) {
        try(PrintWriter writer = new PrintWriter(FILE)) {

            writer.write(gson.toJson(new AuctionsPlayerList(auctionPlayers)));

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
                Bukkit.getLogger().log(Level.SEVERE, "[ezAuctions] Error loading players!", ex);
            }

            return new ArrayList<>();
        }
    }
}
