package heos.storage;

import java.util.UUID;

/**
 * Player data storage
 */
public class PlayerData {
    public String username;
    public UUID uuid;
    public String password;
    public String lastIp;
    public boolean isOnlineAccount;
    
    public PlayerData(String username) {
        this.username = username;
        this.password = "";
        this.lastIp = "";
        this.isOnlineAccount = false;
    }
    
    public boolean isRegistered() {
        return password != null && !password.isEmpty();
    }
}

