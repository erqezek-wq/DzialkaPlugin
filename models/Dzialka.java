package pl.dzialka.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class Dzialka {

    private final String id;
    private String name;
    private final UUID ownerUUID;
    private String ownerName;
    private final String worldName;

    // Centrum (gdzie postawiono noteblock)
    private final int centerX;
    private final int centerZ;

    // Granice działki
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    private boolean pvpEnabled;
    private final Set<UUID> members;
    private final long createdAt;

    private static final int HALF_SIZE = 25; // 50/2

    public Dzialka(String id, String name, UUID ownerUUID, String ownerName,
                   String worldName, int centerX, int centerZ) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minX = centerX - HALF_SIZE;
        this.maxX = centerX + HALF_SIZE;
        this.minZ = centerZ - HALF_SIZE;
        this.maxZ = centerZ + HALF_SIZE;
        this.pvpEnabled = false;
        this.members = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Konstruktor do ładowania z pliku
    public Dzialka(String id, String name, UUID ownerUUID, String ownerName,
                   String worldName, int centerX, int centerZ,
                   boolean pvpEnabled, Set<UUID> members, long createdAt) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minX = centerX - HALF_SIZE;
        this.maxX = centerX + HALF_SIZE;
        this.minZ = centerZ - HALF_SIZE;
        this.maxZ = centerZ + HALF_SIZE;
        this.pvpEnabled = pvpEnabled;
        this.members = members != null ? new HashSet<>(members) : new HashSet<>();
        this.createdAt = createdAt;
    }

    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) return false;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Dzialka other) {
        if (!other.worldName.equals(this.worldName)) return false;
        return !(other.maxX < this.minX || other.minX > this.maxX ||
                 other.maxZ < this.minZ || other.minZ > this.maxZ);
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid) || isOwner(uuid);
    }

    public boolean isAuthorized(UUID uuid) {
        return isOwner(uuid) || members.contains(uuid);
    }

    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }

    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, centerX, 64, centerZ);
    }

    // Gettery
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public String getWorldName() { return worldName; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public long getCreatedAt() { return createdAt; }
}
