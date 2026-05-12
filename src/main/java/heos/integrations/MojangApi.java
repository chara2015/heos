package heos.integrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import heos.utils.HeosLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Mojang API integration for checking premium accounts
 */
public class MojangApi {
    private static final String MOJANG_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

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
        if (!isValidMojangUsername(username)) {
            return new LookupResult(LookupResultType.NOT_FOUND, null);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(MOJANG_API + username))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return parseProfile(username, response.body());
            }
            if (status == HttpURLConnection.HTTP_NO_CONTENT || status == HttpURLConnection.HTTP_NOT_FOUND) {
                return new LookupResult(LookupResultType.NOT_FOUND, null);
            }

            HeosLogger.warn("Unexpected Mojang API status for " + username + ": " + status);
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            HeosLogger.error("Failed to check Mojang account for " + username, e);
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (RuntimeException e) {
            HeosLogger.error("Failed to parse Mojang account response for " + username, e);
            return new LookupResult(LookupResultType.ERROR, null);
        }
    }

    private static LookupResult parseProfile(String username, String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        String compactUuid = json.has("id") ? json.get("id").getAsString() : null;
        if (compactUuid == null || !compactUuid.matches("^[0-9a-fA-F]{32}$")) {
            HeosLogger.warn("Mojang API response for " + username + " contained an invalid id: " + responseBody);
            return new LookupResult(LookupResultType.ERROR, null);
        }
        return new LookupResult(LookupResultType.FOUND, expandUuid(compactUuid));
    }

    private static UUID expandUuid(String compactUuid) {
        String dashedUuid = compactUuid.substring(0, 8)
            + "-" + compactUuid.substring(8, 12)
            + "-" + compactUuid.substring(12, 16)
            + "-" + compactUuid.substring(16, 20)
            + "-" + compactUuid.substring(20);
        return UUID.fromString(dashedUuid);
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
