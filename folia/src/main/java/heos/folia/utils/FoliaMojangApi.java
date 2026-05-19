package heos.folia.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Logger;

public class FoliaMojangApi {
    private static final String MOJANG_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private static final Logger LOGGER = Logger.getLogger("Heos");

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

            LOGGER.warning("Unexpected Mojang API status for " + username + ": " + status);
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.severe("Failed to check Mojang account for " + username + ": " + e.getMessage());
            return new LookupResult(LookupResultType.ERROR, null);
        } catch (RuntimeException e) {
            LOGGER.severe("Failed to parse Mojang account response for " + username + ": " + e.getMessage());
            return new LookupResult(LookupResultType.ERROR, null);
        }
    }

    private static LookupResult parseProfile(String username, String responseBody) {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        String compactUuid = json.has("id") ? json.get("id").getAsString() : null;
        if (compactUuid == null || !compactUuid.matches("^[0-9a-fA-F]{32}$")) {
            LOGGER.warning("Mojang API response for " + username + " contained an invalid id: " + responseBody);
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

    public static boolean isValidMojangUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    public static boolean isAllowedOfflineUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_+\\-.]{3,16}$");
    }
}
