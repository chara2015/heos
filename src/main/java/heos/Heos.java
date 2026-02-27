package heos;

import net.minecraft.server.MinecraftServer;
import heos.utils.HeosLogger;

import java.nio.file.Path;

/**
 * Main Heos class
 */
public class Heos {
    public static Path gameDirectory;

    static void onStartServer(MinecraftServer server) {
        HeosLogger.info("=================================");
        HeosLogger.info("  _   _ _____    ___  ____  ");
        HeosLogger.info(" | | | | ____|  / _ \\/ ___| ");
        HeosLogger.info(" | |_| |  _|   | | | \\___ \\ ");
        HeosLogger.info(" |  _  | |___  | |_| |___) |");
        HeosLogger.info(" |_| |_|_____|  \\___/|____/ ");
        HeosLogger.info("=================================");
        HeosLogger.info("Heos server started successfully!");
        HeosLogger.info("Minecraft version: " + server.getVersion());
        HeosLogger.info("Game directory: " + gameDirectory);
    }

    static void onStopServer(MinecraftServer server) {
        HeosLogger.info("=================================");
        HeosLogger.info("Shutting down Heos server...");
        HeosLogger.info("Goodbye!");
        HeosLogger.info("=================================");
    }
}