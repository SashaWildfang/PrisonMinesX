package com.sasha.prisonminesx.utils;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MineSerializer {

    /**
     * Converts a Mine object into raw YAML data.
     */
    public static void serializeToYaml(Mine mine, ConfigurationSection config) {
        // Core & Boundaries
        config.set("world", mine.getWorldName());
        config.set("bounds.minX", mine.getMinX());
        config.set("bounds.minY", mine.getMinY());
        config.set("bounds.minZ", mine.getMinZ());
        config.set("bounds.maxX", mine.getMaxX());
        config.set("bounds.maxY", mine.getMaxY());
        config.set("bounds.maxZ", mine.getMaxZ());

        // Settings
        config.set("settings.reset-delay", mine.getResetDelay());
        config.set("settings.reset-warnings", mine.getResetWarnings());
        config.set("settings.silent", mine.isSilent());

        // Teleport Location (Null-safe)
        if (mine.getTpLocation() != null) {
            Location loc = mine.getTpLocation();
            config.set("teleport.x", loc.getX());
            config.set("teleport.y", loc.getY());
            config.set("teleport.z", loc.getZ());
            config.set("teleport.yaw", loc.getYaw());
            config.set("teleport.pitch", loc.getPitch());
        }

        // Composition
        config.set("composition", null); // Wipe the old composition section
        if (mine.getComposition() != null && !mine.getComposition().isEmpty()) {
            for (Map.Entry<String, Double> entry : mine.getComposition().entrySet()) {
                config.set("composition." + entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Rebuilds a Mine object from raw YAML data.
     */
    public static Mine deserializeFromYaml(String mineName, ConfigurationSection config) {
        String worldName = config.getString("world");

        // Boundaries
        int minX = config.getInt("bounds.minX");
        int minY = config.getInt("bounds.minY");
        int minZ = config.getInt("bounds.minZ");
        int maxX = config.getInt("bounds.maxX");
        int maxY = config.getInt("bounds.maxY");
        int maxZ = config.getInt("bounds.maxZ");

        // Instantiate the base mine
        Mine mine = new Mine(mineName, worldName, minX, minY, minZ, maxX, maxY, maxZ);

        // Settings
        mine.setResetDelay(config.getInt("settings.reset-delay", 0));
        mine.setResetWarnings(config.getIntegerList("settings.reset-warnings"));
        mine.setSilent(config.getBoolean("settings.silent", false));

        // Teleport Location
        if (config.contains("teleport.x")) {
            World world = Bukkit.getWorld(worldName);
            double x = config.getDouble("teleport.x");
            double y = config.getDouble("teleport.y");
            double z = config.getDouble("teleport.z");
            float yaw = (float) config.getDouble("teleport.yaw");
            float pitch = (float) config.getDouble("teleport.pitch");

            mine.setTpLocation(new Location(world, x, y, z, yaw, pitch));
        }

        // Composition
        ConfigurationSection compSection = config.getConfigurationSection("composition");
        if (compSection != null) {
            for (String blockMaterial : compSection.getKeys(false)) {
                double chance = compSection.getDouble(blockMaterial);
                mine.addComposition(blockMaterial, chance);
            }
        }

        return mine;
    }
}