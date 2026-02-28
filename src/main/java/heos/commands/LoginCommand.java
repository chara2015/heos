package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import heos.interfaces.PlayerAuth;
import heos.storage.PlayerData;
import heos.utils.HeosLogger;
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
        
        // Register alias "/l"
        dispatcher.register(
            CommandManager.literal("l")
                .redirect(loginNode)
        );
    }
    
    public static LiteralCommandNode<ServerCommandSource> registerLogin(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(
            CommandManager.literal("login")
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(LoginCommand::execute)
                )
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal("§e请输入密码: /login <密码>"));
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
        
        // Check if already authenticated
        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendMessage(Text.literal("§c你已经登录了！"), false);
            return 0;
        }
        
        // Check if player can skip auth (premium player)
        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to login");
            player.sendMessage(Text.literal("§c正版玩家无需登录！"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if registered
        if (!data.isRegistered()) {
            HeosLogger.info("Player " + username + " is not registered");
            player.sendMessage(Text.literal("§c你还没有注册！请使用 /register <密码> <确认密码>"), false);
            return 0;
        }
        
        String password = StringArgumentType.getString(context, "password");
        
        // Verify password using hash
        if (PasswordHasher.verifyPassword(password, data.passwordHash)) {
            HeosLogger.info("Player " + username + " provided correct password");
            playerAuth.heos$setAuthenticated(true);
            data.lastIp = playerAuth.heos$getIpAddress();
            data.lastLoginTime = System.currentTimeMillis();
            data.save();
            
            player.sendMessage(Text.literal("§a================================="), false);
            player.sendMessage(Text.literal("§a登录成功！欢迎回来！"), false);
            player.sendMessage(Text.literal("§a================================="), false);
            HeosLogger.info("Player " + username + " logged in successfully");
            return 1;
        } else {
            HeosLogger.warn("Player " + username + " provided wrong password");
            player.sendMessage(Text.literal("§c密码错误！"), false);
            return 0;
        }
    }
}
