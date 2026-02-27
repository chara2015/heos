package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Register command for offline players
 */
public class RegisterCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("register")
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .then(CommandManager.argument("confirmPassword", StringArgumentType.string())
                        .executes(RegisterCommand::execute)
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
        
        // Check if already authenticated
        if (playerAuth.heos$isAuthenticated()) {
            player.sendMessage(Text.literal("§c你已经登录了！"), false);
            return 0;
        }
        
        // Check if player can skip auth (premium player)
        if (playerAuth.heos$canSkipAuth()) {
            player.sendMessage(Text.literal("§c正版玩家无需注册！"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if already registered
        if (data.isRegistered()) {
            player.sendMessage(Text.literal("§c你已经注册过了！请使用 /login <密码>"), false);
            return 0;
        }
        
        String password = StringArgumentType.getString(context, "password");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");
        
        // Validate password length
        if (password.length() < 4) {
            player.sendMessage(Text.literal("§c密码太短！至少需要4个字符"), false);
            return 0;
        }
        
        if (password.length() > 32) {
            player.sendMessage(Text.literal("§c密码太长！最多32个字符"), false);
            return 0;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            player.sendMessage(Text.literal("§c两次输入的密码不一致！"), false);
            return 0;
        }
        
        // Register player (simple storage for now, should use hashing and database)
        data.password = password;
        data.lastIp = playerAuth.heos$getIpAddress();
        data.uuid = player.getUuid();
        
        playerAuth.heos$setAuthenticated(true);
        
        player.sendMessage(Text.literal("§a注册成功！你已自动登录"), false);
        HeosLogger.info("Player " + player.getName().getString() + " registered successfully");
        
        return 1;
    }
}
