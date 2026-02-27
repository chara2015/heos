package heos.mixin;

import heos.Heos;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles player join and leave events
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    
    /**
     * Called when player joins the server
     */
    @Inject(
        method = "onPlayerConnect",
        at = @At("TAIL")
    )
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        String username = player.getName().getString();
        
        // Get or create player data
        PlayerData data = Heos.getPlayerData(username);
        playerAuth.heos$setPlayerData(data);
        playerAuth.heos$setIpAddress(connection);
        
        // Check if player can skip authentication
        if (data.isOnlineAccount) {
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(true);
            playerAuth.heos$setAuthenticated(true);
            
            HeosLogger.info("Premium player " + username + " joined, authentication skipped");
            player.sendMessage(Text.literal("§a欢迎回来，正版玩家！"), false);
        } else {
            playerAuth.heos$setCanSkipAuth(false);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(false);
            
            HeosLogger.info("Offline player " + username + " joined, authentication required");
            
            // Send authentication message
            if (data.isRegistered()) {
                player.sendMessage(Text.literal("§e================================="), false);
                player.sendMessage(Text.literal("§e请使用 /login <密码> 登录"), false);
                player.sendMessage(Text.literal("§e================================="), false);
            } else {
                player.sendMessage(Text.literal("§e================================="), false);
                player.sendMessage(Text.literal("§e请使用 /register <密码> <确认密码> 注册"), false);
                player.sendMessage(Text.literal("§e================================="), false);
            }
        }
    }
    
    /**
     * Called when player leaves the server
     */
    @Inject(
        method = "remove",
        at = @At("HEAD")
    )
    private void onPlayerLeave(ServerPlayerEntity player, CallbackInfo ci) {
        String username = player.getName().getString();
        HeosLogger.debug("Player " + username + " left the server");
    }
}
