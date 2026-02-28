package heos;

import heos.config.HeosConfig;
import heos.storage.BanData;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Heos class
 */
public class Heos {
    public static Path gameDirectory;
    
    // Configuration
    private static HeosConfig config;
    
    // Ban data
    private static BanData banData;
    
    // Player data cache
    private static final Map<String, PlayerData> playerDataCache = new HashMap<>();
    
    /**
     * Gets configuration
     */
    public static HeosConfig getConfig() {
        if (config == null) {
            config = HeosConfig.load();
        }
        return config;
    }
    
    /**
     * Gets ban data
     */
    public static BanData getBanData() {
        if (banData == null) {
            banData = BanData.load();
        }
        return banData;
    }
    
    /**
     * Gets or creates player data
     */
    public static PlayerData getPlayerData(String username) {
        return playerDataCache.computeIfAbsent(username.toLowerCase(), k -> PlayerData.load(username));
    }
    
    /**
     * Removes player data from cache
     */
    public static void removePlayerData(String username) {
        playerDataCache.remove(username.toLowerCase());
    }

    static void onStartServer(MinecraftServer server) {
        HeosLogger.info("=================================");
        HeosLogger.info("  _   _ _____    ___  ____  ");
        HeosLogger.info(" | | | | ____|  / _ \\/ ___| ");
        HeosLogger.info(" | |_| |  _|   | | | \\___ \\ ");
        HeosLogger.info(" |  _  | |___  | |_| |___) |");
        HeosLogger.info(" |_| |_|_____|  \\___/|____/ ");
        HeosLogger.info("=================================");
        HeosLogger.info("Heos authentication system started!");
        HeosLogger.info("Minecraft version: " + server.getVersion());
        HeosLogger.info("Game directory: " + gameDirectory);
        HeosLogger.info("Online mode: " + server.isOnlineMode());
        
        // Load config and ban data
        config = HeosConfig.load();
        banData = BanData.load();
        
        HeosLogger.info("Authentication: " + (config.enableAuthentication ? "Enabled" : "Disabled"));
        HeosLogger.info("Whitelist: " + (config.enableWhitelist ? "Enabled" : "Disabled"));
        HeosLogger.info("Custom Ban: " + (config.enableCustomBan ? "Enabled" : "Disabled"));
        HeosLogger.info("Debug Logging: " + (config.enableDebugLogging ? "Enabled" : "Disabled"));
    }

    static void onStopServer(MinecraftServer server) {
        HeosLogger.info("=================================");
        HeosLogger.info("Shutting down Heos server...");
        HeosLogger.info("Clearing player data cache...");
        playerDataCache.clear();
        HeosLogger.info("Goodbye!");
        HeosLogger.info("=================================");
    }
}
