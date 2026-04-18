package heos.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heos.Heos;
import heos.utils.HeosLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ban data storage
 */
public class BanData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BAN_FILE = "heos_bans.json";
    
    public List<BanEntry> playerBans = new ArrayList<>();
    public List<IpBanEntry> ipBans = new ArrayList<>();
    
    public static class BanEntry {
        public String username;
        public UUID uuid;
        public String reason;
        public long bannedTime;
        public long expiryTime; // -1 for permanent
        public String bannedBy;
        
        public BanEntry(String username, UUID uuid, String reason, long expiryTime, String bannedBy) {
            this.username = username;
            this.uuid = uuid;
            this.reason = reason;
            this.bannedTime = System.currentTimeMillis();
            this.expiryTime = expiryTime;
            this.bannedBy = bannedBy;
        }
        
        public boolean isExpired() {
            return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
        }
        
        public boolean isPermanent() {
            return expiryTime == -1;
        }
    }
    
    public static class IpBanEntry {
        public String ip;
        public String reason;
        public long bannedTime;
        public long expiryTime; // -1 for permanent
        public String bannedBy;
        
        public IpBanEntry(String ip, String reason, long expiryTime, String bannedBy) {
            this.ip = ip;
            this.reason = reason;
            this.bannedTime = System.currentTimeMillis();
            this.expiryTime = expiryTime;
            this.bannedBy = bannedBy;
        }
        
        public boolean isExpired() {
            return expiryTime != -1 && System.currentTimeMillis() > expiryTime;
        }
        
        public boolean isPermanent() {
            return expiryTime == -1;
        }
    }
    
    /**
     * Loads ban data from disk
     */
    public static BanData load() {
        try {
            File banFile = new File(Heos.gameDirectory.toFile(), BAN_FILE);
            
            if (!banFile.exists()) {
                return new BanData();
            }
            
            try (FileReader reader = new FileReader(banFile)) {
                BanData data = GSON.fromJson(reader, BanData.class);
                if (data == null) {
                    return new BanData();
                }
                
                // Remove expired bans
                data.playerBans.removeIf(BanEntry::isExpired);
                data.ipBans.removeIf(IpBanEntry::isExpired);
                
                HeosLogger.info("Loaded " + data.playerBans.size() + " player bans and " + data.ipBans.size() + " IP bans");
                return data;
            }
        } catch (IOException e) {
            HeosLogger.error("Failed to load ban data", e);
            return new BanData();
        }
    }
    
    /**
     * Saves ban data to disk
     */
    public void save() {
        try {
            File banFile = new File(Heos.gameDirectory.toFile(), BAN_FILE);
            try (FileWriter writer = new FileWriter(banFile)) {
                GSON.toJson(this, writer);
            }
            HeosLogger.debug("Saved ban data");
        } catch (IOException e) {
            HeosLogger.error("Failed to save ban data", e);
        }
    }
    
    /**
     * Checks if player is banned
     */
    public BanEntry getPlayerBan(String username, UUID uuid) {
        playerBans.removeIf(BanEntry::isExpired);
        
        for (BanEntry ban : playerBans) {
            if (ban.username.equalsIgnoreCase(username) || (uuid != null && uuid.equals(ban.uuid))) {
                if (!ban.isExpired()) {
                    return ban;
                }
            }
        }
        return null;
    }
    
    /**
     * Checks if IP is banned
     */
    public IpBanEntry getIpBan(String ip) {
        ipBans.removeIf(IpBanEntry::isExpired);
        
        for (IpBanEntry ban : ipBans) {
            if (ban.ip.equals(ip)) {
                if (!ban.isExpired()) {
                    return ban;
                }
            }
        }
        return null;
    }
    
    /**
     * Adds a player ban
     */
    public void addPlayerBan(String username, UUID uuid, String reason, long expiryTime, String bannedBy) {
        // Remove existing ban
        playerBans.removeIf(ban -> ban.username.equalsIgnoreCase(username) || (uuid != null && uuid.equals(ban.uuid)));
        
        playerBans.add(new BanEntry(username, uuid, reason, expiryTime, bannedBy));
        save();
    }
    
    /**
     * Adds an IP ban
     */
    public void addIpBan(String ip, String reason, long expiryTime, String bannedBy) {
        // Remove existing ban
        ipBans.removeIf(ban -> ban.ip.equals(ip));
        
        ipBans.add(new IpBanEntry(ip, reason, expiryTime, bannedBy));
        save();
    }
    
    /**
     * Removes a player ban
     */
    public boolean removePlayerBan(String username) {
        boolean removed = playerBans.removeIf(ban -> ban.username.equalsIgnoreCase(username));
        if (removed) {
            save();
        }
        return removed;
    }
    
    /**
     * Removes an IP ban
     */
    public boolean removeIpBan(String ip) {
        boolean removed = ipBans.removeIf(ban -> ban.ip.equals(ip));
        if (removed) {
            save();
        }
        return removed;
    }
}



