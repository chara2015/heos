package heos.integrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import heos.utils.HeosLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Mojang API integration for checking premium accounts
 */
public class MojangApi {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String MOJANG_API = "https://api.mojang.com/users/profiles/minecraft/";
    
    /**
     * Gets UUID of a premium Mojang account
     * @param username Player username
     * @return UUID if account exists, null otherwise
     */
    public static UUID getUuid(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MOJANG_API + username))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String uuidString = json.get("id").getAsString();
                
                // Convert UUID string to proper format
                return UUID.fromString(
                    uuidString.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                    )
                );
            }
            
            return null;
        } catch (IOException | InterruptedException e) {
            HeosLogger.error("Failed to check Mojang account for " + username, e);
            return null;
        }
    }
    
    /**
     * Checks if username is valid for Mojang account (alphanumeric and underscore only)
     */
    public static boolean isValidMojangUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9_]{3,16}$");
    }
}
