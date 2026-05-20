package heos.folia.event;

import heos.folia.commands.FoliaBanCommands;
import heos.folia.storage.FoliaBanData;
import heos.folia.storage.FoliaWhitelistData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import heos.folia.utils.FoliaMessages;
import heos.folia.utils.FoliaMojangApi;
import heos.folia.utils.FoliaTimeParser;

public final class FoliaAuthListener implements Listener {
    private final Plugin plugin;
    private final FoliaAuthService authService;
    private final FoliaBanData banData;
    private final FoliaWhitelistData whitelistData;

    public FoliaAuthListener(Plugin plugin, FoliaAuthService authService, FoliaBanData banData, FoliaWhitelistData whitelistData) {
        this.plugin = plugin;
        this.authService = authService;
        this.banData = banData;
        this.whitelistData = whitelistData;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        boolean allowMoreCharacters = plugin.getConfig().getBoolean("allowMoreOfflineUsernameCharacters", true);
        if (!FoliaMojangApi.isValidMojangUsername(username) && !FoliaMojangApi.isAllowedOfflineUsername(username, allowMoreCharacters)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, FoliaMessages.offlineNameHint());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onLogin(PlayerLoginEvent event) {
        if (!authService.areOfflinePlayersAllowed() && !isPremiumUuid(event.getPlayer())) {
            plugin.getLogger().info("Offline player is not allowed: " + event.getPlayer().getName());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, FoliaMessages.offlineNameHint());
            return;
        }
        if (plugin.getConfig().getBoolean("enableWhitelist", false) && !whitelistData.isWhitelisted(event.getPlayer().getName())) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, FoliaMessages.whitelistKick());
            plugin.getLogger().info(FoliaMessages.whitelistDeniedLog(event.getPlayer().getName()));
            return;
        }
        if (plugin.getConfig().getBoolean("enableCustomBan", true)) {
            FoliaBanData.BanEntry playerBan = banData.getPlayerBan(event.getPlayer().getName(), event.getPlayer().getUniqueId());
            if (playerBan != null) {
                if (FoliaMessages.isMigrationReason(playerBan.reason)) {
                    plugin.getLogger().info(FoliaMessages.migrationBanAttemptLog(event.getPlayer().getName()));
                }
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, FoliaMessages.banMessage(playerBan.reason, FoliaTimeParser.formatAbsolute(playerBan.expiryTime)));
                return;
            }
            String ip = event.getAddress() == null ? "" : event.getAddress().getHostAddress();
            FoliaBanData.IpBanEntry ipBan = banData.getIpBan(ip);
            if (ipBan != null) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, FoliaMessages.banIpMessage(ipBan.reason, FoliaTimeParser.formatAbsolute(ipBan.expiryTime)));
            }
        }
    }

    private static boolean isPremiumUuid(Player player) {
        java.util.UUID offline = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return !player.getUniqueId().equals(offline);
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> authService.prepare(player), null, 1L);
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        authService.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onCommand(PlayerCommandPreprocessEvent event) {
        if (!authService.shouldBlock(event.getPlayer())) {
            return;
        }
        String command = event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage();
        if (!authService.canRunCommandWhileLocked(command)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + FoliaMessages.authPromptLogin());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onCommandSend(PlayerCommandSendEvent event) {
        if (!authService.isAuthenticationEnabled()) {
            return;
        }
        if (!event.getPlayer().hasPermission("heos.admin")) {
            event.getCommands().remove("heos");
            event.getCommands().remove("ban");
            event.getCommands().remove("ban-ip");
            event.getCommands().remove("unban");
            event.getCommands().remove("unban-ip");
            event.getCommands().remove("banlist");
        }
        if (!authService.isAuthenticated(event.getPlayer())) {
            event.getCommands().remove("changepassword");
            event.getCommands().remove("changepw");
        } else {
            event.getCommands().remove("login");
            event.getCommands().remove("l");
            event.getCommands().remove("register");
            event.getCommands().remove("reg");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onChat(AsyncPlayerChatEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + FoliaMessages.authPromptLogin());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onMove(PlayerMoveEvent event) {
        if (authService.shouldBlock(event.getPlayer()) && event.getFrom().distanceSquared(event.getTo()) > 0.0001D) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onInteract(PlayerInteractEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onBreak(BlockBreakEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onPlace(BlockPlaceEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onDrop(PlayerDropItemEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onHeld(PlayerItemHeldEvent event) {
        if (authService.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && authService.shouldBlock(player)) {
            event.setCancelled(true);
        }
    }
}
