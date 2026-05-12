package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.PasswordHasher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Change password command
 */
public class ChangePasswordCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("changepassword")
                .then(Commands.argument("oldPassword", StringArgumentType.string())
                    .then(Commands.argument("newPassword", StringArgumentType.string())
                        .then(Commands.argument("confirmPassword", StringArgumentType.string())
                            .executes(ChangePasswordCommand::execute)
                        )
                    )
                )
        );
        
        // Alias
        dispatcher.register(
            Commands.literal("changepw")
                .then(Commands.argument("oldPassword", StringArgumentType.string())
                    .then(Commands.argument("newPassword", StringArgumentType.string())
                        .then(Commands.argument("confirmPassword", StringArgumentType.string())
                            .executes(ChangePasswordCommand::execute)
                        )
                    )
                )
        );
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("This command can only be run by a player"));
            return 0;
        }
        
        ServerPlayer player = source.getPlayer();
        String oldPassword = StringArgumentType.getString(context, "oldPassword");
        String newPassword = StringArgumentType.getString(context, "newPassword");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");
        return execute(player, oldPassword, newPassword, confirmPassword);
    }

    public static int execute(ServerPlayer player, String oldPassword, String newPassword, String confirmPassword) {
        PlayerAuth playerAuth = (PlayerAuth) player;
        
        // Check if player can skip auth (premium player)
        if (playerAuth.heos$canSkipAuth()) {
            player.sendSystemMessage(Component.literal("Premium players do not need to change a password"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if registered
        if (!data.isRegistered()) {
            player.sendSystemMessage(Component.literal("You are not registered. Use /register <password> <confirmPassword>"), false);
            return 0;
        }
        
        // Verify old password
        if (!PasswordHasher.verifyPassword(oldPassword, data.passwordHash)) {
            player.sendSystemMessage(Component.literal("Old password is incorrect"), false);
            HeosLogger.warn("Player " + player.getName().getString() + " failed to change password (wrong old password)");
            return 0;
        }
        
        // Validate new password length
        if (newPassword.length() < heos.Heos.getConfig().minPasswordLength) {
            player.sendSystemMessage(Component.literal("New password is too short. Minimum length is " + heos.Heos.getConfig().minPasswordLength + " characters"), false);
            return 0;
        }
        
        if (newPassword.length() > heos.Heos.getConfig().maxPasswordLength) {
            player.sendSystemMessage(Component.literal("New password is too long. Maximum length is " + heos.Heos.getConfig().maxPasswordLength + " characters"), false);
            return 0;
        }
        
        // Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
            player.sendSystemMessage(Component.literal("New passwords do not match"), false);
            return 0;
        }
        
        // Check if new password is same as old
        if (oldPassword.equals(newPassword)) {
            player.sendSystemMessage(Component.literal("New password cannot be the same as the old password"), false);
            return 0;
        }
        
        // Hash new password and save
        String newPasswordHash = PasswordHasher.hashPassword(newPassword);
        if (newPasswordHash == null) {
            player.sendSystemMessage(Component.literal("Failed to change password. Please contact an administrator"), false);
            HeosLogger.error("Failed to hash new password for " + player.getName().getString());
            return 0;
        }
        
        data.passwordHash = newPasswordHash;
        data.save();
        
        player.sendSystemMessage(Component.literal("================================="), false);
        player.sendSystemMessage(Component.literal("Password changed successfully"), false);
        player.sendSystemMessage(Component.literal("Keep your new password safe"), false);
        player.sendSystemMessage(Component.literal("================================="), false);
        HeosLogger.info("Player " + player.getName().getString() + " changed password successfully");
        
        return 1;
    }
}



