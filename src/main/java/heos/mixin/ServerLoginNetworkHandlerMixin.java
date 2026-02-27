package heos.mixin;

import com.mojang.authlib.GameProfile;
import heos.Heos;
import heos.integrations.MojangApi;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
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
    
    @Final
    @Shadow
    MinecraftServer server;
    
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
            allowOfflineLogin(username);
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
        UUID clientUuid = packet.profileId().orElse(null);
        
        if (clientUuid != null && clientUuid.equals(mojangUuid)) {
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
     * Allows player to login in offline mode
     */
    private void allowOfflineLogin(String username) {
        this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(username), username);
        this.state = ServerLoginNetworkHandler.State.VERIFYING;
    }
}
