package heos.mixin;

import heos.event.AuthEventHandler;
import heos.interfaces.PlayerAuth;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts packets from unauthenticated players
 * Note: Disabled for 1.21.4+ due to handlePacket method removal
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    
    @Shadow
    public ServerPlayerEntity player;
    
    /**
     * Intercepts all incoming packets
     * Disabled: handlePacket method doesn't exist in 1.21.4+
     */
    // @Inject(
    //     method = "handlePacket",
    //     at = @At("HEAD"),
    //     cancellable = true
    // )
    // private void onPacketReceived(Packet<?> packet, CallbackInfo ci) {
    //     PlayerAuth playerAuth = (PlayerAuth) player;
    //     
    //     // If player is authenticated, allow all packets
    //     if (playerAuth.heos$isAuthenticated()) {
    //         return;
    //     }
    //     
    //     // Check if packet is allowed for unauthenticated players
    //     if (!AuthEventHandler.isAllowedPacket(packet)) {
    //         ci.cancel();
    //     }
    // }
}
