package com.sasha.prisonminesx.utils;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

public class MineSerializer {

    public static void serializeToYaml(Mine mine, ConfigurationSection config) {
        config.set("world", mine.getWorldName());
        config.set("bounds.minX", mine.getMinX());
        config.set("bounds.minY", mine.getMinY());
        config.set("bounds.minZ", mine.getMinZ());
        config.set("bounds.maxX", mine.getMaxX());
        config.set("bounds.maxY", mine.getMaxY());
        config.set("bounds.maxZ", mine.getMaxZ());

        config.set("settings.description", mine.getDescription());
        config.set("settings.display-item", mine.getDisplayItem());
        config.set("settings.reset-delay", mine.getResetDelay());
        config.set("settings.reset-warnings", mine.getResetWarnings());
        config.set("settings.reset-percentage", mine.getResetPercentage());
        config.set("settings.silent", mine.isSilent());
        config.set("settings.fill-mode", mine.isFillMode());
        config.set("settings.surface", mine.getSurface());
        config.set("settings.teleport-on-reset", mine.isTeleportOnReset());
        config.set("settings.hologram-enabled", mine.isHologramEnabled());
        config.set("settings.actionbar-enabled", mine.isActionbarEnabled());

        config.set("settings.warn-global", null);
        config.set("settings.warn-mode", mine.getWarnMode());

        config.set("settings.paused", mine.isPaused());
        config.set("settings.schematic", mine.getSchematic());
        config.set("settings.reset-style", mine.getResetStyle());
        config.set("settings.reset-schedules", mine.getResetSchedules());
        config.set("settings.mine-fly", mine.isMineFly());
        config.set("settings.warp-delay", mine.getWarpDelay());

        config.set("settings.hunger", mine.isHunger());
        config.set("settings.fall-damage", mine.isFallDamage());
        config.set("settings.pvp", mine.isPvp());
        config.set("settings.place-blocks", mine.isPlaceBlocks());

        config.set("analytics.lifetime-mined", mine.getLifetimeMinedBlocks());
        config.set("analytics.lifetime-resets", mine.getLifetimeResets());

        if (mine.getTpLocation() != null) {
            Location loc = mine.getTpLocation();
            config.set("teleport.x", loc.getX());
            config.set("teleport.y", loc.getY());
            config.set("teleport.z", loc.getZ());
            config.set("teleport.yaw", loc.getYaw());
            config.set("teleport.pitch", loc.getPitch());
        }

        if (mine.getHologramLocation() != null) {
            config.set("settings.holo-loc.x", mine.getHologramLocation().getX());
            config.set("settings.holo-loc.y", mine.getHologramLocation().getY());
            config.set("settings.holo-loc.z", mine.getHologramLocation().getZ());
        } else {
            config.set("settings.holo-loc", null);
        }

        config.set("composition", null);
        if (mine.getComposition() != null && !mine.getComposition().isEmpty()) {
            for (Map.Entry<String, Double> entry : mine.getComposition().entrySet()) {
                config.set("composition." + entry.getKey(), entry.getValue());
            }
        }
    }

    public static Mine deserializeFromYaml(String mineName, ConfigurationSection config) {
        String worldName = config.getString("world");

        int minX = config.getInt("bounds.minX");
        int minY = config.getInt("bounds.minY");
        int minZ = config.getInt("bounds.minZ");
        int maxX = config.getInt("bounds.maxX");
        int maxY = config.getInt("bounds.maxY");
        int maxZ = config.getInt("bounds.maxZ");

        Mine mine = new Mine(mineName, worldName, minX, minY, minZ, maxX, maxY, maxZ);

        if (config.contains("settings.description")) mine.setDescription(config.getString("settings.description"));
        if (config.contains("settings.display-item")) mine.setDisplayItem(config.getString("settings.display-item"));
        mine.setResetDelay(config.getInt("settings.reset-delay", 600));
        mine.setResetWarnings(config.getIntegerList("settings.reset-warnings"));
        if (config.contains("settings.reset-percentage")) mine.setResetPercentage(config.getDouble("settings.reset-percentage"));
        mine.setSilent(config.getBoolean("settings.silent", true));
        mine.setFillMode(config.getBoolean("settings.fill-mode", false));
        if (config.contains("settings.surface")) mine.setSurface(config.getString("settings.surface"));
        mine.setTeleportOnReset(config.getBoolean("settings.teleport-on-reset", true));
        mine.setHologramEnabled(config.getBoolean("settings.hologram-enabled", false));
        mine.setActionbarEnabled(config.getBoolean("settings.actionbar-enabled", false));

        if (config.contains("settings.warn-mode")) mine.setWarnMode(config.getString("settings.warn-mode"));
        else if (config.contains("settings.warn-global")) mine.setWarnMode(config.getBoolean("settings.warn-global") ? "GLOBAL" : "NEARBY");

        mine.setPaused(config.getBoolean("settings.paused", false));
        if (config.contains("settings.schematic")) mine.setSchematic(config.getString("settings.schematic"));
        if (config.contains("settings.reset-style")) mine.setResetStyle(config.getString("settings.reset-style"));
        if (config.contains("settings.reset-schedules")) mine.setResetSchedules(config.getStringList("settings.reset-schedules"));
        if (config.contains("settings.mine-fly")) mine.setMineFly(config.getBoolean("settings.mine-fly"));
        if (config.contains("settings.warp-delay")) mine.setWarpDelay(config.getInt("settings.warp-delay"));

        if (config.contains("settings.hunger")) mine.setHunger(config.getBoolean("settings.hunger"));
        if (config.contains("settings.fall-damage")) mine.setFallDamage(config.getBoolean("settings.fall-damage"));
        if (config.contains("settings.pvp")) mine.setPvp(config.getBoolean("settings.pvp"));
        if (config.contains("settings.place-blocks")) mine.setPlaceBlocks(config.getBoolean("settings.place-blocks"));

        mine.setLifetimeMinedBlocks(config.getLong("analytics.lifetime-mined", 0));
        mine.setLifetimeResets(config.getInt("analytics.lifetime-resets", 0));

        if (config.contains("teleport.x")) {
            World world = Bukkit.getWorld(worldName);
            double x = config.getDouble("teleport.x");
            double y = config.getDouble("teleport.y");
            double z = config.getDouble("teleport.z");
            float yaw = (float) config.getDouble("teleport.yaw");
            float pitch = (float) config.getDouble("teleport.pitch");

            mine.setTpLocation(new Location(world, x, y, z, yaw, pitch));
        }

        if (config.contains("settings.holo-loc.x")) {
            mine.setHologramLocation(new Location(Bukkit.getWorld(worldName),
                    config.getDouble("settings.holo-loc.x"),
                    config.getDouble("settings.holo-loc.y"),
                    config.getDouble("settings.holo-loc.z")));
        }

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