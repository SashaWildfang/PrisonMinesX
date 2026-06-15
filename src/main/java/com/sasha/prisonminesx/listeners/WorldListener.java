package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Ensures mines correctly hook/unhook from memory and spatial maps
 * when worlds load or unload, preventing null pointers and memory leaks.
 */
public class WorldListener implements Listener {

    private final PrisonMinesX plugin;

    public WorldListener(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    /** Asynchronously bulk-loads mines from the database when a world is loaded by the server. */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getMineManager().loadMinesForWorld(event.getWorld().getName());
        });
    }

    /** Unhooks the mine from the spatial map and memory to avoid ghost memory leaks. */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.getMineManager().unloadMinesForWorld(event.getWorld().getName());
    }
}