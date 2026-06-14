package com.sasha.prisonminesx.models;

import org.bukkit.Location;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mine {
    private final String name;
    private String worldName;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    // --- Core Flags & Defaults ---
    private String displayItem = "DIAMOND_PICKAXE";
    private int resetDelay = 600;
    private List<Integer> resetWarnings = Arrays.asList(600, 300, 60);
    private boolean silent = true;
    private Location tpLocation;
    private Map<String, Double> composition = new HashMap<>();

    // --- Premium Flags & Defaults ---
    private double resetPercentage = 20.0;
    private boolean fillMode = false;
    private String surface = null;
    private boolean teleportOnReset = true;
    private boolean hologramEnabled = false;
    private boolean actionbarEnabled = false;
    private boolean warnGlobal = true;
    private boolean paused = false;

    // --- Premium Analytics ---
    private long lifetimeMinedBlocks = 0;
    private int lifetimeResets = 0;

    // --- In-memory tracking ---
    private transient int totalBlocks = 0;
    private transient int minedBlocks = 0;
    private transient int timeUntilReset = 600;

    // --- Undo History Variables ---
    private transient int[] prevBounds = null;
    private transient String prevWorld = null;

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

    // --- Undo Management ---
    public void savePreviousBounds() {
        this.prevBounds = new int[]{minX, minY, minZ, maxX, maxY, maxZ};
        this.prevWorld = this.worldName;
    }

    public boolean hasPreviousBounds() {
        return prevBounds != null;
    }

    public void restorePreviousBounds() {
        if (prevBounds != null) {
            this.worldName = prevWorld;
            this.minX = prevBounds[0];
            this.minY = prevBounds[1];
            this.minZ = prevBounds[2];
            this.maxX = prevBounds[3];
            this.maxY = prevBounds[4];
            this.maxZ = prevBounds[5];
            this.prevBounds = null;
        }
    }

    public void incrementMinedBlocks() { this.minedBlocks++; }

    public double getPercentRemaining() {
        if (totalBlocks == 0) return 100.0;
        int remaining = totalBlocks - minedBlocks;
        return ((double) remaining / totalBlocks) * 100.0;
    }

    public int getTotalBlocks() { return totalBlocks; }
    public int getMinedBlocks() { return minedBlocks; }

    public void addComposition(String material, double chance) { this.composition.put(material, chance); }
    public void setComposition(Map<String, Double> composition) { this.composition = composition; }

    // --- Getters ---
    public String getName() { return name; }
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
    public int getTimeUntilReset() { return timeUntilReset; }

    public boolean isFillMode() { return fillMode; }
    public String getSurface() { return surface; }
    public boolean isTeleportOnReset() { return teleportOnReset; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public boolean isActionbarEnabled() { return actionbarEnabled; }
    public boolean isWarnGlobal() { return warnGlobal; }
    public boolean isPaused() { return paused; }

    public long getLifetimeMinedBlocks() { return lifetimeMinedBlocks; }
    public int getLifetimeResets() { return lifetimeResets; }

    // --- Setters ---
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
    public void setActionbarEnabled(boolean actionbarEnabled) { this.actionbarEnabled = actionbarEnabled; }
    public void setWarnGlobal(boolean warnGlobal) { this.warnGlobal = warnGlobal; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public void setLifetimeMinedBlocks(long lifetimeMinedBlocks) { this.lifetimeMinedBlocks = lifetimeMinedBlocks; }
    public void setLifetimeResets(int lifetimeResets) { this.lifetimeResets = lifetimeResets; }
    public void incrementLifetimeMinedBlocks() { this.lifetimeMinedBlocks++; }
    public void incrementLifetimeResets() { this.lifetimeResets++; }
}