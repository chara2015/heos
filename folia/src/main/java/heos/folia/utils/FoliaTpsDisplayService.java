package heos.folia.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FoliaTpsDisplayService {
    private final Plugin plugin;
    private final Map<UUID, Player> activePlayers = new ConcurrentHashMap<>();
    private int ticksUntilUpdate;

    public FoliaTpsDisplayService(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> tick(), 20L, 20L);
    }

    public void start(Player player) {
        if (!plugin.getConfig().getBoolean("enableAutoLogTps", true)) {
            return;
        }
        activePlayers.put(player.getUniqueId(), player);
    }

    public void stop(Player player) {
        activePlayers.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.setPlayerListFooter("");
        }
    }

    public void close() {
        for (Player player : activePlayers.values()) {
            if (player.isOnline()) {
                player.setPlayerListFooter("");
            }
        }
        activePlayers.clear();
    }

    private void tick() {
        if (activePlayers.isEmpty() || !plugin.getConfig().getBoolean("enableAutoLogTps", true)) {
            return;
        }
        int delaySeconds = Math.max(1, plugin.getConfig().getInt("autoLogTpsDelayTicks", 20) / 20);
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = delaySeconds - 1;
        String footer = footer();
        for (Map.Entry<UUID, Player> entry : activePlayers.entrySet()) {
            Player player = entry.getValue();
            if (!player.isOnline()) {
                activePlayers.remove(entry.getKey());
                continue;
            }
            player.getScheduler().run(plugin, task -> player.setPlayerListFooter(footer), null);
        }
    }

    private static String footer() {
        double tps = currentTps();
        ChatColor color = color(tps);
        double mspt = tps <= 0.0D ? 50.0D : Math.min(999.9D, 1000.0D / tps);
        return ChatColor.GRAY + "TPS: " + color + String.format(java.util.Locale.ROOT, "%.1f", tps)
                + ChatColor.GRAY + " MSPT: " + color + String.format(java.util.Locale.ROOT, "%.1f", mspt);
    }

    private static double currentTps() {
        try {
            Method method = Bukkit.class.getMethod("getTPS");
            Object value = method.invoke(null);
            if (value instanceof double[] tpsValues && tpsValues.length > 0) {
                return Math.min(20.0D, tpsValues[0]);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 20.0D;
    }

    private static ChatColor color(double tps) {
        if (tps < 10.0D) {
            return ChatColor.RED;
        }
        if (tps < 18.0D) {
            return ChatColor.YELLOW;
        }
        return ChatColor.GREEN;
    }
}
