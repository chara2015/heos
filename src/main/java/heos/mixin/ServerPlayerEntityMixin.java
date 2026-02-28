package heos.mixin;

import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import net.minecraft.block.Blocks;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Implements PlayerAuth interface for ServerPlayerEntity
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends EntityMixin implements PlayerAuth {
    
    @Unique
    private boolean heos$authenticated = false;
    
    @Unique
    private boolean heos$canSkipAuth = false;
    
    @Unique
    private boolean heos$usingMojangAccount = false;
    
    @Unique
    private String heos$ipAddress = "";
    
    @Unique
    private PlayerData heos$playerData = null;
    
    @Unique
    private long heos$kickTimer = 60 * 20; // 60 seconds
    
    @Override
    public void heos$setAuthenticated(boolean authenticated) {
        this.heos$authenticated = authenticated;
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        
        if (authenticated) {
            HeosLogger.debug("Player authenticated: " + player.getName().getString());
            heos$kickTimer = 60 * 20; // Reset timer
            
            // Update portal blocks if player was in portal
            BlockPos pos = player.getBlockPos();
            player.getServerWorld().updateListeners(pos, player.getServerWorld().getBlockState(pos), player.getServerWorld().getBlockState(pos), 3);
            player.getServerWorld().updateListeners(pos.up(), player.getServerWorld().getBlockState(pos.up()), player.getServerWorld().getBlockState(pos.up()), 3);
        }
    }
    
    @Override
    public boolean heos$isAuthenticated() {
        return this.heos$authenticated;
    }
    
    @Override
    public boolean heos$canSkipAuth() {
        return this.heos$canSkipAuth;
    }
    
    @Override
    public void heos$setCanSkipAuth(boolean canSkip) {
        this.heos$canSkipAuth = canSkip;
    }
    
    @Override
    public boolean heos$isUsingMojangAccount() {
        return this.heos$usingMojangAccount;
    }
    
    @Override
    public void heos$setUsingMojangAccount(boolean usingMojang) {
        this.heos$usingMojangAccount = usingMojang;
    }
    
    @Override
    public String heos$getIpAddress() {
        return this.heos$ipAddress;
    }
    
    @Override
    public void heos$setIpAddress(ClientConnection connection) {
        String address = connection.getAddress().toString();
        if (address.contains("/")) {
            address = address.substring(address.indexOf('/') + 1);
        }
        if (address.contains(":")) {
            address = address.substring(0, address.indexOf(':'));
        }
        this.heos$ipAddress = address;
    }
    
    @Override
    public void heos$setIpAddress(String ipAddress) {
        this.heos$ipAddress = ipAddress;
    }
    
    @Override
    public PlayerData heos$getPlayerData() {
        return this.heos$playerData;
    }
    
    @Override
    public void heos$setPlayerData(PlayerData data) {
        this.heos$playerData = data;
    }
    
    @Override
    public void heos$sendAuthMessage() {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (heos$playerData != null && heos$playerData.isRegistered()) {
            player.sendMessage(Text.literal("§e请使用 /login <密码> 登录"), false);
        } else {
            player.sendMessage(Text.literal("§e请使用 /register <密码> <确认密码> 注册"), false);
        }
    }
    
    /**
     * Makes unauthenticated players invisible to mobs
     */
    @Override
    public boolean heos$isInvisible(boolean original) {
        return original || !heos$authenticated;
    }
    
    /**
     * Makes unauthenticated players invulnerable to damage
     */
    @Override
    public boolean heos$isInvulnerable(boolean original) {
        return original || !heos$authenticated;
    }
    
    /**
     * Player tick - handles kick timer and portal rescue
     */
    @Inject(method = "playerTick()V", at = @At("HEAD"), cancellable = true)
    private void onPlayerTick(CallbackInfo ci) {
        if (!heos$authenticated) {
            ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
            
            // Kick timer countdown
            if (heos$kickTimer <= 0 && player.networkHandler.isConnectionOpen()) {
                player.networkHandler.disconnect(Text.literal("§c登录超时！请在60秒内完成登录"));
            } else {
                // Send auth message every 10 seconds
                if (heos$kickTimer % 200 == 0) {
                    heos$sendAuthMessage();
                }
                --heos$kickTimer;
            }
            
            // Portal rescue - prevent player from being stuck in nether portal
            BlockPos pos = player.getBlockPos();
            if (player.getServerWorld().getBlockState(pos).getBlock().equals(Blocks.NETHER_PORTAL) 
                    || player.getServerWorld().getBlockState(pos.up()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                
                // Teleport player slightly to avoid portal
                player.teleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5, false);
                
                // Fake portal blocks as air on client side
                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);
                
                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);
            }
            
            ci.cancel(); // Don't run normal player tick
        }
    }
    
    /**
     * Prevent item dropping for unauthenticated players
     * Note: Disabled for 1.21.4+ due to method signature change
     */
    // @Inject(method = "dropSelectedItem(Z)V", at = @At("HEAD"), cancellable = true)
    // private void onDropItem(boolean entireStack, CallbackInfo ci) {
    //     if (!heos$authenticated) {
    //         heos$sendAuthMessage();
    //         ci.cancel();
    //     }
    // }
    
    /**
     * Copy authentication data when player respawns
     */
    @Inject(method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V", at = @At("RETURN"))
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerAuth oldAuth = (PlayerAuth) oldPlayer;
        PlayerAuth newAuth = (PlayerAuth) (Object) this;
        
        newAuth.heos$setAuthenticated(oldAuth.heos$isAuthenticated());
        newAuth.heos$setCanSkipAuth(oldAuth.heos$canSkipAuth());
        newAuth.heos$setUsingMojangAccount(oldAuth.heos$isUsingMojangAccount());
        newAuth.heos$setPlayerData(oldAuth.heos$getPlayerData());
        newAuth.heos$setIpAddress(oldAuth.heos$getIpAddress()); // Use string setter
    }
}
