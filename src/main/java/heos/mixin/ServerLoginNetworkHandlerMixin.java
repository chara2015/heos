package heos.mixin;

import com.mojang.authlib.GameProfile;
import heos.Heos;
import heos.integrations.MojangApi;
import heos.storage.BanData;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.LoginFailureTracker;
import heos.utils.Messages;
import heos.utils.TimeParser;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
//? if >= 1.21.2 {
import net.minecraft.network.DisconnectionDetails;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle offline players joining online-mode server
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    //? if >= 1.20.5 {
    public GameProfile authenticatedProfile;
    //?} else {
    /*public GameProfile gameProfile;
    *///?}

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Final
    Connection connection;

    @Shadow
    public abstract String getUserName();

    @Shadow
    public abstract void disconnect(Component reason);

    //? if >= 1.20.5 {
    @Invoker("finishLoginAndWaitForClient")
    abstract void heos$finishLoginAndWaitForClient(GameProfile profile);
    //?}

    @Unique
    private boolean heos$pendingPremiumVerification = false;
    @Unique
    private String heos$loginUsername = "unknown";

    @Inject(method = "handleHello(Lnet/minecraft/network/protocol/login/ServerboundHelloPacket;)V", at = @At("HEAD"), cancellable = true)
    private void checkBan(ServerboundHelloPacket packet, CallbackInfo ci) {
        String username = packet.name();
        heos$loginUsername = username;
        BanData banData = Heos.getBanData();

        String ip = getUserName();
        if (ip.contains("/")) {
            ip = ip.substring(ip.indexOf('/') + 1);
        }
        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(':'));
        }

        if (LoginFailureTracker.isBlocked(username, ip)) {
            heos$disconnectWithoutVanillaLogs(Component.literal(LoginFailureTracker.blockMessage(username, ip)), Component.literal("Heos login failure lock"));
            ci.cancel();
            return;
        }

        BanData.IpBanEntry ipBan = banData.getIpBan(ip);
        if (ipBan != null && Heos.getConfig().enableCustomBan) {
            String message = Messages.banIpMessage(ipBan.reason, TimeParser.formatAbsoluteTime(ipBan.expiryTime));
            ((ServerLoginPacketListenerImpl) (Object) this).disconnect(Component.literal(message));
            ci.cancel();
            return;
        }

        BanData.BanEntry playerBan = banData.getPlayerBan(username, null);
        if (playerBan != null) {
            if (!Heos.getConfig().enableCustomBan && !Messages.isMigrationReason(playerBan.reason)) {
                return;
            }
            String message = Messages.banMessage(playerBan.reason, TimeParser.formatAbsoluteTime(playerBan.expiryTime));
            if (Messages.isMigrationReason(playerBan.reason)) {
                HeosLogger.info(Messages.migrationBanAttemptLog(username));
                heos$disconnectWithoutVanillaLogs(Component.literal(message), Component.literal(Messages.migrationBanLogOnly()));
            } else {
                ((ServerLoginPacketListenerImpl) (Object) this).disconnect(Component.literal(message));
            }
            ci.cancel();
        }
    }

    @Inject(
        method = "handleHello(Lnet/minecraft/network/protocol/login/ServerboundHelloPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"),
        cancellable = true
    )
    private void checkPremiumAccount(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (!server.usesAuthentication()) {
            return;
        }

        String username = packet.name();
        HeosLogger.debug("Checking player: " + username);

        if (!MojangApi.isValidMojangUsername(username)) {
            if (!MojangApi.isAllowedOfflineUsername(username)) {
                HeosLogger.info(Messages.invalidOfflineNameLog() + ": " + username);
                heos$disconnectWithoutVanillaLogs(Component.literal(Messages.offlineNameHint()), Component.literal(Messages.offlineNameLogOnly()));
                ci.cancel();
                return;
            }

            HeosLogger.info("Offline player is using an allowed non-premium username: " + username);
            GameProfile offlineProfile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
            //? if >= 1.20.5 {
            this.authenticatedProfile = offlineProfile;
            heos$finishLoginAndWaitForClient(offlineProfile);
            //?} else {
            /*this.gameProfile = offlineProfile;
            ((ServerLoginPacketListenerImpl) (Object) this).handleAcceptedLogin();
            *///?}
            ci.cancel();
            return;
        }

        PlayerData data = Heos.getPlayerData(username);
        if (data.isOnlineAccount) {
            HeosLogger.debug("Player " + username + " is cached as premium, continuing vanilla auth");
            return;
        }

        MojangApi.LookupResult lookup = MojangApi.lookupAccount(username);

        if (lookup.type == MojangApi.LookupResultType.NOT_FOUND) {
            HeosLogger.info(Messages.invalidOfflineNameLog() + ": " + username);
            heos$disconnectWithoutVanillaLogs(Component.literal(Messages.offlineNameHint()), Component.literal(Messages.offlineNameLogOnly()));
            ci.cancel();
            return;
        }

        if (lookup.type == MojangApi.LookupResultType.ERROR) {
            HeosLogger.warn("Mojang API lookup failed for " + username + ", allowing vanilla online-mode auth");
            return;
        }

        HeosLogger.info("Player " + username + " uses a premium name, deferring to vanilla authentication");
        heos$pendingPremiumVerification = true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaDisconnectLog(Component reason, CallbackInfo ci) {
        if (reason != null) {
            String text = reason.getString();
            if (heos$isWhitelistDisconnect(text)) {
                HeosLogger.info(Messages.whitelistDeniedLog(heos$loginUsername));
                heos$disconnectWithoutVanillaLogs(Component.literal(Messages.whitelistKick()), Component.literal(Messages.whitelistLogOnly()));
                ci.cancel();
                return;
            }
            if (Messages.offlineNameLogOnly().equals(text)
                    || Messages.migrationBanLogOnly().equals(text)
                    || Messages.whitelistLogOnly().equals(text)) {
                ci.cancel();
            }
        }
    }

    //? if >= 1.21.2 {
    @Inject(method = "onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaLostConnectionLog(DisconnectionDetails info, CallbackInfo ci) {
        if (info != null && info.reason() != null) {
            String text = info.reason().getString();
            if (Messages.offlineNameLogOnly().equals(text)
                    || Messages.migrationBanLogOnly().equals(text)
                    || Messages.whitelistLogOnly().equals(text)) {
                ci.cancel();
            }
        }
    }
    //?}

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void rewriteInvalidSessionDisconnect(Component reason, CallbackInfo ci) {
        if (!heos$pendingPremiumVerification || reason == null) {
            return;
        }

        String message = reason.getString();
        if (message == null) {
            return;
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("invalid session")
                || normalized.contains("failed to verify username")) {
            heos$pendingPremiumVerification = false;
            heos$disconnectWithoutVanillaLogs(Component.literal(Messages.offlineNameHint()), Component.literal(Messages.offlineNameLogOnly()));
            ci.cancel();
        }
    }

    @Unique
    private void heos$disconnectWithoutVanillaLogs(Component playerMessage, Component internalReason) {
        connection.send(new ClientboundLoginDisconnectPacket(playerMessage));
        connection.disconnect(internalReason);
    }

    @Unique
    private boolean heos$isWhitelistDisconnect(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("not white-listed")
                || normalized.contains("not whitelisted")
                || normalized.contains("whitelist");
    }
}
