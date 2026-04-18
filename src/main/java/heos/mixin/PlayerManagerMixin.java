package heos.mixin;

import heos.Heos;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles player join and leave events
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        String username = player.getName().getString();

        PlayerData data = Heos.getPlayerData(username);
        playerAuth.heos$setPlayerData(data);
        playerAuth.heos$setIpAddress(connection);

        if (player.getClass() != ServerPlayerEntity.class) {
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(true);
            HeosLogger.info("Fake/mod player " + username + " joined, authentication skipped");
            return;
        }

        if (data.isOnlineAccount || (player.getServer().isOnlineMode() && !player.getUuid().equals(Uuids.getOfflinePlayerUuid(username)))) {
            data.isOnlineAccount = true;
            data.uuid = player.getUuid();
            data.save();
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(true);
            playerAuth.heos$setAuthenticated(true);

            HeosLogger.info("Premium player " + username + " joined, authentication skipped");
            player.sendMessage(Text.literal(Messages.premiumWelcome()), false);
        } else {
            playerAuth.heos$setCanSkipAuth(false);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(false);
            HeosLogger.info("Offline player " + username + " joined, authentication required");
            playerAuth.heos$sendAuthMessage();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity player, CallbackInfo ci) {
        String username = player.getName().getString();
        HeosLogger.debug("Player " + username + " left the server");
    }
}
