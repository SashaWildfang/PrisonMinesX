package com.sasha.prisonminesx.gui;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MineGUI {

    public static void openMainMenu(Player player, PrisonMinesX plugin, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalMines = mines.size();

        // Exact 1-row sizing rule: Allows up to 7 items on Row 1 before paginating if needed
        int maxPerPage = (totalMines <= 7) ? 7 : 45;

        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;

        // Failsafe if external command tries opening invalid page
        if (page > totalPages) page = totalPages;

        // Dynamic GUI Sizing: Calculates EXACT number of rows needed for the active page
        int minesOnPage = Math.min(maxPerPage, totalMines - (page - 1) * maxPerPage);
        if (minesOnPage < 0) minesOnPage = 0;

        int guiSize;
        if (totalMines <= 7) {
            guiSize = 9; // Single Row
        } else {
            int mineRows = (int) Math.ceil(minesOnPage / 9.0);
            if (mineRows == 0) mineRows = 1; // Minimum space
            guiSize = (mineRows + 1) * 9; // Add 1 row exclusively for navigation
        }

        String title = totalPages > 1 ? "§8Mines - " + totalMines + " | Pg " + page : "§8Mines - " + totalMines;
        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalMines);

        int slotOffset = (guiSize == 9) ? 1 : 0;
        for (int i = startIndex; i < endIndex; i++) {
            Mine mine = mines.get(i);
            Material mat = Material.matchMaterial(mine.getDisplayItem());
            if (mat == null) mat = Material.DIAMOND_PICKAXE;

            inv.setItem(slotOffset++, createGuiItem(mat, "§b§lMine: §f" + mine.getName(),
                    "§7World: §f" + mine.getWorldName(),
                    "§7Blocks Remaining: §f" + String.format("%.1f", mine.getPercentRemaining()) + "%",
                    "", "§eClick to edit settings."));
        }

        // Apply Nav Row strictly to the final row of the exact generated size
        int navStart = guiSize - 9;
        if (page > 1 || totalPages > 1) {
            inv.setItem(navStart, createGuiItem(Material.ARROW, "§cPrevious Page"));
            inv.setItem(guiSize - 1, createGuiItem(Material.ARROW, "§aNext Page"));
        }

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = navStart; i < guiSize; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }

        player.openInventory(inv);
    }

    public static void openEditMenu(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Editing: §b" + mine.getName());

        inv.setItem(10, createGuiItem(Material.ENDER_PEARL, "§aSet Teleport Location", "§7Sets the teleport point to", "§7your current location."));
        inv.setItem(12, createGuiItem(Material.COMPARATOR, "§6Edit Flags", "§7Manage resets, behavior, and modes."));
        inv.setItem(14, createGuiItem(Material.EMERALD_BLOCK, "§bEdit Block Composition", "§7Manage blocks spawning in the mine."));
        inv.setItem(16, createGuiItem(Material.BARRIER, "§cDelete Mine", "§7Shift-Right-Click to permanently", "§7delete this mine."));

        inv.setItem(26, createGuiItem(Material.ARROW, "§cBack to Mines"));

        fillEmpty(inv);
        player.openInventory(inv);
    }

    public static void openFlagsMenu(Player player, Mine mine) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Flags: §b" + mine.getName());

        inv.setItem(10, createGuiItem(Material.CLOCK, "§eReset Delay",
                "§7The time between automatic resets.",
                "§7Current: §f" + mine.getResetDelay() / 60 + "m", "", "§eClick to edit via chat."));

        inv.setItem(11, createGuiItem(Material.BELL, "§eReset Warnings",
                "§7Broadcasts warnings before reset.",
                "§7Current: §f" + formatWarnings(mine.getResetWarnings()), "", "§eClick to edit via chat."));

        inv.setItem(12, createGuiItem(Material.GRASS_BLOCK, "§eSurface Block",
                "§7Forces the top layer to be a specific block.",
                "§7Current: §f" + (mine.getSurface() == null ? "None" : com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getSurface())), "", "§eClick to edit via chat."));

        inv.setItem(13, createGuiItem(Material.EXPERIENCE_BOTTLE, "§ePercent Reset",
                "§7Resets when the mine is X% mined.",
                "§7Current: §f" + (mine.getResetPercentage() == -1.0 ? "Default" : mine.getResetPercentage() + "%"), "", "§eClick to edit via chat."));

        Material iconMat = Material.matchMaterial(mine.getDisplayItem());
        if (iconMat == null) iconMat = Material.DIAMOND_PICKAXE;
        inv.setItem(14, createGuiItem(iconMat, "§dSet Display Item",
                "§7Changes the icon for this mine.",
                "§7Current: §f" + com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getDisplayItem()), "", "§eClick here, then click an item", "§ein your inventory to set it."));

        inv.setItem(19, createGuiItem(Material.SPONGE, "§6Fill Mode",
                "§7Only replaces Air blocks when resetting.",
                "§7Current: " + (mine.isFillMode() ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle."));

        inv.setItem(20, createGuiItem(Material.NOTE_BLOCK, "§6Silent Reset",
                "§7Disables reset broadcast messages.",
                "§7Current: " + (mine.isSilent() ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle."));

        inv.setItem(21, createGuiItem(Material.ENDER_EYE, "§6Teleport on Reset",
                "§7Teleports players out on reset.",
                "§7Current: " + (mine.isTeleportOnReset() ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle."));

        inv.setItem(22, createGuiItem(Material.ARMOR_STAND, "§6Hologram",
                "§7Displays a floating info hologram.",
                "§7Current: " + (mine.isHologramEnabled() ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle."));

        inv.setItem(23, createGuiItem(Material.NAME_TAG, "§6Actionbar Notifs",
                "§7Displays remaining time/blocks.",
                "§7Current: " + (mine.isActionbarEnabled() ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle."));

        inv.setItem(24, createGuiItem(Material.OAK_SIGN, "§6Warning Broadcast",
                "§7Global vs Nearby Player Warnings.",
                "§7Current: " + (mine.isWarnGlobal() ? "§aGlobal" : "§cNearby Only"), "", "§eClick to toggle."));

        inv.setItem(25, createGuiItem(Material.BARRIER, "§6Stop Resetting",
                "§7Pauses all timers and % resets.",
                "§7Current: " + (mine.isPaused() ? "§cPaused" : "§aRunning"), "", "§eClick to toggle."));

        inv.setItem(31, createGuiItem(Material.ARROW, "§cGo Back"));

        fillEmpty(inv);
        player.openInventory(inv);
    }

    public static void openBlockEditor(Player player, Mine mine) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8Blocks: §b" + mine.getName());

        List<Map.Entry<String, Double>> sortedComposition = new ArrayList<>(mine.getComposition().entrySet());
        sortedComposition.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int slot = 0;
        for (Map.Entry<String, Double> entry : sortedComposition) {
            if (slot >= 45) break;

            Material mat = Material.matchMaterial(entry.getKey());
            if (mat == null) mat = Material.STONE;

            String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(entry.getKey());

            ItemStack item = createGuiItem(mat, "§a" + niceName,
                    "§7Current Chance: §e" + entry.getValue() + "%",
                    "",
                    "§eLeft-Click §7to edit percentage.",
                    "§cRight-Click §7to remove block.");

            inv.setItem(slot++, item);
        }

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i <= 53; i++) {
            if (i == 45 || i == 49 || i == 53) continue;
            inv.setItem(i, filler);
        }

        inv.setItem(45, createGuiItem(Material.ARROW, "§cGo Back"));
        inv.setItem(49, createGuiItem(Material.PAPER, "§eHow to add blocks:", "§7Click any block in your", "§7player inventory below", "§7to add it to the mine."));
        inv.setItem(53, createGuiItem(Material.GOLDEN_PICKAXE, "§eForce Reset", "§7Click to instantly reset", "§7this mine."));

        player.openInventory(inv);
    }

    private static String formatWarnings(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i : list) sb.append(i / 60).append(",");
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "None";
    }

    private static void fillEmpty(Inventory inv) {
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private static ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(line);
            meta.setLore(loreList);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}