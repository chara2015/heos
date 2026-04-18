package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import heos.Heos;
import heos.storage.PlayerData;
import heos.storage.WhitelistData;
import heos.utils.HeosLogger;
import heos.utils.PasswordHasher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Admin commands for managing player accounts
 */
public class HeosAdminCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("heosadmin")
                .requires(source -> source.hasPermissionLevel(3))
                .then(CommandManager.literal("resetpassword")
                    .then(CommandManager.argument("player", StringArgumentType.string())
                        .then(CommandManager.argument("newPassword", StringArgumentType.string())
                            .executes(HeosAdminCommand::resetPassword)
                        )
                    )
                )
                .then(CommandManager.literal("unregister")
                    .then(CommandManager.argument("player", StringArgumentType.string())
                        .executes(HeosAdminCommand::unregister)
                    )
                )
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("player", StringArgumentType.string())
                        .executes(HeosAdminCommand::info)
                    )
                )
                .then(CommandManager.literal("whitelist")
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", StringArgumentType.string())
                            .executes(HeosAdminCommand::whitelistAdd)
                        )
                    )
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", StringArgumentType.string())
                            .executes(HeosAdminCommand::whitelistRemove)
                        )
                    )
                    .then(CommandManager.literal("list")
                        .executes(HeosAdminCommand::whitelistList)
                    )
                )
        );
    }

    private static int resetPassword(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetUsername = StringArgumentType.getString(context, "player");
        String newPassword = StringArgumentType.getString(context, "newPassword");

        if (newPassword.length() < 4) {
            source.sendError(Text.literal("§c新密码太短！至少需要4个字符"));
            return 0;
        }

        if (newPassword.length() > 32) {
            source.sendError(Text.literal("§c新密码太长！最多32个字符"));
            return 0;
        }

        PlayerData data = Heos.getPlayerData(targetUsername);
        if (!data.isRegistered()) {
            source.sendError(Text.literal("§c玩家 " + targetUsername + " 还没有注册！"));
            return 0;
        }

        String newPasswordHash = PasswordHasher.hashPassword(newPassword);
        if (newPasswordHash == null) {
            source.sendError(Text.literal("§c重置密码失败！"));
            HeosLogger.error("Failed to hash password for " + targetUsername);
            return 0;
        }

        data.passwordHash = newPasswordHash;
        data.save();

        source.sendFeedback(() -> Text.literal("§a成功重置玩家 " + targetUsername + " 的密码"), true);
        HeosLogger.info("Admin " + source.getName() + " reset password for " + targetUsername);

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(targetUsername);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Text.literal("§c================================="), false);
            targetPlayer.sendMessage(Text.literal("§c你的密码已被管理员重置！"), false);
            targetPlayer.sendMessage(Text.literal("§e新密码：" + newPassword), false);
            targetPlayer.sendMessage(Text.literal("§e请尽快使用 /changepassword 修改密码"), false);
            targetPlayer.sendMessage(Text.literal("§c================================="), false);
        }

        return 1;
    }

    private static int unregister(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetUsername = StringArgumentType.getString(context, "player");

        PlayerData data = Heos.getPlayerData(targetUsername);
        if (!data.isRegistered()) {
            source.sendError(Text.literal("§c玩家 " + targetUsername + " 还没有注册！"));
            return 0;
        }

        data.passwordHash = "";
        data.save();
        Heos.removePlayerData(targetUsername);

        source.sendFeedback(() -> Text.literal("§a成功注销玩家 " + targetUsername + " 的账号"), true);
        HeosLogger.info("Admin " + source.getName() + " unregistered " + targetUsername);

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(targetUsername);
        if (targetPlayer != null) {
            targetPlayer.networkHandler.disconnect(Text.literal("§c你的账号已被管理员注销\n§e请重新注册"));
        }

        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetUsername = StringArgumentType.getString(context, "player");

        PlayerData data = Heos.getPlayerData(targetUsername);
        if (!data.isRegistered()) {
            source.sendError(Text.literal("§c玩家 " + targetUsername + " 还没有注册！"));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("§e================================="), false);
        source.sendFeedback(() -> Text.literal("§e玩家信息：" + targetUsername), false);
        source.sendFeedback(() -> Text.literal("§7UUID: " + (data.uuid != null ? data.uuid.toString() : "未知")), false);
        source.sendFeedback(() -> Text.literal("§7最后IP: " + (data.lastIp != null && !data.lastIp.isEmpty() ? data.lastIp : "未知")), false);
        source.sendFeedback(() -> Text.literal("§7注册时间: " + (data.registeredTime > 0 ? new java.util.Date(data.registeredTime).toString() : "未知")), false);
        source.sendFeedback(() -> Text.literal("§7最后登录: " + (data.lastLoginTime > 0 ? new java.util.Date(data.lastLoginTime).toString() : "未知")), false);
        source.sendFeedback(() -> Text.literal("§7账号类型: " + (data.isOnlineAccount ? "正版" : "离线")), false);
        source.sendFeedback(() -> Text.literal("§e================================="), false);

        return 1;
    }

    private static int whitelistAdd(CommandContext<ServerCommandSource> context) {
        String username = StringArgumentType.getString(context, "player");
        WhitelistData whitelistData = Heos.getWhitelistData();

        if (whitelistData.add(username)) {
            context.getSource().sendFeedback(() -> Text.literal("§a已将 " + username + " 添加到白名单"), true);
            return 1;
        }

        context.getSource().sendError(Text.literal("§c玩家已在白名单中: " + username));
        return 0;
    }

    private static int whitelistRemove(CommandContext<ServerCommandSource> context) {
        String username = StringArgumentType.getString(context, "player");
        WhitelistData whitelistData = Heos.getWhitelistData();

        if (whitelistData.remove(username)) {
            context.getSource().sendFeedback(() -> Text.literal("§a已将 " + username + " 从白名单移除"), true);
            return 1;
        }

        context.getSource().sendError(Text.literal("§c玩家不在白名单中: " + username));
        return 0;
    }

    private static int whitelistList(CommandContext<ServerCommandSource> context) {
        WhitelistData whitelistData = Heos.getWhitelistData();
        context.getSource().sendFeedback(() -> Text.literal("§e白名单人数: " + whitelistData.usernames.size()), false);
        if (!whitelistData.usernames.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("§7" + String.join(", ", whitelistData.usernames)), false);
        }
        return 1;
    }
}
