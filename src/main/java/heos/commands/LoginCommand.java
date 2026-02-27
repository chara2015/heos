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
 * Login command for offline players
 */
public class LoginCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("login")
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(LoginCommand::execute)
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
            player.sendMessage(Text.literal("§c正版玩家无需登录！"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if registered
        if (!data.isRegistered()) {
            player.sendMessage(Text.literal("§c你还没有注册！请使用 /register <密码> <确认密码>"), false);
            return 0;
        }
        
        String password = StringArgumentType.getString(context, "password");
        
        // Verify password (simple comparison for now, should use hashing)
        if (data.password.equals(password)) {
            playerAuth.heos$setAuthenticated(true);
            data.lastIp = playerAuth.heos$getIpAddress();
            
            player.sendMessage(Text.literal("§a登录成功！欢迎回来！"), false);
            HeosLogger.info("Player " + player.getName().getString() + " logged in successfully");
            return 1;
        } else {
            player.sendMessage(Text.literal("§c密码错误！"), false);
            HeosLogger.warn("Player " + player.getName().getString() + " failed to login (wrong password)");
            return 0;
        }
    }
}
