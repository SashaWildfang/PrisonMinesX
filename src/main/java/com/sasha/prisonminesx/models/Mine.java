package com.sasha.prisonminesx.models;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mine {
    private final String name;
    private String worldName;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    private int resetDelay = 0;
    private List<Integer> resetWarnings = new ArrayList<>();
    private boolean silent = false;
    private Location tpLocation;
    private Map<String, Double> composition = new HashMap<>();

    // Constructor for creating a brand new mine
    public Mine(String name, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = name;
        this.worldName = worldName;
        // Automatically calculate minimums and maximums so boundaries are never inverted
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    // Quick validation check before we allow FAWE to attempt a reset
    public boolean isSetup() {
        return worldName != null && !composition.isEmpty();
    }

    // --- Data Management ---
    public void addComposition(String material, double chance) {
        this.composition.put(material, chance);
    }

    public void setComposition(Map<String, Double> composition) {
        this.composition = composition;
    }

    // --- Getters ---
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public int getResetDelay() { return resetDelay; }
    public List<Integer> getResetWarnings() { return resetWarnings; }
    public boolean isSilent() { return silent; }
    public Location getTpLocation() { return tpLocation; }
    public Map<String, Double> getComposition() { return composition; }

    // --- Setters (Needed for SQL/YAML Database Loading) ---
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public void setMinX(int minX) { this.minX = minX; }
    public void setMinY(int minY) { this.minY = minY; }
    public void setMinZ(int minZ) { this.minZ = minZ; }
    public void setMaxX(int maxX) { this.maxX = maxX; }
    public void setMaxY(int maxY) { this.maxY = maxY; }
    public void setMaxZ(int maxZ) { this.maxZ = maxZ; }

    public void setResetDelay(int resetDelay) { this.resetDelay = resetDelay; }
    public void setResetWarnings(List<Integer> resetWarnings) { this.resetWarnings = resetWarnings; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public void setTpLocation(Location tpLocation) { this.tpLocation = tpLocation; }
}