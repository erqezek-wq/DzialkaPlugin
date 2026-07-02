package pl.dzialka.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.managers.DzialkaManager;
import pl.dzialka.models.Dzialka;
import pl.dzialka.utils.ConfigManager;

import java.util.Set;

public class InteractListener implements Listener {

    private final DzialkaPlugin plugin;
    private final DzialkaManager manager;
    private final ConfigManager cfg;

    // Bloki wymagające autoryzacji do interakcji
    private static final Set<Material> PROTECTED_INTERACT = Set.of(
            // Skrzynki
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.ENDER_CHEST,
            Material.BARREL,
            Material.SHULKER_BOX,
            // Metalowe półpłytki
            Material.IRON_TRAPDOOR,
            // Kamienne przyciski
            Material.STONE_BUTTON,
            Material.POLISHED_BLACKSTONE_BUTTON,
            // Inne chronione
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.BREWING_STAND,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.ENCHANTING_TABLE,
            Material.DISPENSER,
            Material.DROPPER,
            Material.HOPPER,
            Material.JUKEBOX,
            Material.LECTERN,
            Material.LOOM,
            Material.STONECUTTER,
            Material.GRINDSTONE,
            Material.CARTOGRAPHY_TABLE,
            Material.SMITHING_TABLE,
            Material.COMPOSTER,
            Material.BEEHIVE,
            Material.BEE_NEST
    );

    // Metalowe półpłytki - chronione (tylko interakcja fizyczna/klikanie)
    private static final Set<Material> IRON_TRAPDOORS = Set.of(
            Material.IRON_TRAPDOOR
    );

    // Drewniane przyciski - DOZWOLONE (nie chronione)
    private static final Set<Material> WOODEN_BUTTONS = Set.of(
            Material.OAK_BUTTON,
            Material.SPRUCE_BUTTON,
            Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON,
            Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON,
            Material.MANGROVE_BUTTON,
            Material.CHERRY_BUTTON,
            Material.BAMBOO_BUTTON,
            Material.CRIMSON_BUTTON,
            Material.WARPED_BUTTON
    );

    public InteractListener(DzialkaPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDzialkaManager();
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK &&
            event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Material type = block.getType();

        // Drewniane przyciski - zawsze dozwolone
        if (WOODEN_BUTTONS.contains(type)) return;

        // Sprawdź czy blok jest chroniony
        boolean isProtected = PROTECTED_INTERACT.contains(type);

        // Kamienne przyciski specjalnie
        if (type == Material.STONE_BUTTON || type == Material.POLISHED_BLACKSTONE_BUTTON) {
            isProtected = true;
        }

        // Metalowe półpłytki
        if (type == Material.IRON_TRAPDOOR) {
            isProtected = true;
        }

        if (!isProtected) return;

        Dzialka dzialka = manager.getDzialkaAt(block.getLocation());
        if (dzialka == null) return;

        if (player.hasPermission("dzialka.admin")) return;

        if (!dzialka.isAuthorized(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_permission"));
        }
    }
}
