package heos.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heos.Heos;
import heos.utils.HeosLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * Player data storage with JSON persistence
 */
public class PlayerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public String username;
    public UUID uuid;
    public String passwordHash;  // Stored as salt:hash
    public String lastIp;
    public boolean isOnlineAccount;
    public long registeredTime;
    public long lastLoginTime;
    
    public PlayerData(String username) {
        this.username = username;
        this.passwordHash = "";
        this.lastIp = "";
        this.isOnlineAccount = false;
        this.registeredTime = 0;
        this.lastLoginTime = 0;
    }
    
    public boolean isRegistered() {
        return passwordHash != null && !passwordHash.isEmpty();
    }
    
    /**
     * Saves player data to disk
     */
    public void save() {
        try {
            File dataDir = new File(Heos.gameDirectory.toFile(), "heos_data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            File playerFile = new File(dataDir, username.toLowerCase() + ".json");
            try (FileWriter writer = new FileWriter(playerFile)) {
                GSON.toJson(this, writer);
            }
            
            HeosLogger.debug("Saved player data for " + username);
        } catch (IOException e) {
            HeosLogger.error("Failed to save player data for " + username, e);
        }
    }
    
    /**
     * Loads player data from disk
     */
    public static PlayerData load(String username) {
        try {
            File dataDir = new File(Heos.gameDirectory.toFile(), "heos_data");
            File playerFile = new File(dataDir, username.toLowerCase() + ".json");
            
            if (!playerFile.exists()) {
                return new PlayerData(username);
            }
            
            try (FileReader reader = new FileReader(playerFile)) {
                PlayerData data = GSON.fromJson(reader, PlayerData.class);
                if (data == null) {
                    return new PlayerData(username);
                }
                HeosLogger.debug("Loaded player data for " + username);
                return data;
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to load player data for " + username, e);
            return new PlayerData(username);
        }
    }
}
