package heos.mixin;

import com.mojang.authlib.GameProfile;
import heos.Heos;
import heos.integrations.MojangApi;
import heos.storage.BanData;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.TimeParser;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.UUID;

/**
 * Mixin to handle offline players joining online-mode server
 * Key idea: Check if player has premium account, if not, allow offline login
 */
@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    
    @Shadow
    public GameProfile profile;
    
    @Shadow
    private ServerLoginNetworkHandler.State state;
    
    @Shadow
    @Final
    MinecraftServer server;
    
    @Shadow
    public abstract String getConnectionInfo();
    
    /**
     * Check for bans before allowing login
     */
    @Inject(
        method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void checkBan(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (!Heos.getConfig().enableCustomBan) {
            return; // Custom ban system disabled
        }
        
        String username = packet.name();
        BanData banData = Heos.getBanData();
        
        // Get IP address
        String ip = getConnectionInfo();
        if (ip.contains("/")) {
            ip = ip.substring(ip.indexOf('/') + 1);
        }
        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(':'));
        }
        
        // Check IP ban first
        BanData.IpBanEntry ipBan = banData.getIpBan(ip);
        if (ipBan != null) {
            String message = Heos.getConfig().banIpMessageFormat
                .replace("%reason%", ipBan.reason)
                .replace("%expiry%", TimeParser.formatAbsoluteTime(ipBan.expiryTime));
            ((ServerLoginNetworkHandler)(Object)this).disconnect(Text.literal(message));
            ci.cancel();
            return;
        }
        
        // Check player ban
        BanData.BanEntry playerBan = banData.getPlayerBan(username, null);
        if (playerBan != null) {
            String message = Heos.getConfig().banMessageFormat
                .replace("%reason%", playerBan.reason)
                .replace("%expiry%", TimeParser.formatAbsoluteTime(playerBan.expiryTime));
            ((ServerLoginNetworkHandler)(Object)this).disconnect(Text.literal(message));
            ci.cancel();
            return;
        }
    }
    
    /**
     * Intercepts login process to check if player should use offline mode
     * This is called during the login handshake
     */
    @Inject(
        method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"
        ),
        cancellable = true
    )
    private void checkPremiumAccount(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (!server.isOnlineMode()) {
            return; // Server is offline mode, continue normally
        }
        
        String username = packet.name();
        HeosLogger.debug("Checking player: " + username);
        
        // Check if username contains special characters (offline player indicator)
        if (!MojangApi.isValidMojangUsername(username)) {
            HeosLogger.info("Player " + username + " has invalid Mojang username format, treating as offline");
            this.state = ServerLoginNetworkHandler.State.VERIFYING;
            this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(username), username);
            ci.cancel();
            return;
        }
        
        // Check player data cache
        PlayerData data = Heos.getPlayerData(username);
        
        if (data.isOnlineAccount) {
            HeosLogger.debug("Player " + username + " is cached as premium, continuing vanilla auth");
            return; // Continue with vanilla authentication
        }
        
        // Check with Mojang API
        UUID mojangUuid = MojangApi.getUuid(username);
        
        if (mojangUuid == null) {
            // No Mojang account found
            HeosLogger.info("Player " + username + " has no Mojang account");
            
            // Disconnect with helpful message
            Text message = Text.literal(
                "§c无效会话\n\n" +
                "§e离线玩家请在用户名中添加以下符号之一：\n" +
                "§a+ - .\n\n" +
                "§7例如：Player+123, Test.User, User-Name\n" +
                "§7否则您将走正版验证流程\n\n" +
                "§cInvalid Session\n\n" +
                "§eOffline players please add one of these symbols to your username:\n" +
                "§a+ - .\n\n" +
                "§7Example: Player+123, Test.User, User-Name\n" +
                "§7Otherwise you will go through premium authentication"
            );
            
            ((ServerLoginNetworkHandler)(Object)this).disconnect(message);
            ci.cancel();
            return;
        }
        
        // Mojang account exists, verify UUID matches
        if (checkUuid(packet.profileId(), mojangUuid)) {
            // Valid premium player
            HeosLogger.info("Player " + username + " verified as premium");
            data.isOnlineAccount = true;
            // Continue with vanilla authentication
        } else {
            // UUID mismatch, treat as offline (cracked client with premium name)
            HeosLogger.warn("Player " + username + " UUID mismatch, treating as offline");
            
            // Disconnect with helpful message
            Text message = Text.literal(
                "§c无效会话\n\n" +
                "§e此用户名已被正版玩家使用\n" +
                "§e离线玩家请在用户名中添加以下符号之一：\n" +
                "§a+ - .\n\n" +
                "§7例如：" + username + "+123, " + username + ".User\n\n" +
                "§cInvalid Session\n\n" +
                "§eThis username is taken by a premium player\n" +
                "§eOffline players please add one of these symbols to your username:\n" +
                "§a+ - .\n\n" +
                "§7Example: " + username + "+123, " + username + ".User"
            );
            
            ((ServerLoginNetworkHandler)(Object)this).disconnect(message);
            ci.cancel();
        }
    }
    
    /**
     * Checks UUID match - overload for UUID (pre-1.20.2)
     */
    @Unique
    private boolean checkUuid(UUID uuid, UUID onlineUuid) {
        return uuid != null && uuid.equals(onlineUuid);
    }
    
    /**
     * Checks UUID match - overload for Optional<UUID> (1.20.2+)
     */
    @Unique
    private boolean checkUuid(Optional<UUID> uuid, UUID onlineUuid) {
        return uuid.isPresent() && uuid.get().equals(onlineUuid);
    }
}
