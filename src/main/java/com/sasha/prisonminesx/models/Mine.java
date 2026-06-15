package com.sasha.prisonminesx.models;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Mine {

    public static class PlayerRecord {
        public long enteredAt;
        public int blocksMined;
        public long lastMined;
        public Map<String, Integer> specificBlocksMined;

        public PlayerRecord(long enteredAt) {
            this.enteredAt = enteredAt;
            this.blocksMined = 0;
            this.lastMined = 0;
            this.specificBlocksMined = new HashMap<>();
        }
    }

    private String name;
    private String description = null;
    private String worldName;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    // --- Core Flags & Defaults ---
    private String displayItem = "DIAMOND_PICKAXE";
    private int resetDelay = 600;
    private List<Integer> resetWarnings = new ArrayList<>(Arrays.asList(600, 300, 60));
    private boolean silent = true;
    private Location tpLocation;
    private Map<String, Double> composition = new HashMap<>();

    // --- Premium Flags & Defaults ---
    private double resetPercentage = 20.0;
    private boolean fillMode = false;
    private String surface = null;
    private boolean teleportOnReset = true;
    private boolean hologramEnabled = false;
    private Location hologramLocation = null;
    private boolean actionbarEnabled = false;
    private String warnMode = "GLOBAL";
    private boolean paused = false;
    private String schematic = null;
    private String resetStyle = "BOTTOM_UP";
    private List<String> resetSchedules = new ArrayList<>();

    // NEW REGION FLAGS
    private boolean mineFly = false;
    private boolean hunger = false;
    private boolean fallDamage = false;
    private boolean pvp = false;
    private boolean placeBlocks = false;
    private int warpDelay = -1;

    // --- Premium Analytics ---
    private long lifetimeMinedBlocks = 0;
    private int lifetimeResets = 0;

    // --- In-memory tracking ---
    private transient int totalBlocks = 0;
    private transient int minedBlocks = 0;
    private transient int timeUntilReset = 600;
    private transient long hologramFlashUntil = 0;
    private transient final Map<UUID, PlayerRecord> activePlayers = new ConcurrentHashMap<>();

    // --- Undo History Variables ---
    private transient int[] prevBounds = null;
    private transient String prevWorld = null;
    private transient String prevSchematic = null;

    public Mine(String name, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public boolean isSetup() { return worldName != null; }

    public boolean intersects(Mine other) {
        if (!this.worldName.equals(other.getWorldName())) return false;
        return (this.minX <= other.getMaxX() && this.maxX >= other.getMinX()) &&
                (this.minY <= other.getMaxY() && this.maxY >= other.getMinY()) &&
                (this.minZ <= other.getMaxZ() && this.maxZ >= other.getMinZ());
    }

    public void calculateTotalBlocks() {
        this.totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        this.minedBlocks = 0;
        this.timeUntilReset = this.resetDelay;
    }

    public void savePreviousBounds() {
        this.prevBounds = new int[]{minX, minY, minZ, maxX, maxY, maxZ};
        this.prevWorld = this.worldName;
        this.prevSchematic = this.schematic;
    }

    public boolean hasPreviousBounds() { return prevBounds != null; }

    public void restorePreviousBounds() {
        if (prevBounds != null) {
            this.worldName = prevWorld;
            this.minX = prevBounds[0];
            this.minY = prevBounds[1];
            this.minZ = prevBounds[2];
            this.maxX = prevBounds[3];
            this.maxY = prevBounds[4];
            this.maxZ = prevBounds[5];
            this.schematic = prevSchematic;
            this.prevBounds = null;
        }
    }

    public double getPercentRemaining() {
        if (totalBlocks == 0) return 100.0;
        int remaining = totalBlocks - minedBlocks;
        return ((double) remaining / totalBlocks) * 100.0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public String getDisplayItem() { return displayItem; }
    public int getResetDelay() { return resetDelay; }
    public List<Integer> getResetWarnings() { return resetWarnings; }
    public boolean isSilent() { return silent; }
    public Location getTpLocation() { return tpLocation; }
    public Map<String, Double> getComposition() { return composition; }
    public double getResetPercentage() { return resetPercentage; }
    public boolean isFillMode() { return fillMode; }
    public String getSurface() { return surface; }
    public boolean isTeleportOnReset() { return teleportOnReset; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public Location getHologramLocation() { return hologramLocation; }
    public boolean isActionbarEnabled() { return actionbarEnabled; }
    public String getWarnMode() { return warnMode == null ? "GLOBAL" : warnMode; }
    public boolean isPaused() { return paused; }
    public String getSchematic() { return schematic; }
    public String getResetStyle() { return resetStyle == null ? "BOTTOM_UP" : resetStyle; }
    public List<String> getResetSchedules() { return resetSchedules; }
    public boolean isMineFly() { return mineFly; }
    public boolean isHunger() { return hunger; }
    public boolean isFallDamage() { return fallDamage; }
    public boolean isPvp() { return pvp; }
    public boolean isPlaceBlocks() { return placeBlocks; }
    public int getWarpDelay() { return warpDelay; }

    public int getTotalBlocks() { return totalBlocks; }
    public int getMinedBlocks() { return minedBlocks; }
    public int getTimeUntilReset() { return timeUntilReset; }
    public long getLifetimeMinedBlocks() { return lifetimeMinedBlocks; }
    public int getLifetimeResets() { return lifetimeResets; }
    public long getHologramFlashUntil() { return hologramFlashUntil; }
    public Map<UUID, PlayerRecord> getActivePlayers() { return activePlayers; }

    public void incrementMinedBlocks() { this.minedBlocks++; }
    public void addComposition(String material, double chance) { this.composition.put(material, chance); }
    public void setComposition(Map<String, Double> composition) { this.composition = composition; }
    public void addActivePlayer(UUID uuid) { activePlayers.putIfAbsent(uuid, new PlayerRecord(System.currentTimeMillis())); }
    public void removeActivePlayer(UUID uuid) { activePlayers.remove(uuid); }

    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setMinX(int minX) { this.minX = minX; }
    public void setMinY(int minY) { this.minY = minY; }
    public void setMinZ(int minZ) { this.minZ = minZ; }
    public void setMaxX(int maxX) { this.maxX = maxX; }
    public void setMaxY(int maxY) { this.maxY = maxY; }
    public void setMaxZ(int maxZ) { this.maxZ = maxZ; }

    public void setDisplayItem(String displayItem) { this.displayItem = displayItem; }
    public void setResetDelay(int resetDelay) { this.resetDelay = resetDelay; }
    public void setResetWarnings(List<Integer> resetWarnings) { this.resetWarnings = resetWarnings; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public void setTpLocation(Location tpLocation) { this.tpLocation = tpLocation; }
    public void setResetPercentage(double resetPercentage) { this.resetPercentage = resetPercentage; }
    public void setTimeUntilReset(int timeUntilReset) { this.timeUntilReset = timeUntilReset; }
    public void setFillMode(boolean fillMode) { this.fillMode = fillMode; }
    public void setSurface(String surface) { this.surface = surface; }
    public void setTeleportOnReset(boolean teleportOnReset) { this.teleportOnReset = teleportOnReset; }
    public void setHologramEnabled(boolean hologramEnabled) { this.hologramEnabled = hologramEnabled; }
    public void setHologramLocation(Location hologramLocation) { this.hologramLocation = hologramLocation; }
    public void setActionbarEnabled(boolean actionbarEnabled) { this.actionbarEnabled = actionbarEnabled; }
    public void setWarnMode(String warnMode) { this.warnMode = warnMode; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public void setSchematic(String schematic) { this.schematic = schematic; }
    public void setResetStyle(String resetStyle) { this.resetStyle = resetStyle; }
    public void setResetSchedules(List<String> resetSchedules) { this.resetSchedules = resetSchedules; }
    public void setMineFly(boolean mineFly) { this.mineFly = mineFly; }
    public void setHunger(boolean hunger) { this.hunger = hunger; }
    public void setFallDamage(boolean fallDamage) { this.fallDamage = fallDamage; }
    public void setPvp(boolean pvp) { this.pvp = pvp; }
    public void setPlaceBlocks(boolean placeBlocks) { this.placeBlocks = placeBlocks; }
    public void setWarpDelay(int warpDelay) { this.warpDelay = warpDelay; }
    public void setLifetimeMinedBlocks(long lifetimeMinedBlocks) { this.lifetimeMinedBlocks = lifetimeMinedBlocks; }
    public void setLifetimeResets(int lifetimeResets) { this.lifetimeResets = lifetimeResets; }
    public void setHologramFlashUntil(long hologramFlashUntil) { this.hologramFlashUntil = hologramFlashUntil; }
    public void incrementLifetimeMinedBlocks() { this.lifetimeMinedBlocks++; }
    public void incrementLifetimeResets() { this.lifetimeResets++; }
}