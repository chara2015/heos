package heos.mixin;

import com.mojang.authlib.GameProfile;
import heos.Heos;
import heos.integrations.MojangApi;
import heos.storage.BanData;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import heos.utils.TimeParser;
import net.minecraft.network.ClientConnection;
//? if >= 1.21.2 {
import net.minecraft.network.DisconnectionInfo;
//?}
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle offline players joining online-mode server
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
    @Final
    ClientConnection connection;

    @Shadow
    public abstract String getConnectionInfo();

    @Shadow
    public abstract void disconnect(Text reason);

    @Unique
    private boolean heos$pendingPremiumVerification = false;

    @Inject(method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    private void checkBan(LoginHelloC2SPacket packet, CallbackInfo ci) {
        String username = packet.name();
        BanData banData = Heos.getBanData();

        String ip = getConnectionInfo();
        if (ip.contains("/")) {
            ip = ip.substring(ip.indexOf('/') + 1);
        }
        if (ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(':'));
        }

        BanData.IpBanEntry ipBan = banData.getIpBan(ip);
        if (ipBan != null && Heos.getConfig().enableCustomBan) {
            String message = Heos.getConfig().banIpMessageFormat
                .replace("%reason%", ipBan.reason)
                .replace("%expiry%", TimeParser.formatAbsoluteTime(ipBan.expiryTime));
            ((ServerLoginNetworkHandler) (Object) this).disconnect(Text.literal(message));
            ci.cancel();
            return;
        }

        BanData.BanEntry playerBan = banData.getPlayerBan(username, null);
        if (playerBan != null) {
            if (!Heos.getConfig().enableCustomBan && !Messages.isMigrationReason(playerBan.reason)) {
                return;
            }
            String message = Heos.getConfig().banMessageFormat
                .replace("%reason%", playerBan.reason)
                .replace("%expiry%", TimeParser.formatAbsoluteTime(playerBan.expiryTime));
            if (Messages.isMigrationReason(playerBan.reason)) {
                HeosLogger.info(Messages.migrationBanAttemptLog(username));
                heos$disconnectWithoutVanillaLogs(Text.literal(message), Text.literal(Messages.migrationBanLogOnly()));
            } else {
                ((ServerLoginNetworkHandler) (Object) this).disconnect(Text.literal(message));
            }
            ci.cancel();
        }
    }

    @Inject(
        method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"),
        cancellable = true
    )
    private void checkPremiumAccount(LoginHelloC2SPacket packet, CallbackInfo ci) {
        if (!server.isOnlineMode()) {
            return;
        }

        String username = packet.name();
        HeosLogger.debug("Checking player: " + username);

        if (!MojangApi.isValidMojangUsername(username)) {
            HeosLogger.info(Messages.invalidOfflineNameLog() + ": " + username);
            //? if >= 1.20.2 {
            this.state = ServerLoginNetworkHandler.State.VERIFYING;
            //?} else {
            /*this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
            *///?}
            this.profile = new GameProfile(Uuids.getOfflinePlayerUuid(username), username);
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
            heos$disconnectWithoutVanillaLogs(Text.literal(Messages.offlineNameHint()), Text.literal(Messages.offlineNameLogOnly()));
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

    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaDisconnectLog(Text reason, CallbackInfo ci) {
        if (reason != null) {
            String text = reason.getString();
            if (Messages.offlineNameLogOnly().equals(text) || Messages.migrationBanLogOnly().equals(text)) {
                ci.cancel();
            }
        }
    }

    //? if >= 1.21.2 {
    @Inject(method = "onDisconnected(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At("HEAD"), cancellable = true)
    private void suppressVanillaLostConnectionLog(DisconnectionInfo info, CallbackInfo ci) {
        if (info != null && info.reason() != null) {
            String text = info.reason().getString();
            if (Messages.offlineNameLogOnly().equals(text) || Messages.migrationBanLogOnly().equals(text)) {
                ci.cancel();
            }
        }
    }
    //?}

    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void rewriteInvalidSessionDisconnect(Text reason, CallbackInfo ci) {
        if (!heos$pendingPremiumVerification || reason == null) {
            return;
        }

        String message = reason.getString();
        if (message == null) {
            return;
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("invalid session")
                || normalized.contains("failed to verify username")
                || normalized.contains("无效会话")
                || normalized.contains("登录失败")
                || normalized.contains("请尝试重启游戏及启动器")) {
            heos$pendingPremiumVerification = false;
            heos$disconnectWithoutVanillaLogs(Text.literal(Messages.offlineNameHint()), Text.literal(Messages.offlineNameLogOnly()));
            ci.cancel();
        }
    }

    @Unique
    private void heos$disconnectWithoutVanillaLogs(Text playerMessage, Text internalReason) {
        connection.send(new LoginDisconnectS2CPacket(playerMessage));
        connection.disconnect(internalReason);
    }
}
