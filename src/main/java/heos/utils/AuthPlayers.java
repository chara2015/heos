package heos.utils;

import heos.interfaces.PlayerAuth;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared auth-state predicates for protections that should only apply to real clients.
 */
public final class AuthPlayers {
    private AuthPlayers() {
    }

    public static boolean isRealPlayerWaitingForAuth(Object entity) {
        return entity instanceof ServerPlayer player
                && player.getClass() == ServerPlayer.class
                && entity instanceof PlayerAuth auth
                && !auth.heos$isAuthenticated();
    }
}
