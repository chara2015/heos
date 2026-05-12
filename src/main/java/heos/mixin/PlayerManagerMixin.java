package heos.mixin;

import heos.Heos;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
//? if >= 1.20.5 {
import net.minecraft.server.network.CommonListenerCookie;
//?}
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles player join and leave events
 */
@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

    //? if >= 1.20.5 {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerJoin(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        heos$handlePlayerJoin(connection, player);
    }
    //?} else {
    /*@Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerJoin(Connection connection, ServerPlayer player, CallbackInfo ci) {
        heos$handlePlayerJoin(connection, player);
    }
    *///?}

    private void heos$handlePlayerJoin(Connection connection, ServerPlayer player) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        String username = player.getName().getString();

        PlayerData data = Heos.getPlayerData(username);
        playerAuth.heos$setPlayerData(data);
        playerAuth.heos$setIpAddress(connection);
        if (heos$exceedsIpSessionLimit(player)) {
            player.connection.disconnect(Component.literal(Heos.getConfig().sessionLimitKickMessage));
            return;
        }

        boolean fakePlayer = player.getClass() != ServerPlayer.class;
        if (fakePlayer) {
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(true);
            HeosLogger.info("Fake/mod player " + username + " joined, authentication skipped");
            return;
        }

        if (!Heos.getConfig().enableAuthentication) {
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(true);
            HeosLogger.info("Authentication disabled, player " + username + " joined without auth");
            return;
        }

        boolean currentSessionUsesMojangAccount = player.level().getServer().usesAuthentication()
                && !player.getUUID().equals(UUIDUtil.createOfflinePlayerUUID(username));
        if (currentSessionUsesMojangAccount) {
            data.isOnlineAccount = true;
            data.uuid = player.getUUID();
            data.save();
            playerAuth.heos$setCanSkipAuth(true);
            playerAuth.heos$setUsingMojangAccount(true);
            playerAuth.heos$setAuthenticated(true);

            HeosLogger.info("Premium player " + username + " joined, authentication skipped");
            player.sendSystemMessage(Component.literal(Messages.premiumWelcome()), false);
        } else {
            if (data.isOnlineAccount || data.uuid == null || !data.uuid.equals(player.getUUID())) {
                data.isOnlineAccount = false;
                data.uuid = player.getUUID();
                data.save();
            }
            playerAuth.heos$setCanSkipAuth(false);
            playerAuth.heos$setUsingMojangAccount(false);
            playerAuth.heos$setAuthenticated(false);
            HeosLogger.info("Offline player " + username + " joined, authentication required");
            player.level().getServer().execute(playerAuth::heos$sendAuthMessage);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayer player, CallbackInfo ci) {
        String username = player.getName().getString();
        ((PlayerAuth) player).heos$stopTpsDisplay();
        HeosLogger.debug("Player " + username + " left the server");
    }

    private boolean heos$exceedsIpSessionLimit(ServerPlayer player) {
        int maxSessions = Heos.getConfig().maxConcurrentSessionsPerIp;
        if (maxSessions <= 0) {
            return false;
        }
        String ip = ((PlayerAuth) player).heos$getIpAddress();
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        int sessions = 0;
        for (ServerPlayer onlinePlayer : player.level().getServer().getPlayerList().getPlayers()) {
            if (ip.equals(((PlayerAuth) onlinePlayer).heos$getIpAddress())) {
                sessions++;
            }
        }
        return sessions > maxSessions;
    }
}
