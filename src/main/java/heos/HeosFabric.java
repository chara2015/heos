package heos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import heos.utils.HeosLogger;

/**
 * Fabric mod initializer for Heos
 */
public class HeosFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Heos.gameDirectory = FabricLoader.getInstance().getGameDir();
        
        HeosLogger.info("Initializing Heos mod...");
        HeosLogger.info("Game directory: " + Heos.gameDirectory);
        
        registerEvents();
        
        HeosLogger.info("Heos mod initialized successfully!");
    }

    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(Heos::onStartServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(Heos::onStopServer);
    }
}