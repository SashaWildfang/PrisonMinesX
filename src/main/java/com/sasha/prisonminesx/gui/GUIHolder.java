package com.sasha.prisonminesx.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public interface GUIHolder extends InventoryHolder {
    /**
     * This method fires whenever a player clicks inside this specific GUI.
     */
    void onGUIClick(InventoryClickEvent event);
}