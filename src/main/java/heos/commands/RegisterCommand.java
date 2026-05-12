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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Register command for offline players
 */
public class RegisterCommand {

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> registerNode = registerRegister(dispatcher);

        dispatcher.register(
            Commands.literal("reg")
                .requires(Permissions.require("heos.commands.register", true))
                .redirect(registerNode)
        );
    }

    public static LiteralCommandNode<CommandSourceStack> registerRegister(CommandDispatcher<CommandSourceStack> dispatcher) {
        return dispatcher.register(
            Commands.literal("register")
                .requires(Permissions.require("heos.commands.register", true))
                .then(Commands.argument("password", StringArgumentType.string())
                    .then(Commands.argument("confirmPassword", StringArgumentType.string())
                        .executes(RegisterCommand::execute)
                    )
                )
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.literal(Messages.registerInputHint()));
                    return 0;
                })
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String password = StringArgumentType.getString(context, "password");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");
        return execute(player, password, confirmPassword);
    }

    public static int execute(ServerPlayer player, String password, String confirmPassword) {
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getName().getString();
        HeosLogger.info("Player " + username + " is trying to register");

        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendSystemMessage(Component.literal(Messages.alreadyLoggedIn()), false);
            return 0;
        }

        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to register");
            player.sendSystemMessage(Component.literal(Messages.premiumNoRegister()), false);
            return 0;
        }

        PlayerData data = playerAuth.heos$getPlayerData();

        if (data.isRegistered()) {
            HeosLogger.info("Player " + username + " is already registered");
            player.sendSystemMessage(Component.literal(Messages.alreadyRegistered()), false);
            return 0;
        }

        if (password.length() < heos.Heos.getConfig().minPasswordLength) {
            player.sendSystemMessage(Component.literal(Messages.passwordTooShort()), false);
            return 0;
        }

        if (password.length() > heos.Heos.getConfig().maxPasswordLength) {
            player.sendSystemMessage(Component.literal(Messages.passwordTooLong()), false);
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            player.sendSystemMessage(Component.literal(Messages.passwordMismatch()), false);
            return 0;
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        if (passwordHash == null) {
            player.sendSystemMessage(Component.literal(Messages.registerFailed()), false);
            HeosLogger.error("Failed to hash password for " + username);
            return 0;
        }

        data.passwordHash = passwordHash;
        data.lastIp = playerAuth.heos$getIpAddress();
        data.uuid = player.getUUID();
        data.registeredTime = System.currentTimeMillis();
        data.lastLoginTime = System.currentTimeMillis();
        data.save();

        playerAuth.heos$setAuthenticated(true);

        player.sendSystemMessage(Component.literal(Messages.registerSuccess()), false);
        player.sendSystemMessage(Component.literal(Messages.keepPasswordSafe()), false);
        HeosLogger.info("Player " + username + " registered successfully");

        return 1;
    }
}
