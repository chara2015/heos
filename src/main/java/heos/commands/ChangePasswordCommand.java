package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import heos.utils.PasswordHasher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Change password command
 */
public class ChangePasswordCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("changepassword")
                .then(CommandManager.argument("oldPassword", StringArgumentType.string())
                    .then(CommandManager.argument("newPassword", StringArgumentType.string())
                        .then(CommandManager.argument("confirmPassword", StringArgumentType.string())
                            .executes(ChangePasswordCommand::execute)
                        )
                    )
                )
        );
        
        // Alias
        dispatcher.register(
            CommandManager.literal("changepw")
                .then(CommandManager.argument("oldPassword", StringArgumentType.string())
                    .then(CommandManager.argument("newPassword", StringArgumentType.string())
                        .then(CommandManager.argument("confirmPassword", StringArgumentType.string())
                            .executes(ChangePasswordCommand::execute)
                        )
                    )
                )
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("此命令只能由玩家执行"));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        PlayerAuth playerAuth = (PlayerAuth) player;
        
        // Check if player can skip auth (premium player)
        if (playerAuth.heos$canSkipAuth()) {
            player.sendMessage(Text.literal("§c正版玩家无需修改密码！"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if registered
        if (!data.isRegistered()) {
            player.sendMessage(Text.literal("§c你还没有注册！请使用 /register <密码> <确认密码>"), false);
            return 0;
        }
        
        String oldPassword = StringArgumentType.getString(context, "oldPassword");
        String newPassword = StringArgumentType.getString(context, "newPassword");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");
        
        // Verify old password
        if (!PasswordHasher.verifyPassword(oldPassword, data.passwordHash)) {
            player.sendMessage(Text.literal("§c旧密码错误！"), false);
            HeosLogger.warn("Player " + player.getName().getString() + " failed to change password (wrong old password)");
            return 0;
        }
        
        // Validate new password length
        if (newPassword.length() < 4) {
            player.sendMessage(Text.literal("§c新密码太短！至少需要4个字符"), false);
            return 0;
        }
        
        if (newPassword.length() > 32) {
            player.sendMessage(Text.literal("§c新密码太长！最多32个字符"), false);
            return 0;
        }
        
        // Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
            player.sendMessage(Text.literal("§c两次输入的新密码不一致！"), false);
            return 0;
        }
        
        // Check if new password is same as old
        if (oldPassword.equals(newPassword)) {
            player.sendMessage(Text.literal("§c新密码不能与旧密码相同！"), false);
            return 0;
        }
        
        // Hash new password and save
        String newPasswordHash = PasswordHasher.hashPassword(newPassword);
        if (newPasswordHash == null) {
            player.sendMessage(Text.literal("§c修改密码失败！请联系管理员"), false);
            HeosLogger.error("Failed to hash new password for " + player.getName().getString());
            return 0;
        }
        
        data.passwordHash = newPasswordHash;
        data.save();
        
        player.sendMessage(Text.literal("§a================================="), false);
        player.sendMessage(Text.literal("§a密码修改成功！"), false);
        player.sendMessage(Text.literal("§e请妥善保管你的新密码"), false);
        player.sendMessage(Text.literal("§a================================="), false);
        HeosLogger.info("Player " + player.getName().getString() + " changed password successfully");
        
        return 1;
    }
}


