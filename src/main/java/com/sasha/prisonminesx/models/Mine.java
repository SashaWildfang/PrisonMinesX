package com.sasha.prisonminesx.models;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mine {
    private final String name;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    private int resetDelay = 0;
    private List<Integer> resetWarnings = new ArrayList<>();
    private boolean silent = false;
    private Location tpLocation;
    private Map<String, Double> composition = new HashMap<>();

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

    public void addComposition(String material, double chance) {
        this.composition.put(material, chance);
    }

    // --- Getters and Setters ---
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public int getResetDelay() { return resetDelay; }
    public void setResetDelay(int resetDelay) { this.resetDelay = resetDelay; }

    public List<Integer> getResetWarnings() { return resetWarnings; }
    public void setResetWarnings(List<Integer> resetWarnings) { this.resetWarnings = resetWarnings; }

    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }

    public Location getTpLocation() { return tpLocation; }
    public void setTpLocation(Location tpLocation) { this.tpLocation = tpLocation; }

    public Map<String, Double> getComposition() { return composition; }
}