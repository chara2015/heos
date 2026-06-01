package heos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import heos.Heos;
import heos.storage.StoragePaths;
import heos.utils.HeosLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * Heos configuration.
 */
public class HeosConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config.yml";
    private static final String LEGACY_JSON_FILE = "heos_config.json";
    private static final String LEGACY_YAML_FILE = "heos_config.yml";
    private static final List<ConfigSection> SECTIONS = createSections();

    // Authentication settings
    public boolean enableAuthentication = false;
    public String language = "auto";
    public boolean allowOfflinePlayers = false;
    public boolean allowMoreOfflineUsernameCharacters = false;
    public boolean allowUnicodeOfflineUsernameCharacters = false;
    public int loginTimeout = 60; // seconds
    public int loginReminderSeconds = 10;
    public boolean enablePlayerDataMigration = false;
    public int migrationBanSeconds = 30; // seconds
    public int minPasswordLength = 4;
    public int maxPasswordLength = 32;

    // Whitelist settings
    public boolean enableWhitelist = false;

    // Session limit settings
    public int maxConcurrentSessionsPerIp = -1; // -1 to disable
    public String sessionLimitKickMessage = "The online session limit for this IP has been reached";

    // Login failure protection
    public boolean enableUsernameLoginFailureLock = false;
    public int usernameLoginFailureLimit = 5;
    public int usernameLoginFailureLockSeconds = 30;
    public boolean enableIpLoginFailureLock = false;
    public int ipLoginFailureLimit = 10;
    public int ipLoginFailureLockSeconds = 30;

    // Display and integration settings
    public boolean enableAutoLogTps = false;
    public int autoLogTpsDelayTicks = 20;
    @SerializedName(value = "enableLogFilter", alternate = {"日志过滤"})
    public boolean enableLogFilter = false;
    public boolean enableRecipeViewerSync = false;
    public boolean enableGhostPearlFix = false;

    // Ban settings
    public boolean enableCustomBan = false;

    public static HeosConfig load() {
        StoragePaths.ensureRoot();
        File configFile = StoragePaths.file(CONFIG_FILE);
        migrateLegacyYaml(configFile);

        if (!configFile.exists()) {
            HeosConfig migrated = loadLegacyJson();
            if (migrated != null) {
                migrated.save();
                deleteLegacyJson();
                HeosLogger.debug("Migrated legacy JSON config to " + configFile.getPath());
                return migrated;
            }

            HeosLogger.debug("Config file not found, creating default config at " + configFile.getPath());
            HeosConfig config = new HeosConfig();
            config.save();
            return config;
        }

        HeosConfig config = new HeosConfig();
        boolean changed = false;
        String currentSection = null;
        try (BufferedReader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ParsedLine parsed = readConfigLine(config, line, currentSection);
                currentSection = parsed.currentSection();
                changed |= parsed.changed();
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to load config", e);
            return config;
        }

        if (changed || !hasAllKnownKeys(configFile)) {
            config.save();
        }
        HeosLogger.debug("Loaded config from " + configFile.getPath());
        return config;
    }

    public static void migrateLegacyConfig() {
        StoragePaths.ensureRoot();
        moveIfMissing(new File(Heos.gameDirectory.toFile(), CONFIG_FILE), StoragePaths.file(CONFIG_FILE));
        moveIfMissing(new File(Heos.gameDirectory.toFile(), LEGACY_YAML_FILE), StoragePaths.file(LEGACY_YAML_FILE));
        moveIfMissing(new File(Heos.gameDirectory.toFile(), LEGACY_JSON_FILE), StoragePaths.file(LEGACY_JSON_FILE));
    }

    public void save() {
        File configFile = StoragePaths.file(CONFIG_FILE);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            HeosLogger.error("Failed to create config directory: " + parent.getPath());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
            writeYaml(writer);
            HeosLogger.debug("Saved config to " + configFile.getPath());
        } catch (IOException e) {
            HeosLogger.error("Failed to save config", e);
        }
    }

    private static void migrateLegacyYaml(File configFile) {
        File legacyFile = StoragePaths.file(LEGACY_YAML_FILE);
        if (!configFile.exists() && legacyFile.exists()) {
            moveIfMissing(legacyFile, configFile);
        }
    }

    private static HeosConfig loadLegacyJson() {
        File legacyFile = StoragePaths.file(LEGACY_JSON_FILE);
        if (!legacyFile.exists()) {
            return null;
        }
        try (FileReader reader = new FileReader(legacyFile, StandardCharsets.UTF_8)) {
            HeosConfig config = GSON.fromJson(JsonParser.parseReader(reader), HeosConfig.class);
            return config == null ? new HeosConfig() : config;
        } catch (IOException | RuntimeException e) {
            HeosLogger.error("Failed to read legacy JSON config", e);
            return null;
        }
    }

    private static void deleteLegacyJson() {
        try {
            Files.deleteIfExists(StoragePaths.file(LEGACY_JSON_FILE).toPath());
        } catch (IOException e) {
            HeosLogger.warn("Failed to remove migrated legacy JSON config");
        }
    }

    private static void moveIfMissing(File source, File target) {
        if (!source.exists() || target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveException) {
            try {
                Files.move(source.toPath(), target.toPath());
            } catch (IOException moveException) {
                HeosLogger.error("Failed to migrate config file " + source.getPath(), moveException);
            }
        }
    }

    private static ParsedLine readConfigLine(HeosConfig config, String line, String currentSection) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return new ParsedLine(false, currentSection);
        }

        int separator = trimmed.indexOf(':');
        if (separator <= 0) {
            return new ParsedLine(false, currentSection);
        }

        String key = trimmed.substring(0, separator).trim();
        String value = stripInlineComment(trimmed.substring(separator + 1).trim());
        if (value.isEmpty()) {
            return new ParsedLine(false, key);
        }

        int indent = line.indexOf(trimmed);
        if (indent > 0 && currentSection != null) {
            key = currentSection + "." + key;
        } else if (indent == 0) {
            currentSection = null;
        }

        Field field = fieldByConfigKey(key);
        if (field == null) {
            return new ParsedLine(true, currentSection);
        }

        try {
            if (field.getType() == boolean.class) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("Expected boolean");
                }
                field.setBoolean(config, Boolean.parseBoolean(value));
            } else if (field.getType() == int.class) {
                field.setInt(config, Integer.parseInt(value));
            } else if (field.getType() == String.class) {
                field.set(config, unquote(value));
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            HeosLogger.warn("Invalid config value for " + key + ", keeping default");
            return new ParsedLine(true, currentSection);
        }
        return new ParsedLine(false, currentSection);
    }

    private static boolean hasAllKnownKeys(File file) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            for (ConfigSection section : SECTIONS) {
                for (ConfigEntry entry : section.entries()) {
                    if (!text.contains(entry.yamlKey() + ":")) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeYaml(BufferedWriter writer) throws IOException {
        writer.write("# config.yml - Heos Fabric 配置");
        writer.newLine();
        writer.write("# 源代码链接: https://github.com/chara2015/heos");
        writer.newLine();
        writer.write("# 开源协议: MIT");
        writer.newLine();
        writer.write("# 修改后重启服务器生效。");
        writer.newLine();
        writer.write("# 数据库与密钥保存在当前 Heos 目录的 data 文件夹中。");
        writer.newLine();

        for (ConfigSection section : SECTIONS) {
            writer.newLine();
            writer.write(section.key() + ":");
            writer.newLine();
            for (ConfigEntry entry : section.entries()) {
                for (String commentLine : entry.comment().split("\\n")) {
                    writer.write("    # " + commentLine);
                    writer.newLine();
                }
                writer.write("    " + entry.yamlKey() + ": " + yamlValue(entry.fieldName()));
                writer.newLine();
            }
        }
    }

    private String yamlValue(String fieldName) {
        Field field = fieldByName(fieldName);
        if (field == null) {
            return "";
        }
        try {
            Object value = field.get(this);
            if (value instanceof String text) {
                return quoteIfNeeded(text);
            }
            return String.valueOf(value);
        } catch (IllegalAccessException e) {
            return "";
        }
    }

    private static Field fieldByConfigKey(String key) {
        for (ConfigSection section : SECTIONS) {
            for (ConfigEntry entry : section.entries()) {
                if (entry.path(section).equals(key) || entry.fieldName().equals(key)) {
                    return fieldByName(entry.fieldName());
                }
            }
        }
        return fieldByName(key);
    }

    private static Field fieldByName(String key) {
        for (Field field : HeosConfig.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getName().equals(key)) {
                return field;
            }
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName != null && serializedName.value().equals(key)) {
                return field;
            }
        }
        return null;
    }

    private static String stripInlineComment(String value) {
        boolean quoted = false;
        char quote = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if ((current == '\'' || current == '"') && (index == 0 || value.charAt(index - 1) != '\\')) {
                if (!quoted) {
                    quoted = true;
                    quote = current;
                } else if (quote == current) {
                    quoted = false;
                }
            }
            if (!quoted && current == '#' && (index == 0 || Character.isWhitespace(value.charAt(index - 1)))) {
                return value.substring(0, index).trim();
            }
        }
        return value;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return value;
    }

    private static String quoteIfNeeded(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        boolean plain = !value.isBlank()
                && value.chars().noneMatch(Character::isISOControl)
                && !value.contains(":")
                && !value.contains("#")
                && !value.startsWith(" ")
                && !value.endsWith(" ")
                && !"true".equals(lower)
                && !"false".equals(lower)
                && !value.matches("-?\\d+");
        return plain ? value : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static List<ConfigSection> createSections() {
        return List.of(
                new ConfigSection("auth", List.of(
                        entry("enabled", "enableAuthentication", "是否启用登录/注册认证。关闭后玩家无需使用 /login 或 /register。"),
                        entry("language", "language", "服务端消息语言。auto 表示根据运行服务器的电脑系统语言自动选择，也可填写 zh_cn 或 en_us 强制指定。\n玩家进入服务器后，提示消息会优先根据玩家客户端游戏语言自动选择。")
                )),
                new ConfigSection("offline", List.of(
                        entry("allow-players", "allowOfflinePlayers", "在线模式服务器中是否允许非正版玩家进入。关闭后只允许通过 Mojang 正版验证的玩家进入。"),
                        entry("allow-more-username-characters", "allowMoreOfflineUsernameCharacters", "是否允许离线玩家名称在正版名称规则外额外使用 +、-、. 这三个字符。"),
                        entry("allow-unicode-username-characters", "allowUnicodeOfflineUsernameCharacters", "是否允许离线玩家名称使用中文、其他语言字母和数字等 Unicode 字符。（开启后也会包括允许 +、-、. 这三个字符，并且所有正版规则外的字符不强制使用）")
                )),
                new ConfigSection("login", List.of(
                        entry("timeout-seconds", "loginTimeout", "玩家进入服务器后必须完成登录/注册的时间限制，单位为秒。"),
                        entry("reminder-seconds", "loginReminderSeconds", "未登录玩家收到登录/注册提醒的间隔，单位为秒。"),
                        entry("min-password-length", "minPasswordLength", "注册密码允许的最短长度。"),
                        entry("max-password-length", "maxPasswordLength", "注册密码允许的最长长度。")
                )),
                new ConfigSection("security", List.of(
                        entry("max-sessions-per-ip", "maxConcurrentSessionsPerIp", "同一个 IP 最多允许同时在线的玩家数量。-1 表示不限制。"),
                        entry("session-limit-kick-message", "sessionLimitKickMessage", "同 IP 在线数量超过 max-sessions-per-ip 时踢出玩家显示的消息。\n这是管理员可自定义文案，不会自动跟随语言文件。"),
                        entry("username-failure-lock", "enableUsernameLoginFailureLock", "是否在同一用户名连续登录失败过多时临时锁定该用户名。"),
                        entry("username-failure-limit", "usernameLoginFailureLimit", "触发用户名临时锁定前允许的连续失败次数。"),
                        entry("username-failure-lock-seconds", "usernameLoginFailureLockSeconds", "用户名触发失败锁定后的持续时间，单位为秒。"),
                        entry("ip-failure-lock", "enableIpLoginFailureLock", "是否在同一 IP 连续登录失败过多时临时锁定该 IP。"),
                        entry("ip-failure-limit", "ipLoginFailureLimit", "触发 IP 临时锁定前允许的连续失败次数。"),
                        entry("ip-failure-lock-seconds", "ipLoginFailureLockSeconds", "IP 触发失败锁定后的持续时间，单位为秒。")
                )),
                new ConfigSection("features", List.of(
                        entry("whitelist", "enableWhitelist", "是否启用 Heos 自带白名单检查。"),
                        entry("custom-ban", "enableCustomBan", "是否启用 Heos 自带封禁系统和封禁命令。"),
                        entry("player-data-migration", "enablePlayerDataMigration", "是否启用离线/正版玩家数据迁移功能。只在需要迁移玩家数据时开启。"),
                        entry("migration-ban-seconds", "migrationBanSeconds", "数据迁移期间临时阻止相关玩家登录的时间，单位为秒。")
                )),
                new ConfigSection("integrations", List.of(
                        entry("tps-footer", "enableAutoLogTps", "是否在玩家列表页脚显示服务器 TPS 信息。"),
                        entry("tps-footer-delay-ticks", "autoLogTpsDelayTicks", "TPS 信息刷新间隔，单位为 tick。20 tick 约等于 1 秒。"),
                        entry("log-filter", "enableLogFilter", "是否启用日志过滤，隐藏登录、注册、改密等敏感命令内容。"),
                        entry("recipe-viewer-sync", "enableRecipeViewerSync", "是否启用 JEI/REI 配方查看器同步。仅在支持的 Minecraft 版本上生效。"),
                        entry("ghost-pearl-fix", "enableGhostPearlFix", "是否启用幽灵珍珠修复，避免末影珍珠重复保存或重复注册。仅 Fabric 端生效。")
                ))
        );
    }

    private static ConfigEntry entry(String yamlKey, String fieldName, String comment) {
        return new ConfigEntry(yamlKey, fieldName, comment);
    }

    private record ParsedLine(boolean changed, String currentSection) {
    }

    private record ConfigSection(String key, List<ConfigEntry> entries) {
    }

    private record ConfigEntry(String yamlKey, String fieldName, String comment) {
        private String path(ConfigSection section) {
            return section.key() + "." + yamlKey;
        }
    }
}
