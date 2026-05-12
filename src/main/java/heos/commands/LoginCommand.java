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
import heos.utils.LoginFailureTracker;
import heos.utils.Messages;
import heos.utils.PasswordHasher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Login command for offline players
 */
public class LoginCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> loginNode = registerLogin(dispatcher);

        dispatcher.register(
            Commands.literal("l")
                .requires(Permissions.require("heos.commands.login", true))
                .redirect(loginNode)
        );
    }

    public static LiteralCommandNode<CommandSourceStack> registerLogin(CommandDispatcher<CommandSourceStack> dispatcher) {
        return dispatcher.register(
            Commands.literal("login")
                .requires(Permissions.require("heos.commands.login", true))
                .then(Commands.argument("password", StringArgumentType.string())
                    .executes(LoginCommand::execute)
                )
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.literal(Messages.loginInputHint()));
                    return 0;
                })
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String password = StringArgumentType.getString(context, "password");
        return execute(player, password);
    }

    public static int execute(ServerPlayer player, String password) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getName().getString();
        String ip = playerAuth.heos$getIpAddress();
        HeosLogger.info("Player " + username + " is trying to login");

        if (LoginFailureTracker.isBlocked(username, ip)) {
            player.connection.disconnect(Component.literal(LoginFailureTracker.blockMessage(username, ip)));
            return 0;
        }

        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendSystemMessage(Component.literal(Messages.alreadyLoggedIn()), false);
            return 0;
        }

        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to login");
            player.sendSystemMessage(Component.literal(Messages.premiumNoLogin()), false);
            return 0;
        }

        PlayerData data = playerAuth.heos$getPlayerData();

        if (!data.isRegistered()) {
            HeosLogger.info("Player " + username + " is not registered");
            player.sendSystemMessage(Component.literal(Messages.notRegistered()), false);
            return 0;
        }

        if (PasswordHasher.verifyPassword(password, data.passwordHash)) {
            HeosLogger.info("Player " + username + " provided correct password");
            LoginFailureTracker.reset(username, ip);
            playerAuth.heos$setAuthenticated(true);
            if (PasswordHasher.needsRehash(data.passwordHash)) {
                String upgradedHash = PasswordHasher.hashPassword(password);
                if (upgradedHash != null) {
                    data.passwordHash = upgradedHash;
                }
            }
            data.lastIp = playerAuth.heos$getIpAddress();
            data.lastLoginTime = System.currentTimeMillis();
            data.save();

            player.sendSystemMessage(Component.literal(Messages.loginSuccess()), false);
            HeosLogger.info("Player " + username + " logged in successfully");
            return 1;
        } else {
            HeosLogger.warn("Player " + username + " provided wrong password");
            if (LoginFailureTracker.recordFailure(username, ip)) {
                player.connection.disconnect(Component.literal(LoginFailureTracker.blockMessage(username, ip)));
            } else {
                player.sendSystemMessage(Component.literal(Messages.wrongPassword()), false);
            }
            return 0;
        }
    }
}
