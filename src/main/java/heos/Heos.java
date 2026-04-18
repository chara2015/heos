package heos;

import heos.config.HeosConfig;
import heos.storage.BanData;
import heos.storage.PlayerData;
import heos.storage.WhitelistData;
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

    private static HeosConfig config;
    private static BanData banData;
    private static WhitelistData whitelistData;
    private static final Map<String, PlayerData> playerDataCache = new HashMap<>();

    public static HeosConfig getConfig() {
        if (config == null) {
            config = HeosConfig.load();
        }
        return config;
    }

    public static BanData getBanData() {
        if (banData == null) {
            banData = BanData.load();
        }
        return banData;
    }

    public static WhitelistData getWhitelistData() {
        if (whitelistData == null) {
            whitelistData = WhitelistData.load();
        }
        return whitelistData;
    }

    public static PlayerData getPlayerData(String username) {
        return playerDataCache.computeIfAbsent(username.toLowerCase(), k -> PlayerData.load(username));
    }

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
        HeosLogger.info(Heosmod.MOD_NAME + " authentication system started!");
        HeosLogger.info("Mod id: " + Heosmod.MOD_ID);
        HeosLogger.info("Mod version: " + Heosmod.MOD_VERSION);
        HeosLogger.info("Mod author: " + Heosmod.MOD_AUTHOR);
        HeosLogger.info("License: " + Heosmod.MOD_LICENSE);
        HeosLogger.info("Minecraft version: " + server.getVersion());
        HeosLogger.info("Game directory: " + gameDirectory);
        HeosLogger.info("Online mode: " + server.isOnlineMode());

        config = HeosConfig.load();
        banData = BanData.load();
        whitelistData = WhitelistData.load();

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
