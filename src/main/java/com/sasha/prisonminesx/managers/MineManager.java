package com.sasha.prisonminesx.managers;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.FAWEResetEngine;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MineManager {

    private final PrisonMinesX plugin;
    private final Map<String, Mine> activeMines = new HashMap<>();

    public MineManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    public void loadActiveWorlds() {
        activeMines.clear();
        for (World world : Bukkit.getWorlds()) {
            loadMinesForWorld(world.getName());
        }
        plugin.getLogger().info("MineManager lazy-loaded " + activeMines.size() + " mines across active worlds.");
    }

    public void loadMinesForWorld(String worldName) {
        Map<String, Mine> loaded = plugin.getDatabaseManager().getProvider().loadMinesByWorld(worldName);
        if (loaded != null) {
            for (Mine m : loaded.values()) {
                m.calculateTotalBlocks();
                activeMines.put(m.getName(), m);
            }
        }
    }

    public void unloadMinesForWorld(String worldName) {
        activeMines.entrySet().removeIf(entry -> {
            if (entry.getValue().getWorldName().equals(worldName)) {
                plugin.getHologramManager().removeHologram(entry.getKey(), entry.getValue());
                return true;
            }
            return false;
        });
    }

    public void resetMine(String mineName) {
        resetMine(mineName, false);
    }

    public void resetMine(String mineName, boolean isForced) {
        Mine mine = getMine(mineName);
        if (mine != null && mine.isSetup()) {

            FAWEResetEngine.resetMineLayered(plugin, mine);
            mine.calculateTotalBlocks();
            mine.incrementLifetimeResets(); // Track Analytics

            if (mine.isHologramEnabled()) {
                if (isForced) {
                    plugin.getHologramManager().flashForcedReset(mine);
                } else {
                    plugin.getHologramManager().updateHologram(mine);
                }
            }

            if (mine.isTeleportOnReset() && mine.getTpLocation() != null) {
                World w = Bukkit.getWorld(mine.getWorldName());
                if (w != null) {
                    for (Player p : w.getPlayers()) {
                        if (p.getLocation().getBlockX() >= mine.getMinX() && p.getLocation().getBlockX() <= mine.getMaxX() &&
                                p.getLocation().getBlockY() >= mine.getMinY() && p.getLocation().getBlockY() <= mine.getMaxY() &&
                                p.getLocation().getBlockZ() >= mine.getMinZ() && p.getLocation().getBlockZ() <= mine.getMaxZ()) {
                            p.teleport(mine.getTpLocation());
                        }
                    }
                }
            }
        }
    }

    public Mine getMine(String name) {
        return activeMines.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public Collection<Mine> getMines() { return activeMines.values(); }

    public void addMine(Mine mine) {
        activeMines.put(mine.getName(), mine);
        plugin.getDatabaseManager().getProvider().saveMine(mine);
    }

    public void deleteMine(String name) {
        Mine mine = getMine(name);
        if (mine != null) {
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().removeHologram(mine.getName(), mine);
            }
            activeMines.remove(mine.getName());
            plugin.getDatabaseManager().getProvider().deleteMine(mine.getName());
        }
    }
}