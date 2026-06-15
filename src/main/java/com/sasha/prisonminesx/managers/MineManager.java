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

    public Mine getMineAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Mine mine : activeMines.values()) {
            if (!mine.getWorldName().equals(loc.getWorld().getName())) continue;
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
            activeMines.remove(oldName);
            plugin.getDatabaseManager().getProvider().deleteMine(oldName);

            mine.setName(newName);
            activeMines.put(newName, mine);
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