package pl.dzialka.managers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.models.Dzialka;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VisualizationManager {

    private final DzialkaPlugin plugin;
    private final Set<UUID> activeVisualizations = new HashSet<>();

    public VisualizationManager(DzialkaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showBorder(Player player, Dzialka dzialka) {
        UUID uuid = player.getUniqueId();

        if (activeVisualizations.contains(uuid)) {
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") +
                    "§eWizualizacja jest już aktywna!");
            return;
        }

        activeVisualizations.add(uuid);

        int duration = plugin.getConfig().getInt("settings.visualization_duration", 200);
        double spacing = plugin.getConfig().getDouble("settings.visualization_particle_spacing", 2.0);

        World world = plugin.getServer().getWorld(dzialka.getWorldName());
        if (world == null) {
            activeVisualizations.remove(uuid);
            return;
        }

        // Cząsteczki pokazywane co tick przez określony czas
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    activeVisualizations.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks % 10 == 0) { // Odświeżaj co 10 ticków (0.5s)
                    drawBorder(player, dzialka, world, spacing);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        String msg = plugin.getConfigManager().getMessage("visualization_start")
                .replace("{name}", dzialka.getName());
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + msg);
    }

    private void drawBorder(Player player, Dzialka dzialka, World world, double spacing) {
        int y = player.getLocation().getBlockY();
        int minX = dzialka.getMinX();
        int maxX = dzialka.getMaxX();
        int minZ = dzialka.getMinZ();
        int maxZ = dzialka.getMaxZ();

        Particle.DustOptions dustOwner = new Particle.DustOptions(Color.GREEN, 1.5f);
        Particle.DustOptions dustBorder = new Particle.DustOptions(Color.RED, 1.5f);

        // Narożniki zaznaczone na zielono
        spawnCornerParticles(player, world, minX, y, minZ, dustOwner);
        spawnCornerParticles(player, world, maxX, y, minZ, dustOwner);
        spawnCornerParticles(player, world, minX, y, maxZ, dustOwner);
        spawnCornerParticles(player, world, maxX, y, maxZ, dustOwner);

        // Linie na czerwono
        // Linia X (minZ i maxZ)
        for (double x = minX; x <= maxX; x += spacing) {
            spawnParticle(player, world, x, y, minZ, dustBorder);
            spawnParticle(player, world, x, y, maxZ, dustBorder);
        }
        // Linia Z (minX i maxX)
        for (double z = minZ; z <= maxZ; z += spacing) {
            spawnParticle(player, world, minX, y, z, dustBorder);
            spawnParticle(player, world, maxX, y, z, dustBorder);
        }
    }

    private void spawnCornerParticles(Player player, World world, int x, int y, int z,
                                       Particle.DustOptions dust) {
        for (int dy = 0; dy <= 3; dy++) {
            spawnParticle(player, world, x, y + dy, z, dust);
        }
    }

    private void spawnParticle(Player player, World world, double x, int y, double z,
                                Particle.DustOptions dust) {
        Location loc = new Location(world, x, y + 1, z);
        // Pokazuj tylko jeśli gracz jest blisko (64 bloki)
        if (player.getLocation().distanceSquared(loc) <= 4096) {
            player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }
}
