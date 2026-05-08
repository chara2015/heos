package heos.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import heos.Heos;
import heos.integrations.Permissions;
import heos.storage.BanData;
import heos.utils.HeosLogger;
import heos.utils.TimeParser;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

@SuppressWarnings("unused")
public class BanCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("ban")
                .requires(Permissions.requireLevel(3))
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .executes(ctx -> banPlayer(ctx, null, "未指定原因 (No reason specified)"))
                    .then(CommandManager.argument("time", StringArgumentType.string())
                        .executes(ctx -> {
                            String timeStr = StringArgumentType.getString(ctx, "time");
                            if (!timeStr.matches("^-?\\d+[smhdy]?$")) {
                                return banPlayer(ctx, null, timeStr);
                            }
                            return banPlayer(ctx, timeStr, "未指定原因 (No reason specified)");
                        })
                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> banPlayer(ctx,
                                StringArgumentType.getString(ctx, "time"),
                                StringArgumentType.getString(ctx, "reason")))
                        )
                    )
                )
        );

        dispatcher.register(
            CommandManager.literal("ban-ip")
                .requires(Permissions.requireLevel(3))
                .then(CommandManager.argument("ip", StringArgumentType.string())
                    .executes(ctx -> banIp(ctx, null, "未指定原因 (No reason specified)"))
                    .then(CommandManager.argument("time", StringArgumentType.string())
                        .executes(ctx -> {
                            String timeStr = StringArgumentType.getString(ctx, "time");
                            if (!timeStr.matches("^-?\\d+[smhdy]?$")) {
                                return banIp(ctx, null, timeStr);
                            }
                            return banIp(ctx, timeStr, "未指定原因 (No reason specified)");
                        })
                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> banIp(ctx,
                                StringArgumentType.getString(ctx, "time"),
                                StringArgumentType.getString(ctx, "reason")))
                        )
                    )
                )
        );

        dispatcher.register(
            CommandManager.literal("unban")
                .requires(Permissions.requireLevel(3))
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .executes(BanCommands::unbanPlayer)
                )
        );

        dispatcher.register(
            CommandManager.literal("unban-ip")
                .requires(Permissions.requireLevel(3))
                .then(CommandManager.argument("ip", StringArgumentType.string())
                    .executes(BanCommands::unbanIp)
                )
        );

        dispatcher.register(
            CommandManager.literal("banlist")
                .requires(Permissions.requireLevel(3))
                .executes(BanCommands::listBans)
        );
    }

    private static int banPlayer(CommandContext<ServerCommandSource> context, String timeStr, String reason) {
        ServerCommandSource source = context.getSource();
        String targetUsername = StringArgumentType.getString(context, "player");

        long expiryTime = -1;
        if (timeStr != null) {
            expiryTime = TimeParser.parseTime(timeStr);
            if (expiryTime == -2) {
                source.sendError(Text.literal("§c无效的时间格式！使用: 15s, 3m, 24h, 7d, 1y 或 -1 (永久)"));
                return 0;
            }
        }

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(targetUsername);
        UUID targetUuid = targetPlayer != null ? targetPlayer.getUuid() : null;

        BanData banData = Heos.getBanData();
        banData.addPlayerBan(targetUsername, targetUuid, reason, expiryTime, source.getName());

        final String timeInfo = TimeParser.formatExpiryTime(expiryTime);
        source.sendFeedback(() -> Text.literal("§a已封禁玩家 " + targetUsername), true);
        source.sendFeedback(() -> Text.literal("§7原因: " + reason), false);
        source.sendFeedback(() -> Text.literal("§7时长: " + timeInfo), false);

        HeosLogger.info(source.getName() + " banned " + targetUsername + " for " + timeInfo + " - Reason: " + reason);

        if (targetPlayer != null) {
            String kickMessage = Heos.getConfig().banMessageFormat
                .replace("%reason%", reason)
                .replace("%expiry%", TimeParser.formatAbsoluteTime(expiryTime));
            targetPlayer.networkHandler.disconnect(Text.literal(kickMessage));
        }

        return 1;
    }

    static int banPlayerProgrammatic(String username, UUID uuid, String reason, long expiryTime, String bannedBy) {
        BanData banData = Heos.getBanData();
        banData.addPlayerBan(username, uuid, reason, expiryTime, bannedBy);
        return 1;
    }

    private static int banIp(CommandContext<ServerCommandSource> context, String timeStr, String reason) {
        ServerCommandSource source = context.getSource();
        String targetIp = StringArgumentType.getString(context, "ip");

        long expiryTime = -1;
        if (timeStr != null) {
            expiryTime = TimeParser.parseTime(timeStr);
            if (expiryTime == -2) {
                source.sendError(Text.literal("§c无效的时间格式！使用: 15s, 3m, 24h, 7d, 1y 或 -1 (永久)"));
                return 0;
            }
        }

        BanData banData = Heos.getBanData();
        banData.addIpBan(targetIp, reason, expiryTime, source.getName());

        final String timeInfo = TimeParser.formatExpiryTime(expiryTime);
        source.sendFeedback(() -> Text.literal("§a已封禁IP " + targetIp), true);
        source.sendFeedback(() -> Text.literal("§7原因: " + reason), false);
        source.sendFeedback(() -> Text.literal("§7时长: " + timeInfo), false);

        HeosLogger.info(source.getName() + " banned IP " + targetIp + " for " + timeInfo + " - Reason: " + reason);

        String kickMessage = Heos.getConfig().banIpMessageFormat
            .replace("%reason%", reason)
            .replace("%expiry%", TimeParser.formatAbsoluteTime(expiryTime));

        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            String playerIp = player.getIp();
            if (playerIp.equals(targetIp)) {
                player.networkHandler.disconnect(Text.literal(kickMessage));
            }
        }

        return 1;
    }

    private static int unbanPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetUsername = StringArgumentType.getString(context, "player");

        BanData banData = Heos.getBanData();
        if (banData.removePlayerBan(targetUsername)) {
            source.sendFeedback(() -> Text.literal("§a已解除玩家 " + targetUsername + " 的封禁"), true);
            HeosLogger.info(source.getName() + " unbanned " + targetUsername);
            return 1;
        } else {
            source.sendError(Text.literal("§c玩家 " + targetUsername + " 没有被封禁"));
            return 0;
        }
    }

    private static int unbanIp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String targetIp = StringArgumentType.getString(context, "ip");

        BanData banData = Heos.getBanData();
        if (banData.removeIpBan(targetIp)) {
            source.sendFeedback(() -> Text.literal("§a已解除IP " + targetIp + " 的封禁"), true);
            HeosLogger.info(source.getName() + " unbanned IP " + targetIp);
            return 1;
        } else {
            source.sendError(Text.literal("§cIP " + targetIp + " 没有被封禁"));
            return 0;
        }
    }

    private static int listBans(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        BanData banData = Heos.getBanData();

        source.sendFeedback(() -> Text.literal("§e================================="), false);
        source.sendFeedback(() -> Text.literal("§e封禁列表 (Ban List)"), false);
        source.sendFeedback(() -> Text.literal("§e================================="), false);

        if (banData.playerBans.isEmpty() && banData.ipBans.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§7没有封禁记录"), false);
            return 1;
        }

        if (!banData.playerBans.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§6玩家封禁 (" + banData.playerBans.size() + "):"), false);
            for (BanData.BanEntry ban : banData.playerBans) {
                final String ti = TimeParser.formatExpiryTime(ban.expiryTime);
                source.sendFeedback(() -> Text.literal("§7- " + ban.username + " §8| §7" + ti + " §8| §7" + ban.reason), false);
            }
        }

        if (!banData.ipBans.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§6IP封禁 (" + banData.ipBans.size() + "):"), false);
            for (BanData.IpBanEntry ban : banData.ipBans) {
                final String ti = TimeParser.formatExpiryTime(ban.expiryTime);
                source.sendFeedback(() -> Text.literal("§7- " + ban.ip + " §8| §7" + ti + " §8| §7" + ban.reason), false);
            }
        }

        source.sendFeedback(() -> Text.literal("§e================================="), false);

        return 1;
    }
}
