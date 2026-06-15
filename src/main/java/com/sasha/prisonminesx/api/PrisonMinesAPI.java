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
 * The central API for interacting with PrisonMinesX.
 */
public class PrisonMinesAPI {

    private static PrisonMinesAPI instance;
    private final PrisonMinesX plugin;

    public PrisonMinesAPI(PrisonMinesX plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static PrisonMinesAPI getInstance() { return instance; }

    /** Retrieves a mine by name. */
    public Mine getMine(String name) {
        return plugin.getMineManager().getMine(name);
    }

    /** Gets all active mines. */
    public Collection<Mine> getActiveMines() {
        return plugin.getMineManager().getMines();
    }

    /** Checks if a location is inside any active mine. */
    public Mine getMineAt(Location location) {
        return plugin.getMineManager().getMineAt(location);
    }

    /** Forces a mine to reset. */
    public boolean forceReset(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().resetMine(mineName, true);
            return true;
        }
        return false;
    }

    /** Completely deletes a mine from memory and storage. */
    public boolean deleteMine(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().deleteMine(mineName);
            return true;
        }
        return false;
    }

    /** Renames a currently active mine. */
    public boolean renameMine(String oldName, String newName) {
        Mine mine = getMine(oldName);
        if (mine != null && getMine(newName) == null) {
            plugin.getMineManager().renameMine(oldName, newName);
            return true;
        }
        return false;
    }

    /** Gets a set of UUIDs belonging to players currently physically inside the mine. */
    public Set<UUID> getActivePlayersInMine(String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            return mine.getActivePlayers().keySet();
        }
        return null;
    }

    /** Uses the plugin's internal teleportation handler to safely warp a player to a mine. */
    public boolean teleportPlayerToMine(Player player, String mineName) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            plugin.getMineManager().teleportToMine(player, mine);
            return true;
        }
        return false;
    }

    /** Updates the hologram for a specific mine (useful if another plugin edits blocks). */
    public void updateHologram(Mine mine) {
        if (mine != null && mine.isHologramEnabled()) {
            plugin.getHologramManager().updateHologram(mine);
        }
    }

    /** Overrides the entire block composition of a mine remotely. */
    public void setMineComposition(String mineName, Map<String, Double> composition) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            mine.setComposition(composition);
            plugin.getMineManager().addMine(mine);
        }
    }

    /** Quickly adds or alters a single block percentage in the mine. Set chance to 0 to remove. */
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

    /** Pauses or unpauses a mine's timers. */
    public void setMinePaused(String mineName, boolean paused) {
        Mine mine = getMine(mineName);
        if (mine != null) {
            mine.setPaused(paused);
            plugin.getMineManager().addMine(mine);
        }
    }
}