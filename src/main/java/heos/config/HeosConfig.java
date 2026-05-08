package heos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heos.Heos;
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
    public int migrationBanSeconds = 60; // seconds
    public int minPasswordLength = 4;
    public int maxPasswordLength = 32;

    // Whitelist settings
    public boolean enableWhitelist = false;
    public String whitelistKickMessage = "§c服务器白名单已开启\n§e请先注册账号后再加入";

    // Session limit settings
    public int maxConcurrentSessionsPerIp = -1; // -1 to disable
    public String sessionLimitKickMessage = "§c同一IP在线会话数量已达上限";

    // Logging settings
    public boolean enableDebugLogging = true;
    public boolean logPlayerLogin = true;
    public boolean logPlayerRegister = true;
    public boolean logPasswordChange = true;
    public boolean logAdminActions = true;

    // Ban settings
    public boolean enableCustomBan = false;
    public String banMessageFormat = "§c你已被封禁\n§e原因: %reason%\n§e到期时间: %expiry%";
    public String banIpMessageFormat = "§c你的IP已被封禁\n§e原因: %reason%\n§e到期时间: %expiry%";

    public static HeosConfig load() {
        try {
            File configFile = new File(Heos.gameDirectory.toFile(), CONFIG_FILE);

            if (!configFile.exists()) {
                HeosLogger.info("Config file not found, creating default config");
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
                HeosLogger.info("Loaded config from " + CONFIG_FILE);
                return config;
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to load config", e);
            return new HeosConfig();
        }
    }

    public void save() {
        try {
            File configFile = new File(Heos.gameDirectory.toFile(), CONFIG_FILE);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
            HeosLogger.info("Saved config to " + CONFIG_FILE);
        } catch (IOException e) {
            HeosLogger.error("Failed to save config", e);
        }
    }
}
