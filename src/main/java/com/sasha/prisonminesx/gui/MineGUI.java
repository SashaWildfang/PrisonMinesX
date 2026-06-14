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

    private static String get(PrisonMinesX plugin, String path) {
        return plugin.getMessages().getString(path, "&c" + path).replace("&", "§");
    }

    private static List<String> getLore(PrisonMinesX plugin, String path, String... replace) {
        List<String> raw = plugin.getMessages().getStringList(path);
        List<String> formatted = new ArrayList<>();
        for (String s : raw) {
            String line = s.replace("&", "§");
            for (int i = 0; i < replace.length; i += 2) {
                line = line.replace(replace[i], replace[i + 1]);
            }
            formatted.add(line);
        }
        return formatted;
    }

    public static void openMainMenu(Player player, PrisonMinesX plugin, int page) {
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalMines = mines.size();
        int maxPerPage = (totalMines <= 7) ? 7 : 45;
        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int minesOnPage = Math.min(maxPerPage, totalMines - (page - 1) * maxPerPage);
        if (minesOnPage < 0) minesOnPage = 0;

        int guiSize;
        if (totalMines <= 7) {
            guiSize = 9;
        } else {
            int mineRows = (int) Math.ceil(minesOnPage / 9.0);
            if (mineRows == 0) mineRows = 1;
            guiSize = (mineRows + 1) * 9;
        }

        String title = totalPages > 1 ? get(plugin, "gui.main.title-paged").replace("%count%", String.valueOf(totalMines)).replace("%page%", String.valueOf(page))
                : get(plugin, "gui.main.title").replace("%count%", String.valueOf(totalMines));
        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalMines);
        int slotOffset = (guiSize == 9) ? 1 : 0;

        for (int i = startIndex; i < endIndex; i++) {
            Mine mine = mines.get(i);
            Material mat = Material.matchMaterial(mine.getDisplayItem());
            if (mat == null) mat = Material.DIAMOND_PICKAXE;

            inv.setItem(slotOffset++, createGuiItem(mat, get(plugin, "gui.main.mine-name").replace("%mine%", mine.getName()),
                    getLore(plugin, "gui.main.mine-lore", "%world%", mine.getWorldName(), "%percent%", String.format("%.1f", mine.getPercentRemaining()))));
        }

        int navStart = guiSize - 9;
        if (page > 1 || totalPages > 1) {
            inv.setItem(navStart, createGuiItem(Material.ARROW, get(plugin, "gui.main.prev-page")));
            inv.setItem(guiSize - 1, createGuiItem(Material.ARROW, get(plugin, "gui.main.next-page")));
        }

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = navStart; i < guiSize; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane);
        }

        player.openInventory(inv);
    }

    public static void openEditMenu(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, get(plugin, "gui.edit.title").replace("%mine%", mine.getName()));

        inv.setItem(10, createGuiItem(Material.ENDER_PEARL, get(plugin, "gui.edit.teleport"), getLore(plugin, "gui.edit.teleport-lore")));
        inv.setItem(12, createGuiItem(Material.COMPARATOR, get(plugin, "gui.edit.flags"), getLore(plugin, "gui.edit.flags-lore")));
        inv.setItem(14, createGuiItem(Material.EMERALD_BLOCK, get(plugin, "gui.edit.blocks"), getLore(plugin, "gui.edit.blocks-lore")));
        inv.setItem(16, createGuiItem(Material.BARRIER, get(plugin, "gui.edit.delete"), getLore(plugin, "gui.edit.delete-lore")));
        inv.setItem(26, createGuiItem(Material.ARROW, get(plugin, "gui.edit.back")));

        fillEmpty(inv);
        player.openInventory(inv);
    }

    public static void openFlagsMenu(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, get(plugin, "gui.flags.title").replace("%mine%", mine.getName()));

        // --- ROW 2 (Slots 10-16: 7 Items) ---
        inv.setItem(10, createGuiItem(Material.CLOCK, get(plugin, "gui.flags.delay"), getLore(plugin, "gui.flags.delay-lore", "%value%", mine.getResetDelay() / 60 + "m")));

        StringBuilder warns = new StringBuilder();
        for (int i : mine.getResetWarnings()) warns.append(i / 60).append(",");
        String warnStr = warns.length() > 0 ? warns.substring(0, warns.length() - 1) : "None";
        inv.setItem(11, createGuiItem(Material.BELL, get(plugin, "gui.flags.warnings"), getLore(plugin, "gui.flags.warnings-lore", "%value%", warnStr)));

        inv.setItem(12, createGuiItem(Material.EXPERIENCE_BOTTLE, get(plugin, "gui.flags.percent"), getLore(plugin, "gui.flags.percent-lore", "%value%", mine.getResetPercentage() == -1.0 ? "Default" : mine.getResetPercentage() + "%")));

        inv.setItem(13, createGuiItem(Material.SPONGE, get(plugin, "gui.flags.fillmode"), getLore(plugin, "gui.flags.fillmode-lore", "%state%", mine.isFillMode() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        Material surfaceIcon = Material.GRASS_BLOCK;
        if (mine.getSurface() != null) {
            Material matched = Material.matchMaterial(mine.getSurface());
            if (matched != null) surfaceIcon = matched;
        }
        inv.setItem(14, createGuiItem(surfaceIcon, get(plugin, "gui.flags.surface"), getLore(plugin, "gui.flags.surface-lore", "%value%", mine.getSurface() == null ? "None" : com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getSurface()))));

        inv.setItem(15, createGuiItem(Material.NOTE_BLOCK, get(plugin, "gui.flags.silent"), getLore(plugin, "gui.flags.silent-lore", "%state%", mine.isSilent() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(16, createGuiItem(Material.OAK_SIGN, get(plugin, "gui.flags.warn-global"), getLore(plugin, "gui.flags.warn-global-lore", "%state%", mine.isWarnGlobal() ? get(plugin, "formats.global") : get(plugin, "formats.nearby"))));

        // --- ROW 3 (Slots 20-24: 5 Items) ---
        Material iconMat = Material.matchMaterial(mine.getDisplayItem());
        if (iconMat == null) iconMat = Material.DIAMOND_PICKAXE;
        inv.setItem(20, createGuiItem(iconMat, get(plugin, "gui.flags.icon"), getLore(plugin, "gui.flags.icon-lore", "%value%", com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getDisplayItem()))));

        inv.setItem(21, createGuiItem(Material.ENDER_EYE, get(plugin, "gui.flags.tp-reset"), getLore(plugin, "gui.flags.tp-reset-lore", "%state%", mine.isTeleportOnReset() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(22, createGuiItem(Material.ARMOR_STAND, get(plugin, "gui.flags.hologram"), getLore(plugin, "gui.flags.hologram-lore", "%state%", mine.isHologramEnabled() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(23, createGuiItem(Material.NAME_TAG, get(plugin, "gui.flags.actionbar"), getLore(plugin, "gui.flags.actionbar-lore", "%state%", mine.isActionbarEnabled() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(24, createGuiItem(Material.BARRIER, get(plugin, "gui.flags.pause"), getLore(plugin, "gui.flags.pause-lore", "%state%", mine.isPaused() ? get(plugin, "formats.paused") : get(plugin, "formats.running"))));

        // --- ROW 4 (Slot 31: Back Button) ---
        inv.setItem(31, createGuiItem(Material.ARROW, get(plugin, "gui.flags.back")));

        fillEmpty(inv);
        player.openInventory(inv);
    }

    public static void openBlockEditor(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, get(plugin, "gui.blocks.title").replace("%mine%", mine.getName()));

        List<Map.Entry<String, Double>> sortedComposition = new ArrayList<>(mine.getComposition().entrySet());
        sortedComposition.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int slot = 0;
        for (Map.Entry<String, Double> entry : sortedComposition) {
            if (slot >= 45) break;

            Material mat = Material.matchMaterial(entry.getKey());
            if (mat == null) mat = Material.STONE;

            String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(entry.getKey());
            inv.setItem(slot++, createGuiItem(mat, get(plugin, "gui.blocks.block-name").replace("%block%", niceName),
                    getLore(plugin, "gui.blocks.block-lore", "%chance%", String.valueOf(entry.getValue()))));
        }

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i <= 53; i++) {
            if (i == 45 || i == 49 || i == 53) continue;
            inv.setItem(i, filler);
        }

        inv.setItem(45, createGuiItem(Material.ARROW, get(plugin, "gui.blocks.back")));
        inv.setItem(49, createGuiItem(Material.PAPER, get(plugin, "gui.blocks.info"), getLore(plugin, "gui.blocks.info-lore")));
        inv.setItem(53, createGuiItem(Material.GOLDEN_PICKAXE, get(plugin, "gui.blocks.force-reset"), getLore(plugin, "gui.blocks.force-reset-lore")));

        player.openInventory(inv);
    }

    private static void fillEmpty(Inventory inv) {
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    private static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGuiItem(Material material, String name) {
        return createGuiItem(material, name, new ArrayList<>());
    }
}