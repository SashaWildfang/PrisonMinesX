package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

    private final PrisonMinesX plugin;

    public WorldListener(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getMineManager().loadMinesForWorld(event.getWorld().getName());
        });
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.getMineManager().unloadMinesForWorld(event.getWorld().getName());
    }
}