package heos.folia.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public final class FoliaMessages {
    private static final Gson GSON = new Gson();
    private static Map<String, String> FALLBACK = Collections.emptyMap();
    private static Plugin plugin;

    private FoliaMessages() {
    }

    public static void init(Plugin instance) {
        plugin = instance;
        FALLBACK = loadLanguage("en_us");
    }

    private static Map<String, String> loadLanguage(String language) {
        try {
            var stream = FoliaMessages.class.getResourceAsStream("/data/heos/lang/" + language.toLowerCase(Locale.ENGLISH) + ".json");
            if (stream == null) {
                return Collections.emptyMap();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                return map == null ? Collections.emptyMap() : map;
            }
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String translate(String key) {
        String language = plugin == null ? "en_us" : plugin.getConfig().getString("language", "en_us");
        Map<String, String> current = loadLanguage(language);
        if (current.containsKey(key)) {
            return current.get(key);
        }
        return FALLBACK.getOrDefault(key, key);
    }

    public static String authPromptLogin() {
        return translate("text.heos.loginInputHint");
    }

    public static String authPromptRegister() {
        return translate("text.heos.registerInputHint");
    }

    public static String offlineNameHint() {
        return translate("text.heos.disallowedUsername");
    }

    public static String invalidOfflineNameLog() {
        return translate("text.heos.disallowedUsername");
    }

    public static String loginTimeout() {
        return translate("text.heos.timeExpired");
    }

    public static String premiumWelcome() {
        return translate("text.heos.onlinePlayerLogin");
    }

    public static String loginInputHint() {
        return translate("text.heos.loginInputHint");
    }

    public static String registerInputHint() {
        return translate("text.heos.registerRequired");
    }

    public static String alreadyLoggedIn() {
        return translate("text.heos.alreadyAuthenticated");
    }

    public static String premiumNoLogin() {
        return translate("text.heos.onlinePlayerLogin");
    }

    public static String premiumNoRegister() {
        return translate("text.heos.onlinePlayerLogin");
    }

    public static String notRegistered() {
        return translate("text.heos.userNotRegistered");
    }

    public static String alreadyRegistered() {
        return translate("text.heos.alreadyRegistered");
    }

    public static String loginSuccess() {
        return translate("text.heos.successfullyAuthenticated");
    }

    public static String wrongPassword() {
        return translate("text.heos.wrongPassword");
    }

    public static String passwordTooShort() {
        return translate("text.heos.minPasswordChars");
    }

    public static String passwordTooLong() {
        return translate("text.heos.maxPasswordChars");
    }

    public static String passwordMismatch() {
        return translate("text.heos.matchPassword");
    }

    public static String registerFailed() {
        return translate("text.heos.registerRequired");
    }

    public static String registerSuccess() {
        return translate("text.heos.registerSuccess");
    }

    public static String keepPasswordSafe() {
        return translate("text.heos.successfullyAuthenticated");
    }

    public static boolean isMigrationReason(String reason) {
        if (reason == null) {
            return false;
        }
        String normalized = reason.toLowerCase();
        return normalized.contains("data migration in progress")
                || normalized.contains("migration in progress");
    }

    public static String whitelistKick() {
        return translate("text.heos.whitelistKick");
    }

    public static String whitelistDeniedLog(String username) {
        return translate("text.heos.whitelistDeniedLog").formatted(username);
    }

    public static String banMessage(String reason, String expiry) {
        return translate("text.heos.banMessage").formatted(reason, expiry);
    }

    public static String banIpMessage(String reason, String expiry) {
        return translate("text.heos.banIpMessage").formatted(reason, expiry);
    }

    public static String migrationBanAttemptLog(String username) {
        return translate("text.heos.playerAlreadyOnline").formatted(username);
    }

    public static String updateSuppressionCrash(String detail) {
        return translate("text.heos.updateSuppressionCrash").formatted(detail);
    }

    public static String unknownPosition() {
        return translate("text.heos.unknownPosition");
    }
}
