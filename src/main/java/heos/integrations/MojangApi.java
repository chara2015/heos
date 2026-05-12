package heos.integrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import heos.utils.HeosLogger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Mojang API integration for checking premium accounts
 */
public class MojangApi {
    private static final String MOJANG_API = "https://api.mojang.com/users/profiles/minecraft/";

    public enum LookupResultType {
        FOUND,
        NOT_FOUND,
        ERROR
    }

    public static class LookupResult {
        public final LookupResultType type;
        public final UUID uuid;

        public LookupResult(LookupResultType type, UUID uuid) {
            this.type = type;
            this.uuid = uuid;
        }
    }

    /**
     * Gets Mojang account lookup result
     */
    public static LookupResult lookupAccount(String username) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) URI.create(MOJANG_API + username).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int response = connection.getResponseCode();

            if (response == HttpURLConnection.HTTP_OK) {
                String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                connection.disconnect();

                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                if (!json.has("id")) {
                    HeosLogger.warn("Mojang API response for " + username + " did not contain an id: " + responseBody);
                    return new LookupResult(LookupResultType.ERROR, null);
                }
                String uuidString = json.get("id").getAsString();
                if (uuidString == null || !uuidString.matches("^[0-9a-fA-F]{32}$")) {
                    HeosLogger.warn("Mojang API response for " + username + " contained an invalid id: " + responseBody);
                    return new LookupResult(LookupResultType.ERROR, null);
                }
                UUID uuid = UUID.fromString(uuidString.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
                return new LookupResult(LookupResultType.FOUND, uuid);
            }

            if (response == HttpURLConnection.HTTP_NO_CONTENT || response == HttpURLConnection.HTTP_NOT_FOUND) {
                connection.disconnect();
                return new LookupResult(LookupResultType.NOT_FOUND, null);
            }

            connection.disconnect();
            HeosLogger.warn("Unexpected Mojang API status for " + username + ": " + response);
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (IOException e) {
            HeosLogger.error("Failed to check Mojang account for " + username, e);
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (RuntimeException e) {
            HeosLogger.error("Failed to parse Mojang account response for " + username, e);
            return new LookupResult(LookupResultType.ERROR, null);
        }
    }

    /**
     * Checks if username is valid for Mojang account (alphanumeric and underscore only)
     */
    public static boolean isValidMojangUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    /**
     * Checks if username is allowed for offline accounts.
     */
    public static boolean isAllowedOfflineUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_+\\-.]{3,16}$");
    }
}
