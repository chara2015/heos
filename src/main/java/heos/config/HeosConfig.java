package heos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import heos.Heos;
import heos.storage.StoragePaths;
import heos.utils.HeosLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Heos configuration
 */
public class HeosConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "heos_config.json";

    // Authentication settings
    public boolean enableAuthentication = true;
    public String language = "zh_cn";
    public int loginTimeout = 60; // seconds
    public int migrationBanSeconds = 30; // seconds
    public int minPasswordLength = 4;
    public int maxPasswordLength = 32;

    // Whitelist settings
    public boolean enableWhitelist = false;
    public String whitelistKickMessage = "Server whitelist is enabled\nPlease register an account before joining";

    // Session limit settings
    public int maxConcurrentSessionsPerIp = -1; // -1 to disable
    public String sessionLimitKickMessage = "The online session limit for this IP has been reached";

    // Login failure protection
    public boolean enableUsernameLoginFailureLock = true;
    public int usernameLoginFailureLimit = 5;
    public int usernameLoginFailureLockSeconds = 30;
    public boolean enableIpLoginFailureLock = false;
    public int ipLoginFailureLimit = 10;
    public int ipLoginFailureLockSeconds = 30;

    // Logging settings
    public boolean enableDebugLogging = true;
    public boolean logPlayerLogin = true;
    public boolean logPlayerRegister = true;
    public boolean logPasswordChange = true;
    public boolean logAdminActions = true;
    public boolean enableAutoLogTps = true;
    public int autoLogTpsDelayTicks = 20;
    @SerializedName("\u65e5\u5fd7\u8fc7\u6ee4")
    public boolean enableLogFilter = true;

    // Ban settings
    public boolean enableCustomBan = false;
    public String banMessageFormat = "You have been banned\nReason: %reason%\nExpires: %expiry%";
    public String banIpMessageFormat = "Your IP has been banned\nReason: %reason%\nExpires: %expiry%";

    public static HeosConfig load() {
        File configFile = StoragePaths.file(CONFIG_FILE);
        StoragePaths.ensureRoot();

        if (!configFile.exists()) {
            HeosLogger.info("Config file not found, creating default config at " + configFile.getPath());
            HeosConfig config = new HeosConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(configFile)) {
            HeosConfig config = GSON.fromJson(reader, HeosConfig.class);
            if (config == null) {
                HeosLogger.warn("Failed to parse config, using default");
                HeosConfig defaultConfig = new HeosConfig();
                defaultConfig.save();
                return defaultConfig;
            }
            HeosLogger.info("Loaded config from " + configFile.getPath());
            return config;
        } catch (IOException e) {
            HeosLogger.error("Failed to load config", e);
            HeosConfig config = new HeosConfig();
            config.save();
            return config;
        }
    }

    public static void migrateLegacyConfig() {
        File legacyFile = new File(Heos.gameDirectory.toFile(), CONFIG_FILE);
        File targetFile = StoragePaths.file(CONFIG_FILE);
        if (!legacyFile.exists() || targetFile.exists()) {
            return;
        }
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (legacyFile.renameTo(targetFile)) {
            HeosLogger.info("Migrated config file to " + targetFile.getPath());
        }
    }

    public void save() {
        File configFile = StoragePaths.file(CONFIG_FILE);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            HeosLogger.error("Failed to create config directory: " + parent.getPath());
            return;
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
            HeosLogger.info("Saved config to " + configFile.getPath());
        } catch (IOException e) {
            HeosLogger.error("Failed to save config", e);
        }
    }
}
