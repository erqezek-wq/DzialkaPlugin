package pl.dzialka.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.managers.DzialkaManager;
import pl.dzialka.models.Dzialka;
import pl.dzialka.utils.ConfigManager;

import java.util.List;

public class NoteBlockListener implements Listener {

    private final DzialkaPlugin plugin;
    private final DzialkaManager manager;
    private final ConfigManager cfg;

    public NoteBlockListener(DzialkaPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDzialkaManager();
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNoteBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        // Sprawdź czy to noteblock i czy gracz NIE trzymał shifta
        if (block.getType() != Material.NOTE_BLOCK) return;
        if (player.isSneaking()) return;

        // Sprawdź uprawnienia
        if (!player.hasPermission("dzialka.use")) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_permission"));
            return;
        }

        // Sprawdź limit działek
        List<Dzialka> playerDzialki = manager.getPlayerDzialki(player.getUniqueId());
        int limit = manager.getPlayerLimit(player);

        if (playerDzialki.size() >= limit) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() +
                    cfg.getMessage("limit_reached").replace("{limit}", String.valueOf(limit)));
            return;
        }

        // Sprawdź WorldGuard
        if (manager.isInWorldGuardRegion(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("worldguard_conflict"));
            return;
        }

        // Tymczasowa działka do sprawdzenia nakładania
        String tempId = "temp";
        Dzialka tempDzialka = new Dzialka(
                tempId,
                "temp",
                player.getUniqueId(),
                player.getName(),
                block.getWorld().getName(),
                block.getX(), block.getZ()
        );

        // Sprawdź nakładanie z WorldGuard
        if (manager.dzialkaOverlapsWorldGuard(tempDzialka)) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("worldguard_conflict"));
            return;
        }

        // Sprawdź nakładanie z innymi działkami
        if (manager.hasOverlap(tempDzialka)) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("dzialka_conflict"));
            return;
        }

        // Wygeneruj nazwę działki
        String name = "Działka#" + (playerDzialki.size() + 1) + "_" + player.getName();

        // Utwórz działkę
        Dzialka dzialka = manager.createDzialka(player, name, block.getLocation());

        String msg = cfg.getMessage("created")
                .replace("{name}", dzialka.getName())
                .replace("{x}", String.valueOf(block.getX()))
                .replace("{z}", String.valueOf(block.getZ()));

        player.sendMessage(cfg.getPrefix() + msg);
        player.sendMessage(cfg.getPrefix() + "§7ID działki: §e" + dzialka.getId());
        player.sendMessage(cfg.getPrefix() + "§7Rozmiar: §e50x50 §7bloków");
        player.sendMessage(cfg.getPrefix() + "§7Użyj §e/dzialka rename " + dzialka.getId() + " <nowa_nazwa> §7aby zmienić nazwę.");
    }
}
