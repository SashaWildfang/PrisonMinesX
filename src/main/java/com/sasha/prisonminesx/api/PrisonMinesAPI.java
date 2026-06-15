package com.sasha.prisonminesx.api;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The central API for interacting with PrisonMinesX externally.
 * Exposes safe hooks for third-party developers to fetch, modify, or trigger mine actions.
 */
public class PrisonMinesAPI {

    private static PrisonMinesAPI instance;
    private final PrisonMinesX plugin;

    public PrisonMinesAPI(PrisonMinesX plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /** @return The static singleton instance of the API. */
    public static PrisonMinesAPI getInstance() { return instance; }

    /** Retrieves a mine directly by its registered name. */
    public Mine getMine(String name) {
        return plugin.getMineManager().getMine(name);
    }

    /** Gets an unmodifiable collection of all active mines currently loaded in memory. */
    public Collection<Mine> getActiveMines() {
        return plugin.getMineManager().getMines();
    }

    /** * Determines if a specific physical location falls within any mine boundaries.
     * Note: Uses spatial hashing via MineManager in newer versions for O(1) lookups.
     */
    public Mine getMineAt(Location location) {
        return plugin.getMineManager().getMineAt(location);
    }

    /** Forces a specific mine to instantly begin its reset sequence. */
    public boolean forceReset(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().resetMine(mineName, true);
            return true;
        }
        return false;
    }

    /** Completely deletes a mine from RAM and persistent database storage. */
    public boolean deleteMine(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().deleteMine(mineName);
            return true;
        }
        return false;
    }

    /** Safely renames an active mine, migrating its holographic data and storage tags. */
    public boolean renameMine(String oldName, String newName) {
        Mine mine = getMine(oldName);
        if (mine != null && getMine(newName) == null) {
            plugin.getMineManager().renameMine(oldName, newName);
            return true;
        }
        return false;
    }

    /** Retrieves a live Set of UUIDs of all players physically standing inside the mine region. */
    public Set<UUID> getActivePlayersInMine(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            return mine.getActivePlayers().keySet();
        }
        return null;
    }

    /** Uses the plugin's internal teleportation handler, respecting delays and permissions. */
    public boolean teleportPlayerToMine(Player player, String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().teleportToMine(player, mine);
            return true;
        }
        return false;
    }

    /** Forces a refresh of the floating text hologram associated with the mine. */
    public void updateHologram(Mine mine) {
        if (mine != null && mine.isHologramEnabled()) {
            plugin.getHologramManager().updateHologram(mine);
        }
    }

    /** Bulk overrides the entire block composition map of a mine. */
    public void setMineComposition(String mineName, Map<String, Double> composition) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            mine.setComposition(composition);
            plugin.getMineManager().addMine(mine);
        }
    }

    /** Quickly adds, alters, or removes (chance = 0) a single material in the composition pool. */
    public boolean modifyMineBlock(String mineName, String material, double chance) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            if (chance <= 0) {
                mine.getComposition().remove(material.toUpperCase());
            } else {
                mine.addComposition(material.toUpperCase(), chance);
            }
            plugin.getMineManager().addMine(mine);
            return true;
        }
        return false;
    }

    /** Dynamically pauses or unpauses a mine's autonomous timers. */
    public void setMinePaused(String mineName, boolean paused) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            mine.setPaused(paused);
            plugin.getMineManager().addMine(mine);
        }
    }
}