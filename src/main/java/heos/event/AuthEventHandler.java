package heos.event;

import heos.interfaces.PlayerAuth;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
//? if >= 1.20.2 {
import net.minecraft.network.packet.c2s.common.*;
//?}
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Handles authentication events and restricts unauthenticated players
 */
public class AuthEventHandler {

    public static boolean isAllowedPacket(Packet<?> packet) {
        if (packet instanceof KeepAliveC2SPacket
                || packet instanceof ResourcePackStatusC2SPacket
                || packet instanceof TeleportConfirmC2SPacket
                || packet instanceof PlayerSessionC2SPacket
                || packet instanceof ClientStatusC2SPacket
                //? if >= 1.20.2 {
                || packet instanceof CommonPongC2SPacket
                || packet instanceof ClientOptionsC2SPacket
                || packet instanceof AcknowledgeChunksC2SPacket
                || packet instanceof AcknowledgeReconfigurationC2SPacket
                //?}
                || packet instanceof RequestCommandCompletionsC2SPacket
                || packet instanceof CommandExecutionC2SPacket
                //? if >= 1.21.2 {
                || packet instanceof ClientTickEndC2SPacket
                //?}
                //? if >= 1.21.5 {
                /*|| packet instanceof PlayerLoadedC2SPacket
                *///?}
        ) {
            return true;
        }

        if (packet instanceof PlayerMoveC2SPacket
                || packet instanceof VehicleMoveC2SPacket
                || packet instanceof PlayerInputC2SPacket) {
            return true;
        }

        return false;
    }

    public static ActionResult onPlayerMove(ServerPlayerEntity player, PlayerMoveC2SPacket packet) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            double nextX = packet.getX(player.getX());
            double nextY = packet.getY(player.getY());
            double nextZ = packet.getZ(player.getZ());

            boolean moved = Double.compare(nextX, player.getX()) != 0
                    || Double.compare(nextY, player.getY()) != 0
                    || Double.compare(nextZ, player.getZ()) != 0;

            float yaw = packet.getYaw(player.getYaw());
            float pitch = packet.getPitch(player.getPitch());
            player.setYaw(yaw);
            player.setPitch(pitch);
            player.setHeadYaw(yaw);

            if (!moved) {
                return ActionResult.PASS;
            }
            player.networkHandler.requestTeleport(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    yaw,
                    pitch
            );
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onPlayerChat(ServerPlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static boolean onBreakBlock(PlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return true;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return false;
        }
        return true;
    }

    public static ActionResult onUseBlock(PlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onUseItem(PlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onTakeItem(ServerPlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onInventoryAction(ServerPlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onHotbarChange(ServerPlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onDropItem(ServerPlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onAttackEntity(PlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onUseEntity(PlayerEntity player) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    public static ActionResult onPlayerCommand(ServerPlayerEntity player, String command) {
        if (player.getClass() != ServerPlayerEntity.class) {
            return ActionResult.PASS;
        }
        if (command.startsWith("login ") || command.startsWith("register ") || command.startsWith("l ") || command.startsWith("reg ")) {
            return ActionResult.PASS;
        }

        if (!((PlayerAuth) player).heos$isAuthenticated()) {
            ((PlayerAuth) player).heos$sendAuthMessage();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}
