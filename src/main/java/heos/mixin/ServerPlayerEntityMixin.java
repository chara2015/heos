package heos.mixin;

import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import net.minecraft.block.Blocks;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private long heos$kickTimer = 60 * 20;

    @Unique
    private long heos$lastAuthPromptTick = Long.MIN_VALUE;

    @Override
    public void heos$setAuthenticated(boolean authenticated) {
        this.heos$authenticated = authenticated;
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (authenticated) {
            heos$lastAuthPromptTick = Long.MIN_VALUE;
            HeosLogger.debug("Player authenticated: " + player.getName().getString());
            heos$kickTimer = 60 * 20;

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
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (heos$playerData == null) {
            return;
        }

        long currentTick = player.getServerWorld().getTime();
        if (currentTick - heos$lastAuthPromptTick < 40) {
            return;
        }
        heos$lastAuthPromptTick = currentTick;

        if (heos$playerData.isRegistered()) {
            player.sendMessage(Text.literal(Messages.authPromptLogin()), false);
        } else {
            player.sendMessage(Text.literal(Messages.authPromptRegister()), false);
        }
    }

    @Override
    public boolean heos$isInvisible(boolean original) {
        return original || !heos$authenticated;
    }

    @Override
    public boolean heos$isInvulnerable(boolean original) {
        return original || !heos$authenticated;
    }

    @Inject(method = "playerTick()V", at = @At("HEAD"), cancellable = true)
    private void onPlayerTick(CallbackInfo ci) {
        if (!heos$authenticated) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            if (player.getClass() != ServerPlayerEntity.class) {
                return;
            }

            if (heos$kickTimer <= 0 && player.networkHandler.isConnectionOpen()) {
                player.networkHandler.disconnect(Text.literal(Messages.loginTimeout()));
            } else {
                if (heos$kickTimer < 56 * 20 && heos$kickTimer % 200 == 0) {
                    heos$sendAuthMessage();
                }
                --heos$kickTimer;
            }

            BlockPos pos = player.getBlockPos();
            if (player.getServerWorld().getBlockState(pos).getBlock().equals(Blocks.NETHER_PORTAL)
                    || player.getServerWorld().getBlockState(pos.up()).getBlock().equals(Blocks.NETHER_PORTAL)) {
                player.teleport(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5, false);

                BlockUpdateS2CPacket feetPacket = new BlockUpdateS2CPacket(pos, Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(feetPacket);

                BlockUpdateS2CPacket headPacket = new BlockUpdateS2CPacket(pos.up(), Blocks.AIR.getDefaultState());
                player.networkHandler.sendPacket(headPacket);
            }

            ci.cancel();
        }
    }

    @Inject(method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V", at = @At("RETURN"))
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerAuth oldAuth = (PlayerAuth) oldPlayer;
        PlayerAuth newAuth = (PlayerAuth) (Object) this;

        newAuth.heos$setAuthenticated(oldAuth.heos$isAuthenticated());
        newAuth.heos$setCanSkipAuth(oldAuth.heos$canSkipAuth());
        newAuth.heos$setUsingMojangAccount(oldAuth.heos$isUsingMojangAccount());
        newAuth.heos$setPlayerData(oldAuth.heos$getPlayerData());
        newAuth.heos$setIpAddress(oldAuth.heos$getIpAddress());
    }
}
