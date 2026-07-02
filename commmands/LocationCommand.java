package pl.dzialka.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.models.Dzialka;
import pl.dzialka.utils.ConfigManager;

import java.util.List;

public class LocationCommand implements CommandExecutor {

    private final DzialkaPlugin plugin;
    private final ConfigManager cfg;

    public LocationCommand(DzialkaPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        List<Dzialka> dzialki = plugin.getDzialkaManager().getPlayerDzialki(player.getUniqueId());

        if (dzialki.isEmpty()) {
            player.sendMessage(cfg.getPrefix() + cfg.getMessage("no_plots"));
            return true;
        }

        player.sendMessage(cfg.getMessage("location_header"));

        for (Dzialka d : dzialki) {
            String entry = cfg.getMessage("location_entry")
                    .replace("{name}", d.getName())
                    .replace("{x}", String.valueOf(d.getCenterX()))
                    .replace("{z}", String.valueOf(d.getCenterZ()))
                    .replace("{world}", d.getWorldName());
            player.sendMessage(entry);
        }

        player.sendMessage("§8§m----------------------------------------");
        player.sendMessage("§7Łącznie działek: §e" + dzialki.size() +
                "§7/§e" + plugin.getDzialkaManager().getPlayerLimit(player));

        return true;
    }
}
