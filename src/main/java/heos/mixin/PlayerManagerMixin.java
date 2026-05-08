package heos.mixin;

import heos.Heos;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
//? if >= 1.20.2 {
import net.minecraft.server.network.ConnectedClientData;
//?}
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

    //? if >= 1.20.2 {
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        heos$handlePlayerJoin(connection, player);
    }
    //?} else {
    /*@Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        heos$handlePlayerJoin(connection, player);
    }
    *///?}

    private void heos$handlePlayerJoin(ClientConnection connection, ServerPlayerEntity player) {
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

        if (data.isOnlineAccount || (player.server.isOnlineMode() && !player.getUuid().equals(Uuids.getOfflinePlayerUuid(username)))) {
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
            player.server.execute(playerAuth::heos$sendAuthMessage);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayerEntity player, CallbackInfo ci) {
        String username = player.getName().getString();
        HeosLogger.debug("Player " + username + " left the server");
    }
}
