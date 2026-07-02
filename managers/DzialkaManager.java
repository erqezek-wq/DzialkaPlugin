package pl.dzialka.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dzialka.DzialkaPlugin;
import pl.dzialka.models.Dzialka;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DzialkaManager {

    private final DzialkaPlugin plugin;
    private final Map<String, Dzialka> dzialki = new HashMap<>(); // id -> Dzialka
    private File dataFile;
    private FileConfiguration dataConfig;

    public DzialkaManager(DzialkaPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ==================== CRUD ====================

    public Dzialka createDzialka(Player owner, String name, Location center) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Dzialka dzialka = new Dzialka(
                id, name,
                owner.getUniqueId(), owner.getName(),
                center.getWorld().getName(),
                center.getBlockX(), center.getBlockZ()
        );
        dzialki.put(id, dzialka);
        saveAll();
        return dzialka;
    }

    public boolean removeDzialka(String id) {
        if (dzialki.containsKey(id)) {
            dzialki.remove(id);
            saveAll();
            return true;
        }
        return false;
    }

    // ==================== Zapytania ====================

    public Dzialka getDzialkaAt(Location location) {
        for (Dzialka d : dzialki.values()) {
            if (d.contains(location)) return d;
        }
        return null;
    }

    public List<Dzialka> getPlayerDzialki(UUID playerUUID) {
        return dzialki.values().stream()
                .filter(d -> d.isOwner(playerUUID))
                .collect(Collectors.toList());
    }

    public Dzialka getDzialkaByName(UUID ownerUUID, String name) {
        return dzialki.values().stream()
                .filter(d -> d.isOwner(ownerUUID) && d.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Dzialka getDzialkaByIdOrName(UUID ownerUUID, String input) {
        // Szukaj po ID
        if (dzialki.containsKey(input)) {
            Dzialka d = dzialki.get(input);
            if (d.isOwner(ownerUUID)) return d;
        }
        // Szukaj po nazwie
        return getDzialkaByName(ownerUUID, input);
    }

    public boolean hasOverlap(Dzialka newDzialka) {
        for (Dzialka existing : dzialki.values()) {
            if (existing.overlaps(newDzialka)) return true;
        }
        return false;
    }

    public int getPlayerLimit(Player player) {
        if (player.hasPermission("dzialka.svip")) {
            return plugin.getConfigManager().getSvipLimit();
        } else if (player.hasPermission("dzialka.vip")) {
            return plugin.getConfigManager().getVipLimit();
        }
        return plugin.getConfigManager().getDefaultLimit();
    }

    // ==================== WorldGuard ====================

    public boolean isInWorldGuardRegion(Location location) {
        if (!plugin.isWorldGuardEnabled()) return false;
        try {
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager == null) return false;

            com.sk89q.worldedit.math.BlockVector3 pos =
                    BukkitAdapter.asBlockVector(location);

            List<ProtectedRegion> regions = new ArrayList<>(
                    regionManager.getApplicableRegions(pos).getRegions()
            );

            // Wyklucz globalny region "__global__"
            regions.removeIf(r -> r.getId().equals("__global__"));

            return !regions.isEmpty();
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd sprawdzania WorldGuard: " + e.getMessage());
            return false;
        }
    }

    // Sprawdza czy cały obszar działki nie nakłada się z żadnym WG regionem
    public boolean dzialkaOverlapsWorldGuard(Dzialka dzialka) {
        if (!plugin.isWorldGuardEnabled()) return false;
        try {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(dzialka.getWorldName());
            if (world == null) return false;

            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (regionManager == null) return false;

            com.sk89q.worldedit.math.BlockVector3 min =
                    com.sk89q.worldedit.math.BlockVector3.at(dzialka.getMinX(), 0, dzialka.getMinZ());
            com.sk89q.worldedit.math.BlockVector3 max =
                    com.sk89q.worldedit.math.BlockVector3.at(dzialka.getMaxX(), 255, dzialka.getMaxZ());

            com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion testRegion =
                    new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(
                            "test_" + dzialka.getId(), min, max
                    );

            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (region.getId().equals("__global__")) continue;
                if (region.intersectsAnyOf(testRegion.getPoints())) return true;
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd sprawdzania nakładania WG: " + e.getMessage());
            return false;
        }
    }

    // ==================== Zapis / Odczyt ====================

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(),
                plugin.getConfig().getString("data-file", "dzialki.yml"));
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć pliku danych: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveAll() {
        if (dataFile == null) setupDataFile();
        dataConfig.set("dzialki", null); // Wyczyść

        for (Dzialka d : dzialki.values()) {
            String path = "dzialki." + d.getId();
            dataConfig.set(path + ".name", d.getName());
            dataConfig.set(path + ".owner-uuid", d.getOwnerUUID().toString());
            dataConfig.set(path + ".owner-name", d.getOwnerName());
            dataConfig.set(path + ".world", d.getWorldName());
            dataConfig.set(path + ".center-x", d.getCenterX());
            dataConfig.set(path + ".center-z", d.getCenterZ());
            dataConfig.set(path + ".pvp", d.isPvpEnabled());
            dataConfig.set(path + ".created-at", d.getCreatedAt());

            List<String> membersList = d.getMembers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
            dataConfig.set(path + ".members", membersList);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd zapisu działek: " + e.getMessage());
        }
    }

    private void loadData() {
        setupDataFile();
        if (!dataConfig.contains("dzialki")) return;

        for (String id : dataConfig.getConfigurationSection("dzialki").getKeys(false)) {
            String path = "dzialki." + id;
            try {
                String name = dataConfig.getString(path + ".name");
                UUID ownerUUID = UUID.fromString(dataConfig.getString(path + ".owner-uuid"));
                String ownerName = dataConfig.getString(path + ".owner-name");
                String world = dataConfig.getString(path + ".world");
                int cx = dataConfig.getInt(path + ".center-x");
                int cz = dataConfig.getInt(path + ".center-z");
                boolean pvp = dataConfig.getBoolean(path + ".pvp", false);
                long createdAt = dataConfig.getLong(path + ".created-at", System.currentTimeMillis());

                List<String> memberStrings = dataConfig.getStringList(path + ".members");
                Set<UUID> members = new HashSet<>();
                for (String m : memberStrings) {
                    try { members.add(UUID.fromString(m)); }
                    catch (Exception ignored) {}
                }

                Dzialka dzialka = new Dzialka(id, name, ownerUUID, ownerName,
                        world, cx, cz, pvp, members, createdAt);
                dzialki.put(id, dzialka);

            } catch (Exception e) {
                plugin.getLogger().warning("Błąd ładowania działki " + id + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Załadowano " + dzialki.size() + " działek.");
    }

    public Collection<Dzialka> getAllDzialki() {
        return dzialki.values();
    }
}
