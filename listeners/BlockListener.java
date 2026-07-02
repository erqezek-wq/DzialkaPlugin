package pl.dzialka.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.managers.DzialkaManager;
import pl.dzialka.models.Dzialka;
import pl.dzialka.utils.ConfigManager;

public class BlockListener implements Listener {

    private final DzialkaPlugin plugin;
    private final DzialkaManager manager;
    private final ConfigManager cfg;

    public BlockListener(DzialkaPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDzialkaManager();
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Dzialka dzialka = manager.getDzialkaAt(event.getBlock().getLocation());

        if (dzialka == null) return;

        // Admini mogą wszystko
        if (player.hasPermission("dzialka.admin")) return;

        if (!dzialka.isAuthorized(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_permission"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Dzialka dzialka = manager.getDzialkaAt(event.getBlockPlaced().getLocation());

        if (dzialka == null) return;
        if (player.hasPermission("dzialka.admin")) return;

        if (!dzialka.isAuthorized(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_permission"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Dzialka dzialka = manager.getDzialkaAt(victim.getLocation());
        if (dzialka == null) return;

        if (!dzialka.isPvpEnabled()) {
            event.setCancelled(true);
            attacker.sendMessage(cfg.getPrefix() + "§cPvP jest wyłączone na tej działce!");
        }
    }
}
