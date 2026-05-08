package heos.integrations;

import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class Permissions {

    public static @NotNull Predicate<ServerCommandSource> require(@NotNull String permission, boolean defaultValue) {
        return source -> defaultValue;
    }

    public static @NotNull Predicate<ServerCommandSource> requireLevel(int level) {
        return source -> {
            if (!source.isExecutedByPlayer()) {
                return true;
            }
            return source.getServer().getPlayerManager().isOperator(source.getPlayer().getGameProfile());
        };
    }
}
