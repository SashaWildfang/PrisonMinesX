package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.gui.GUIHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the inventory has a holder, and if it belongs to our plugin
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof GUIHolder) {
            // Cancel the event immediately so players can't steal the GUI items
            event.setCancelled(true);

            // Route the click to the specific menu's logic
            ((GUIHolder) holder).onGUIClick(event);
        }
    }
}