package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import heos.integrations.Permissions;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.Messages;
import heos.utils.PasswordHasher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Login command for offline players
 */
public class LoginCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> loginNode = registerLogin(dispatcher);

        dispatcher.register(
            CommandManager.literal("l")
                .requires(Permissions.require("heos.commands.login", true))
                .redirect(loginNode)
        );
    }

    public static LiteralCommandNode<ServerCommandSource> registerLogin(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(
            CommandManager.literal("login")
                .requires(Permissions.require("heos.commands.login", true))
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(LoginCommand::execute)
                )
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal(Messages.loginInputHint()));
                    return 0;
                })
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getName().getString();
        HeosLogger.info("Player " + username + " is trying to login");

        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendMessage(Text.literal(Messages.alreadyLoggedIn()), false);
            return 0;
        }

        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to login");
            player.sendMessage(Text.literal(Messages.premiumNoLogin()), false);
            return 0;
        }

        PlayerData data = playerAuth.heos$getPlayerData();

        if (!data.isRegistered()) {
            HeosLogger.info("Player " + username + " is not registered");
            player.sendMessage(Text.literal(Messages.notRegistered()), false);
            return 0;
        }

        String password = StringArgumentType.getString(context, "password");

        if (PasswordHasher.verifyPassword(password, data.passwordHash)) {
            HeosLogger.info("Player " + username + " provided correct password");
            playerAuth.heos$setAuthenticated(true);
            data.lastIp = playerAuth.heos$getIpAddress();
            data.lastLoginTime = System.currentTimeMillis();
            data.save();

            player.sendMessage(Text.literal(Messages.loginSuccess()), false);
            HeosLogger.info("Player " + username + " logged in successfully");
            return 1;
        } else {
            HeosLogger.warn("Player " + username + " provided wrong password");
            player.sendMessage(Text.literal(Messages.wrongPassword()), false);
            return 0;
        }
    }
}
