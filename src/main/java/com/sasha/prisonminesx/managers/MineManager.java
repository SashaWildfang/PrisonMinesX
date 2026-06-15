package com.sasha.prisonminesx.managers;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.api.events.MinePreResetEvent;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.FAWEResetEngine;
import com.sasha.prisonminesx.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core manager for active mines.
 * Optimized with Chunk-based Spatial Hashing to prevent O(N) lag on Block Breaks.
 */
public class MineManager {

    private final PrisonMinesX plugin;
    private final Map<String, Mine> activeMines = new HashMap<>();

    // OPTIMIZATION: Spatial Hashing Map.
    // Maps a World Name + Chunk Key to a list of mines that occupy that chunk.
    private final Map<String, Map<Long, List<Mine>>> chunkMap = new ConcurrentHashMap<>();

    public MineManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    /** Loads all mines natively across all currently loaded Bukkit worlds. */
    public void loadActiveWorlds() {
        activeMines.clear();
        chunkMap.clear();
        for (World world : Bukkit.getWorlds()) {
            loadMinesForWorld(world.getName());
        }
        plugin.getLogger().info("MineManager lazy-loaded " + activeMines.size() + " mines across active worlds.");
    }

    /** Fetches mines for a specific world from the active Database Provider. */
    public void loadMinesForWorld(String worldName) {
        Map<String, Mine> loaded = plugin.getDatabaseManager().getProvider().loadMinesByWorld(worldName);
        if (loaded != null) {
            for (Mine m : loaded.values()) {
                m.calculateTotalBlocks();
                addMineToCache(m);
            }
        }
    }

    /** Unloads mines from memory when a world unloads to prevent memory leaks. */
    public void unloadMinesForWorld(String worldName) {
        activeMines.entrySet().removeIf(entry -> {
            if (entry.getValue().getWorldName().equals(worldName)) {
                plugin.getHologramManager().removeHologram(entry.getKey(), entry.getValue());
                removeMineFromChunks(entry.getValue());
                return true;
            }
            return false;
        });
        chunkMap.remove(worldName);
    }

    /**
     * Highly optimized O(1) lookup using Spatial Hashing.
     * Prevents looping through all active mines globally for every block broken.
     */
    public Mine getMineAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        String worldName = loc.getWorld().getName();
        if (!chunkMap.containsKey(worldName)) return null;

        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        long chunkKey = getChunkKey(chunkX, chunkZ);

        List<Mine> minesInChunk = chunkMap.get(worldName).get(chunkKey);
        if (minesInChunk == null || minesInChunk.isEmpty()) return null;

