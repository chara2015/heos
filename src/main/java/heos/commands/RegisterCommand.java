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
 * Register command for offline players
 */
public class RegisterCommand {

    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> registerNode = registerRegister(dispatcher);

        dispatcher.register(
            CommandManager.literal("reg")
                .requires(Permissions.require("heos.commands.register", true))
                .redirect(registerNode)
        );
    }

    public static LiteralCommandNode<ServerCommandSource> registerRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(
            CommandManager.literal("register")
                .requires(Permissions.require("heos.commands.register", true))
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .then(CommandManager.argument("confirmPassword", StringArgumentType.string())
                        .executes(RegisterCommand::execute)
                    )
                )
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal(Messages.registerInputHint()));
                    return 0;
                })
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        PlayerAuth playerAuth = (PlayerAuth) player;

        String username = player.getName().getString();
        HeosLogger.info("Player " + username + " is trying to register");

        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendMessage(Text.literal(Messages.alreadyLoggedIn()), false);
            return 0;
        }

        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to register");
            player.sendMessage(Text.literal(Messages.premiumNoRegister()), false);
            return 0;
        }

        PlayerData data = playerAuth.heos$getPlayerData();

        if (data.isRegistered()) {
            HeosLogger.info("Player " + username + " is already registered");
            player.sendMessage(Text.literal(Messages.alreadyRegistered()), false);
            return 0;
        }

        String password = StringArgumentType.getString(context, "password");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");

        if (password.length() < 4) {
            player.sendMessage(Text.literal(Messages.passwordTooShort()), false);
            return 0;
        }

        if (password.length() > 32) {
            player.sendMessage(Text.literal(Messages.passwordTooLong()), false);
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            player.sendMessage(Text.literal(Messages.passwordMismatch()), false);
            return 0;
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        if (passwordHash == null) {
            player.sendMessage(Text.literal(Messages.registerFailed()), false);
            HeosLogger.error("Failed to hash password for " + username);
            return 0;
        }

        data.passwordHash = passwordHash;
        data.lastIp = playerAuth.heos$getIpAddress();
        data.uuid = player.getUuid();
        data.registeredTime = System.currentTimeMillis();
        data.lastLoginTime = System.currentTimeMillis();
        data.save();

        playerAuth.heos$setAuthenticated(true);

        player.sendMessage(Text.literal(Messages.registerSuccess()), false);
        player.sendMessage(Text.literal(Messages.keepPasswordSafe()), false);
        HeosLogger.info("Player " + username + " registered successfully");

        return 1;
    }
}
