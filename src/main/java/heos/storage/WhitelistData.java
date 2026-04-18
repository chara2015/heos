package heos.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heos.Heos;
import heos.utils.HeosLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Whitelist data storage
 */
public class WhitelistData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String WHITELIST_FILE = "heos_whitelist.json";

    public Set<String> usernames = new HashSet<>();

    public boolean isWhitelisted(String username) {
        return usernames.contains(username.toLowerCase());
    }

    public boolean add(String username) {
        boolean added = usernames.add(username.toLowerCase());
        if (added) {
            save();
        }
        return added;
    }

    public boolean remove(String username) {
        boolean removed = usernames.remove(username.toLowerCase());
        if (removed) {
            save();
        }
        return removed;
    }

    public static WhitelistData load() {
        try {
            File file = new File(Heos.gameDirectory.toFile(), WHITELIST_FILE);
            if (!file.exists()) {
                WhitelistData data = new WhitelistData();
                data.save();
                return data;
            }

            try (FileReader reader = new FileReader(file)) {
                WhitelistData data = GSON.fromJson(reader, WhitelistData.class);
                if (data == null) {
                    WhitelistData defaultData = new WhitelistData();
                    defaultData.save();
                    return defaultData;
                }
                return data;
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to load whitelist data", e);
            return new WhitelistData();
        }
    }

    public void save() {
        try {
            File file = new File(Heos.gameDirectory.toFile(), WHITELIST_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to save whitelist data", e);
        }
    }
}