        for (Mine mine : minesInChunk) {
            if (loc.getBlockX() >= mine.getMinX() && loc.getBlockX() <= mine.getMaxX() &&
                    loc.getBlockY() >= mine.getMinY() && loc.getBlockY() <= mine.getMaxY() + 2 &&
                    loc.getBlockZ() >= mine.getMinZ() && loc.getBlockZ() <= mine.getMaxZ()) {
                return mine;
            }
        }
        return null;
    }

    public void resetMine(String mineName) {
        resetMine(mineName, false);
    }

    /**
     * Initiates the FAWE reset sequence.
     * Player teleportation is deliberately excluded here and moved to MineListener
     * to prevent players from suffocating during asynchronous cinematic pastes.
     */
    public void resetMine(String mineName, boolean isForced) {
        Mine mine = getMine(mineName);
        if (mine != null && mine.isSetup()) {

            MinePreResetEvent preEvent = new MinePreResetEvent(mine, isForced);
            Bukkit.getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) return;

            FAWEResetEngine.resetMineLayered(plugin, mine);
            mine.calculateTotalBlocks();
            mine.incrementLifetimeResets();

            if (mine.isHologramEnabled()) {
                if (isForced) {
                    plugin.getHologramManager().flashForcedReset(mine);
                } else {
                    plugin.getHologramManager().updateHologram(mine);
                }
            }
        }
    }

    // --- Spatial Hashing Registration Logic ---
    private void addMineToCache(Mine mine) {
        activeMines.put(mine.getName(), mine);
        registerMineToChunks(mine);
    }

    private void registerMineToChunks(Mine mine) {
        String worldName = mine.getWorldName();
        chunkMap.putIfAbsent(worldName, new ConcurrentHashMap<>());
        Map<Long, List<Mine>> worldChunks = chunkMap.get(worldName);

        int minChunkX = mine.getMinX() >> 4;
        int maxChunkX = mine.getMaxX() >> 4;
        int minChunkZ = mine.getMinZ() >> 4;
        int maxChunkZ = mine.getMaxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                long key = getChunkKey(x, z);
                worldChunks.computeIfAbsent(key, k -> new ArrayList<>()).add(mine);
            }
        }
    }

    private void removeMineFromChunks(Mine mine) {
        String worldName = mine.getWorldName();
        if (!chunkMap.containsKey(worldName)) return;

        int minChunkX = mine.getMinX() >> 4;
        int maxChunkX = mine.getMaxX() >> 4;
        int minChunkZ = mine.getMinZ() >> 4;
        int maxChunkZ = mine.getMaxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                long key = getChunkKey(x, z);
                List<Mine> list = chunkMap.get(worldName).get(key);
                if (list != null) list.remove(mine);
            }
        }
    }

    private long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // --- Teleportation & Management ---

    public void teleportToMine(Player player, Mine mine) {
        String msgPrefix = plugin.getMessages().getString("prefix", "").replace("&", "§");

        if (!player.hasPermission("prisonminesx.mine." + mine.getName().toLowerCase()) && !player.hasPermission("prisonminesx.mine.*") && !player.hasPermission("prisonminesx.mine.all") && !player.hasPermission("prisonminesx.admin")) {
            player.sendMessage(msgPrefix + plugin.getMessages().getString("mine.warp-no-access", "&cLocked.").replace("&", "§"));
            return;
        }

        int delay = mine.getWarpDelay() == -1 ? plugin.getConfig().getInt("settings.default-warp-delay", 3) : mine.getWarpDelay();

        if (delay <= 0 || player.hasPermission("prisonminesx.mine.warp.bypass")) {
            doTeleport(player, mine, msgPrefix);
            return;
        }

        player.sendMessage(msgPrefix + plugin.getMessages().getString("mine.warp-teleporting", "&7Teleporting in %time%.").replace("%time%", TimeUtil.formatTime(delay)).replace("%mine%", mine.getName()).replace("&", "§"));
        Location startLoc = player.getLocation().clone();

        new BukkitRunnable() {
            int ticks = delay * 20;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (startLoc.distanceSquared(player.getLocation()) > 0.5) {
                    player.sendMessage(msgPrefix + plugin.getMessages().getString("mine.warp-cancelled", "&cCancelled.").replace("&", "§"));
                    this.cancel();
                    return;
                }
                if (ticks <= 0) {
                    doTeleport(player, mine, msgPrefix);
                    this.cancel();
                    return;
                }
                ticks -= 5;
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private void doTeleport(Player player, Mine mine, String msgPrefix) {
        Location loc = mine.getTpLocation();
        if (loc == null) {
            World w = Bukkit.getWorld(mine.getWorldName());
            if (w != null) {
                loc = new Location(w, mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0, mine.getMaxY() + 1.0, mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0);
            }
        }
        if (loc != null) {
            player.teleport(loc);
            player.sendMessage(msgPrefix + plugin.getMessages().getString("mine.warp-success", "&7Warped!").replace("%mine%", mine.getName()).replace("&", "§"));
        } else {
            player.sendMessage(msgPrefix + plugin.getMessages().getString("commands.tp-failed", "&cFailed.").replace("&", "§"));
        }
    }

    public void renameMine(String oldName, String newName) {
        Mine mine = getMine(oldName);
        if (mine != null) {
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().removeHologram(oldName, mine);
            }
            removeMineFromChunks(mine);
            activeMines.remove(oldName);
            plugin.getDatabaseManager().getProvider().deleteMine(oldName);

            mine.setName(newName);
            addMineToCache(mine);
            plugin.getDatabaseManager().getProvider().saveMine(mine);

            if (mine.isHologramEnabled()) {
                plugin.getHologramManager().updateHologram(mine);
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
        removeMineFromChunks(mine); // Ensure clean transition if bounds changed
        addMineToCache(mine);
        plugin.getDatabaseManager().getProvider().saveMine(mine);
    }

    public void deleteMine(String name) {
        Mine mine = getMine(name);
        if (mine != null) {
            if (plugin.getHologramManager() != null) {
                plugin.getHologramManager().removeHologram(mine.getName(), mine);
            }
            removeMineFromChunks(mine);
            activeMines.remove(mine.getName());
            plugin.getDatabaseManager().getProvider().deleteMine(mine.getName());
        }
    }
}