package com.sasha.prisonminesx.gui.menus;

import com.sasha.prisonminesx.gui.GUIHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MainMenu implements GUIHolder {

    private final Inventory inventory;

    public MainMenu() {
        // Create a 27-slot (3 rows) inventory. 'this' sets the holder to our custom GUIHolder.
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.DARK_GRAY + "PrisonMinesX - Manager");
        initializeItems();
    }

    private void initializeItems() {
        // Create a basic "Create Mine" button
        ItemStack createMine = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createMine.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Create New Mine");
            createMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to generate a new",
                    ChatColor.GRAY + "mining area."
            ));
            createMine.setItemMeta(createMeta);
        }

        // Place the button in the middle of the GUI
        inventory.setItem(13, createMine);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // If they clicked the Emerald Block (Slot 13)
        if (slot == 13) {
            player.sendMessage(ChatColor.GREEN + "Starting the mine creation process...");
            player.closeInventory();
            // We will hook this up to an anvil GUI or chat prompt later!
        }
    }
}