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
 * Register command for offline players
 */
public class RegisterCommand {
    
    public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> registerNode = registerRegister(dispatcher);
        
        // Register alias "/reg"
        dispatcher.register(
            CommandManager.literal("reg")
                .redirect(registerNode)
        );
    }
    
    public static LiteralCommandNode<ServerCommandSource> registerRegister(CommandDispatcher<ServerCommandSource> dispatcher) {
        return dispatcher.register(
            CommandManager.literal("register")
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .then(CommandManager.argument("confirmPassword", StringArgumentType.string())
                        .executes(RegisterCommand::execute)
                    )
                )
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Text.literal("§e请输入密码: /register <密码> <确认密码>"));
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
        
        // Check if already authenticated
        if (playerAuth.heos$isAuthenticated()) {
            HeosLogger.info("Player " + username + " is already authenticated");
            player.sendMessage(Text.literal("§c你已经登录了！"), false);
            return 0;
        }
        
        // Check if player can skip auth (premium player)
        if (playerAuth.heos$canSkipAuth()) {
            HeosLogger.info("Player " + username + " is premium, no need to register");
            player.sendMessage(Text.literal("§c正版玩家无需注册！"), false);
            return 0;
        }
        
        PlayerData data = playerAuth.heos$getPlayerData();
        
        // Check if already registered
        if (data.isRegistered()) {
            HeosLogger.info("Player " + username + " is already registered");
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
        
        // Hash password and register player
        String passwordHash = PasswordHasher.hashPassword(password);
        if (passwordHash == null) {
            player.sendMessage(Text.literal("§c注册失败！请联系管理员"), false);
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
        
        player.sendMessage(Text.literal("§a================================="), false);
        player.sendMessage(Text.literal("§a注册成功！你已自动登录"), false);
        player.sendMessage(Text.literal("§e请妥善保管你的密码"), false);
        player.sendMessage(Text.literal("§a================================="), false);
        HeosLogger.info("Player " + username + " registered successfully");
        
        return 1;
    }
}
