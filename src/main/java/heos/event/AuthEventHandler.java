package heos.event;

import heos.interfaces.PlayerAuth;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Handles authentication events and restricts unauthenticated players
 */
public class AuthEventHandler {
    
    /**
     * Checks if packet is allowed for unauthenticated players
     */
    public static boolean isAllowedPacket(Packet<?> packet) {
        // Always allow these packets
        if (packet instanceof KeepAliveC2SPacket
                || packet instanceof ResourcePackStatusC2SPacket
                || packet instanceof TeleportConfirmC2SPacket
                || packet instanceof PlayerSessionC2SPacket
                || packet instanceof ClientStatusC2SPacket
                || packet instanceof CommonPongC2SPacket
                || packet instanceof ClientOptionsC2SPacket
                || packet instanceof AcknowledgeChunksC2SPacket
                || packet instanceof AcknowledgeReconfigurationC2SPacket
                //? if >= 1.21.2 {
                || packet instanceof ClientTickEndC2SPacket
                //?}
                //? if >= 1.21.5 {
                /*|| packet instanceof PlayerLoadedC2SPacket
                *///?}
        ) {
            return true;
        }
        
        // Allow movement packets (but position will be reset)
        if (packet instanceof PlayerMoveC2SPacket
                || packet instanceof VehicleMoveC2SPacket
                || packet instanceof PlayerInputC2SPacket) {
            return true;
        }
        
        // Allow command execution (for /login and /register)
        if (packet instanceof CommandExecutionC2SPacket
                || packet instanceof RequestCommandCompletionsC2SPacket) {
            return true;
        }
        
        // Block everything else
        return false;
    }
    
    /**
     * Handles player movement - prevents unauthenticated players from moving
     */
    public static ActionResult onPlayerMove(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            // Teleport player back to prevent movement
            player.networkHandler.requestTeleport(
                player.getX(), 
                player.getY(), 
                player.getZ(), 
                player.getYaw(), 
                player.getPitch()
            );
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles player chat
     */
    public static ActionResult onPlayerChat(ServerPlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles block breaking
     */
    public static boolean onBreakBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return false;
        }
        return true;
    }
    
    /**
     * Handles block interaction
     */
    public static ActionResult onUseBlock(PlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles item usage
     */
    public static ActionResult onUseItem(PlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles entity attack
     */
    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles entity interaction
     */
    public static ActionResult onUseEntity(PlayerEntity player) {
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
    
    /**
     * Handles command execution
     */
    public static ActionResult onPlayerCommand(ServerPlayerEntity player, String command) {
        // Allow login and register commands
        if (command.startsWith("login ") || command.startsWith("register ")) {
            return ActionResult.PASS;
        }
        
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }
}
