package com.sasha.prisonminesx.gui;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The unified GUI Engine handling construction, pagination, and real-time refreshes for all menus.
 * Implements static title caching to prevent O(1) String translation overhead on InventoryClickEvents.
 */
public class MineGUI {

    public enum SortType {
        ALPHABETICAL_AZ("Alphabetical (A-Z)"),
        ALPHABETICAL_ZA("Alphabetical (Z-A)"),
        PLAYERS("Number of Players in Mine"),
        BLOCKS_LEAST("Blocks Remaining (Least)"),
        BLOCKS_MOST("Blocks Remaining (Most)"),
        BLOCKS_MINED_PER_RESET("Blocks Mined Per Reset"),
        TIME_SOONEST("Reset Time (Soonest)"),
        TIME_LATEST("Reset Time (Latest)"),
        SIZE("Mine Size"),
        STATUS("Mine Status"),
        WORLD("World");

        private final String name;
        SortType(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static final Map<UUID, SortType> playerSorts = new HashMap<>();

    // OPTIMIZATION: Cache to avoid extreme overhead during rapid UI interactions
    private static final Map<String, String> cachedBaseTitles = new HashMap<>();

    /**
     * Pre-loads necessary GUI titles from messages.yml upon plugin start or reload.
     */
    public static void cacheTitles(PrisonMinesX plugin) {
        cachedBaseTitles.clear();
        String[] paths = {"gui.main.title", "gui.edit.title", "gui.flags.title", "gui.blocks.title", "gui.active-players.title", "gui.warps.title", "gui.stats.title"};
        for (String path : paths) {
            String raw = plugin.getMessages().getString(path, "UNKNOWN");
            String translated = ChatColor.translateAlternateColorCodes('&', raw);
            cachedBaseTitles.put(path, ChatColor.stripColor(translated).split("%")[0].trim());
        }
    }

    /**
     * Safely retrieves a cached GUI title base to verify if a click event belongs to PrisonMinesX.
     */
    public static String getBaseTitle(PrisonMinesX plugin, String path) {
        if (!cachedBaseTitles.containsKey(path)) {
            // Fallback for missing cache maps
            String raw = plugin.getMessages().getString(path, "");
            if (raw == null || raw.isEmpty()) return "UNKNOWN_TITLE";
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', raw)).split("%")[0].trim();
        }
        return cachedBaseTitles.get(path);
    }

    private static String get(PrisonMinesX plugin, String path) {
        return plugin.getMessages().getString(path, "&c" + path).replace("&", "§");
    }

    /** Handles deep Lore replacement routing, injecting blank lines securely around optional parameters. */
    private static List<String> getLore(PrisonMinesX plugin, String path, String... replace) {
        List<String> raw = plugin.getMessages().getStringList(path);
        List<String> formatted = new ArrayList<>();
        for (String s : raw) {
            String line = s.replace("&", "§");
            boolean dropLine = false;
            for (int i = 0; i < replace.length; i += 2) {
                String key = replace[i];
                String val = replace[i + 1];

                if ((key.equals("%description%") || key.equals("%paused_state%")) && val.isEmpty()) {
                    if (line.contains(key)) {
                        line = line.replace(key, "");
                        if (ChatColor.stripColor(line).trim().isEmpty()) dropLine = true;
                    }
                } else {
                    if (val != null) line = line.replace(key, val);
                }
            }
            if (!dropLine) {
                if (line.contains("\n")) {
                    formatted.addAll(Arrays.asList(line.split("\n", -1)));
                } else {
                    formatted.add(line);
                }
            }
        }
        return formatted;
    }

    /** Formats extremely large block counters to human-readable strings (e.g. 1.25M) */
    public static String formatLargeNumber(double value) {
        if (value < 1000) {
            if (value == Math.floor(value)) {
                return String.valueOf((long) value);
            }
            return String.format("%.2f", value).replace(".00", "");
        }
        String[] suffixes = new String[]{"", "k", "M", "B", "T", "Q"};
        int index = 0;
        while (value >= 1000 && index < suffixes.length - 1) {
            value /= 1000;
            index++;
        }
        String formatted = String.format("%.2f", value);
        if (formatted.endsWith(".00")) formatted = formatted.substring(0, formatted.length() - 3);
        else if (formatted.endsWith("0")) formatted = formatted.substring(0, formatted.length() - 1);
        return formatted + suffixes[index];
    }

    public static void openWarpsMenu(Player player, PrisonMinesX plugin, int page, List<Mine> mines) {
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalMines = mines.size();
        int maxPerPage = plugin.getConfig().getInt("settings.warps-per-page", 45);
        if (maxPerPage > 45) maxPerPage = 45;

        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int minesOnPage = Math.min(maxPerPage, totalMines - (page - 1) * maxPerPage);
        if (minesOnPage < 0) minesOnPage = 0;

        int guiSize = (minesOnPage <= 9) ? 9 : ((int) Math.ceil(minesOnPage / 9.0)) * 9;
        if (guiSize == 0) guiSize = 9;
        if (totalPages > 1) guiSize += 9;
        if (guiSize > 54) guiSize = 54;

        String title = get(plugin, "gui.warps.title").replace("%page%", String.valueOf(page));
        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        refreshWarpsMenu(inv, player, plugin, page, mines);
        player.openInventory(inv);
    }

    public static void refreshWarpsMenu(Inventory inv, Player player, PrisonMinesX plugin, int page, List<Mine> mines) {
        mines.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));

        int totalMines = mines.size();
        int maxPerPage = plugin.getConfig().getInt("settings.warps-per-page", 45);
        if (maxPerPage > 45) maxPerPage = 45;

        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalMines);
        int slotOffset = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Mine mine = mines.get(i);
            Material mat = Material.matchMaterial(mine.getDisplayItem());
            if (mat == null) mat = Material.DIAMOND_PICKAXE;

            String desc = mine.getDescription() == null || mine.getDescription().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', mine.getDescription()) + "\n";
            String pausedState = mine.isPaused() ? get(plugin, "formats.paused-state") : "";

            inv.setItem(slotOffset++, createGuiItem(mat, get(plugin, "gui.main.mine-name").replace("%mine%", mine.getName()),
                    getLore(plugin, "gui.warps.mine-lore",
                            "%description%", desc,
                            "%paused_state%", pausedState,
                            "%players%", String.valueOf(mine.getActivePlayers().size()),
                            "%percent%", String.format("%.1f", mine.getPercentRemaining()),
                            "%time_left%", TimeUtil.formatTime(mine.getTimeUntilReset())
                    )));
        }

        int navStart = inv.getSize() - 9;
        for (int i = slotOffset; i < navStart; i++) inv.setItem(i, null);

        if (totalPages > 1) {
            if (page > 1) inv.setItem(navStart, createGuiItem(Material.ARROW, get(plugin, "gui.main.prev-page")));
            else inv.setItem(navStart, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

            if (page < totalPages) inv.setItem(inv.getSize() - 1, createGuiItem(Material.ARROW, get(plugin, "gui.main.next-page")));
            else inv.setItem(inv.getSize() - 1, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

            ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
            for (int i = navStart + 1; i < inv.getSize() - 1; i++) {
                inv.setItem(i, pane);
            }
        }
    }

    public static void refreshMainMenu(Inventory inv, Player player, PrisonMinesX plugin, int page) {
        int guiSize = inv.getSize();
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        SortType sort = playerSorts.getOrDefault(player.getUniqueId(), SortType.ALPHABETICAL_AZ);

        mines.sort((m1, m2) -> {
            switch (sort) {
                case ALPHABETICAL_ZA: return m2.getName().compareToIgnoreCase(m1.getName());
                case PLAYERS: return Integer.compare(m2.getActivePlayers().size(), m1.getActivePlayers().size());
                case BLOCKS_LEAST: return Double.compare(m1.getPercentRemaining(), m2.getPercentRemaining());
                case BLOCKS_MOST: return Double.compare(m2.getPercentRemaining(), m1.getPercentRemaining());
                case BLOCKS_MINED_PER_RESET: return Long.compare(m2.getLifetimeMinedBlocks() / Math.max(1, m2.getLifetimeResets()), m1.getLifetimeMinedBlocks() / Math.max(1, m1.getLifetimeResets()));
                case TIME_SOONEST: return Integer.compare(m1.getTimeUntilReset(), m2.getTimeUntilReset());
                case TIME_LATEST: return Integer.compare(m2.getTimeUntilReset(), m1.getTimeUntilReset());
                case SIZE: return Integer.compare(m2.getTotalBlocks(), m1.getTotalBlocks());
                case STATUS: return Boolean.compare(m1.isPaused(), m2.isPaused());
                case WORLD: return m1.getWorldName().compareToIgnoreCase(m2.getWorldName());
                case ALPHABETICAL_AZ:
                default: return m1.getName().compareToIgnoreCase(m2.getName());
            }
        });

        int totalMines = mines.size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalMines);
        int slotOffset = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Mine mine = mines.get(i);
            Material mat = Material.matchMaterial(mine.getDisplayItem());
            if (mat == null) mat = Material.DIAMOND_PICKAXE;

            String state = mine.isPaused() ? get(plugin, "formats.paused") : get(plugin, "formats.running");
            String desc = mine.getDescription() == null || mine.getDescription().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', mine.getDescription()) + "\n";

            inv.setItem(slotOffset++, createGuiItem(mat, get(plugin, "gui.main.mine-name").replace("%mine%", mine.getName()),
                    getLore(plugin, "gui.main.mine-lore",
                            "%description%", desc,
                            "%state%", state,
                            "%world%", mine.getWorldName(),
                            "%players%", String.valueOf(mine.getActivePlayers().size()),
                            "%percent%", String.format("%.1f", mine.getPercentRemaining()),
                            "%time_left%", TimeUtil.formatTime(mine.getTimeUntilReset())
                    )));
        }

        int navStart = guiSize - 9;
        for (int i = slotOffset; i < navStart; i++) inv.setItem(i, null);

        if (page > 1) inv.setItem(navStart, createGuiItem(Material.ARROW, get(plugin, "gui.main.prev-page")));
        else inv.setItem(navStart, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (page < totalPages) inv.setItem(guiSize - 1, createGuiItem(Material.ARROW, get(plugin, "gui.main.next-page")));
        else inv.setItem(guiSize - 1, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<String> sortLore = new ArrayList<>();
        sortLore.add("§7Click to cycle sorting.");
        sortLore.add("");
        for (SortType type : SortType.values()) {
            if (type == sort) sortLore.add(get(plugin, "formats.sort-active").replace("%name%", type.getName()));
            else sortLore.add(get(plugin, "formats.sort-inactive").replace("%name%", type.getName()));
        }
        sortLore.add("");
        sortLore.add("§bLeft-Click §7to cycle down.");
        sortLore.add("§cRight-Click §7to cycle up.");

        inv.setItem(guiSize - 5, createGuiItem(Material.OAK_SIGN, get(plugin, "gui.main.sort-title"), sortLore));

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = navStart + 1; i < guiSize - 1; i++) {
            if (i == guiSize - 5) continue;
            inv.setItem(i, pane);
        }
    }

    public static void openMainMenu(Player player, PrisonMinesX plugin, int page) {
        int totalMines = plugin.getMineManager().getMines().size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int minesOnPage = Math.min(maxPerPage, totalMines - (page - 1) * maxPerPage);
        if (minesOnPage < 0) minesOnPage = 0;

        int guiSize = ((int) Math.ceil(minesOnPage / 9.0) + 1) * 9;
        if (guiSize < 18) guiSize = 18;
        if (guiSize > 54) guiSize = 54;

        String title = totalPages > 1 ? get(plugin, "gui.main.title-paged").replace("%count%", String.valueOf(totalMines)).replace("%page%", String.valueOf(page))
                : get(plugin, "gui.main.title").replace("%count%", String.valueOf(totalMines));
        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        refreshMainMenu(inv, player, plugin, page);
        player.openInventory(inv);
    }

    public static void refreshStatsMenu(Inventory inv, Player player, PrisonMinesX plugin, int page) {
        int guiSize = inv.getSize();
        List<Mine> mines = new ArrayList<>(plugin.getMineManager().getMines());
        SortType sort = playerSorts.getOrDefault(player.getUniqueId(), SortType.ALPHABETICAL_AZ);

        mines.sort((m1, m2) -> {
            switch (sort) {
                case ALPHABETICAL_ZA: return m2.getName().compareToIgnoreCase(m1.getName());
                case PLAYERS: return Integer.compare(m2.getActivePlayers().size(), m1.getActivePlayers().size());
                case BLOCKS_LEAST: return Double.compare(m1.getPercentRemaining(), m2.getPercentRemaining());
                case BLOCKS_MOST: return Double.compare(m2.getPercentRemaining(), m1.getPercentRemaining());
                case BLOCKS_MINED_PER_RESET: return Long.compare(m2.getLifetimeMinedBlocks() / Math.max(1, m2.getLifetimeResets()), m1.getLifetimeMinedBlocks() / Math.max(1, m1.getLifetimeResets()));
                case TIME_SOONEST: return Integer.compare(m1.getTimeUntilReset(), m2.getTimeUntilReset());
                case TIME_LATEST: return Integer.compare(m2.getTimeUntilReset(), m1.getTimeUntilReset());
                case SIZE: return Integer.compare(m2.getTotalBlocks(), m1.getTotalBlocks());
                case STATUS: return Boolean.compare(m1.isPaused(), m2.isPaused());
                case WORLD: return m1.getWorldName().compareToIgnoreCase(m2.getWorldName());
                case ALPHABETICAL_AZ:
                default: return m1.getName().compareToIgnoreCase(m2.getName());
            }
        });

        int totalMines = mines.size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalMines);
        int slotOffset = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Mine mine = mines.get(i);
            Material mat = Material.matchMaterial(mine.getDisplayItem());
            if (mat == null) mat = Material.DIAMOND_PICKAXE;

            long totalMined = mine.getLifetimeMinedBlocks();
            int totalResets = mine.getLifetimeResets();
            double avgPerReset = totalResets == 0 ? totalMined : (double) totalMined / totalResets;
            String state = mine.isPaused() ? get(plugin, "formats.paused") : get(plugin, "formats.running");
            String desc = mine.getDescription() == null || mine.getDescription().isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', mine.getDescription()) + "\n";

            inv.setItem(slotOffset++, createGuiItem(mat, get(plugin, "gui.main.mine-name").replace("%mine%", mine.getName()),
                    getLore(plugin, "gui.stats.mine-lore",
                            "%description%", desc,
                            "%state%", state,
                            "%lifetime_mined%", formatLargeNumber(totalMined),
                            "%lifetime_resets%", formatLargeNumber(totalResets),
                            "%avg_per_reset%", formatLargeNumber(avgPerReset),
                            "%current_blocks%", formatLargeNumber(mine.getMinedBlocks()),
                            "%total_blocks%", formatLargeNumber(mine.getTotalBlocks()),
                            "%percent_left%", String.format("%.1f", mine.getPercentRemaining()),
                            "%time_left%", TimeUtil.formatTime(mine.getTimeUntilReset()),
                            "%reset_percent%", String.format("%.1f", mine.getResetPercentage()),
                            "%reset_delay%", TimeUtil.formatTime(mine.getResetDelay()),
                            "%fill_mode%", mine.isFillMode() ? "§aEnabled" : "§cDisabled"
                    )));
        }

        int navStart = guiSize - 9;
        for (int i = slotOffset; i < navStart; i++) inv.setItem(i, null);

        if (page > 1) inv.setItem(navStart, createGuiItem(Material.ARROW, get(plugin, "gui.main.prev-page")));
        else inv.setItem(navStart, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (page < totalPages) inv.setItem(guiSize - 1, createGuiItem(Material.ARROW, get(plugin, "gui.main.next-page")));
        else inv.setItem(guiSize - 1, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        List<String> sortLore = new ArrayList<>();
        sortLore.add("§7Click to cycle sorting.");
        sortLore.add("");
        for (SortType type : SortType.values()) {
            if (type == sort) sortLore.add(get(plugin, "formats.sort-active").replace("%name%", type.getName()));
            else sortLore.add(get(plugin, "formats.sort-inactive").replace("%name%", type.getName()));
        }
        sortLore.add("");
        sortLore.add("§bLeft-Click §7to cycle down.");
        sortLore.add("§cRight-Click §7to cycle up.");

        inv.setItem(guiSize - 5, createGuiItem(Material.OAK_SIGN, get(plugin, "gui.main.sort-title"), sortLore));

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = navStart + 1; i < guiSize - 1; i++) {
            if (i == guiSize - 5) continue;
            inv.setItem(i, pane);
        }
    }

    public static void openStatsMenu(Player player, PrisonMinesX plugin, int page) {
        int totalMines = plugin.getMineManager().getMines().size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalMines / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int minesOnPage = Math.min(maxPerPage, totalMines - (page - 1) * maxPerPage);
        if (minesOnPage < 0) minesOnPage = 0;

        int guiSize = ((int) Math.ceil(minesOnPage / 9.0) + 1) * 9;
        if (guiSize < 18) guiSize = 18;
        if (guiSize > 54) guiSize = 54;

        String title = get(plugin, "gui.stats.title").replace("%count%", String.valueOf(totalMines)).replace("%page%", String.valueOf(page));
        Inventory inv = Bukkit.createInventory(null, guiSize, title);

        refreshStatsMenu(inv, player, plugin, page);
        player.openInventory(inv);
    }

    public static void openEditMenu(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, get(plugin, "gui.edit.title").replace("%mine%", mine.getName()));

        inv.setItem(9, createGuiItem(Material.ENDER_EYE, "§3Teleport to Mine", Arrays.asList("§7Click to instantly warp", "§7to this mine.")));
        inv.setItem(11, createGuiItem(Material.ENDER_PEARL, get(plugin, "gui.edit.teleport"), getLore(plugin, "gui.edit.teleport-lore")));
        inv.setItem(13, createGuiItem(Material.EMERALD_BLOCK, get(plugin, "gui.edit.blocks"), getLore(plugin, "gui.edit.blocks-lore")));
        inv.setItem(15, createGuiItem(Material.COMPARATOR, get(plugin, "gui.edit.flags"), getLore(plugin, "gui.edit.flags-lore")));

        String activePlayersTitle = get(plugin, "gui.edit.view-players") + " " + get(plugin, "gui.edit.view-players-count").replace("%count%", String.valueOf(mine.getActivePlayers().size()));
        inv.setItem(17, createGuiItem(Material.PLAYER_HEAD, activePlayersTitle, getLore(plugin, "gui.edit.view-players-lore")));

        inv.setItem(21, createGuiItem(Material.BARRIER, get(plugin, "gui.edit.delete"), getLore(plugin, "gui.edit.delete-lore")));
        inv.setItem(23, createGuiItem(Material.ARROW, get(plugin, "gui.edit.back")));

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        player.openInventory(inv);
    }

    public static void openActivePlayersMenu(Player player, Mine mine, PrisonMinesX plugin, int page) {
        String title = get(plugin, "gui.active-players.title").replace("%mine%", mine.getName()).replace("%page%", String.valueOf(page));

        List<UUID> players = new ArrayList<>(mine.getActivePlayers().keySet());
        int totalPlayers = players.size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalPlayers / maxPerPage);
        if (totalPages == 0) totalPages = 1;

        int pOnPage = Math.min(maxPerPage, totalPlayers - (page - 1) * maxPerPage);
        if (pOnPage < 0) pOnPage = 0;

        int guiSize = (pOnPage <= 9) ? 18 : ((int) Math.ceil(pOnPage / 9.0) + 1) * 9;
        if (guiSize > 54) guiSize = 54;

        Inventory inv = Bukkit.createInventory(null, guiSize, title);
        refreshActivePlayersMenu(player, mine, plugin, page, inv);
        player.openInventory(inv);
    }

    public static void refreshActivePlayersMenu(Player viewer, Mine mine, PrisonMinesX plugin, int page, Inventory inv) {
        int guiSize = inv.getSize();

        List<UUID> players = new ArrayList<>(mine.getActivePlayers().keySet());
        int totalPlayers = players.size();
        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) totalPlayers / maxPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, totalPlayers);

        int slot = 0;
        long now = System.currentTimeMillis();
        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            Mine.PlayerRecord record = mine.getActivePlayers().get(uuid);
            long enteredAt = record.enteredAt;
            int secondsInMine = (int) ((now - enteredAt) / 1000);

            String status = (now - record.lastMined > 10000) ? "&cIDLE" : "&aMINING";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
                ((org.bukkit.inventory.meta.SkullMeta) meta).setOwner("DetectivePlapper");
            }
            meta.setDisplayName(get(plugin, "gui.active-players.head-name").replace("%player%", p.getName()));

            List<String> rawLore = plugin.getMessages().getStringList("gui.active-players.head-lore");
            List<String> formattedLore = new ArrayList<>();
            for (String s : rawLore) {
                if (s.contains("%specifics%")) {
                    if (record.specificBlocksMined.isEmpty()) {
                        formattedLore.add("  §8- §cNone");
                    } else {
                        for (Map.Entry<String, Integer> e : record.specificBlocksMined.entrySet()) {
                            formattedLore.add("  §8- §7" + e.getKey() + ": §b" + e.getValue());
                        }
                    }
                } else {
                    formattedLore.add(s.replace("&", "§")
                            .replace("%time%", TimeUtil.formatTime(secondsInMine))
                            .replace("%status%", status.replace("&", "§"))
                            .replace("%blocks%", String.valueOf(record.blocksMined)));
                }
            }

            meta.setLore(formattedLore);
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        int navStart = guiSize - 9;
        for (int i = slot; i < navStart; i++) inv.setItem(i, null);

        if (page > 1) inv.setItem(navStart, createGuiItem(Material.ARROW, get(plugin, "gui.main.prev-page")));
        else inv.setItem(navStart, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        if (page < totalPages) inv.setItem(guiSize - 1, createGuiItem(Material.ARROW, get(plugin, "gui.main.next-page")));
        else inv.setItem(guiSize - 1, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        inv.setItem(navStart + 4, createGuiItem(Material.ARROW, get(plugin, "gui.flags.back")));

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = navStart; i < guiSize; i++) {
            if (i == navStart || i == guiSize - 1 || i == navStart + 4) continue;
            inv.setItem(i, pane);
        }
    }

    public static void openFlagsMenu(Player player, Mine mine, PrisonMinesX plugin) {
        Inventory inv = Bukkit.createInventory(null, 45, get(plugin, "gui.flags.title").replace("%mine%", mine.getName()));

        inv.setItem(10, createGuiItem(Material.CLOCK, get(plugin, "gui.flags.delay"), getLore(plugin, "gui.flags.delay-lore", "%value%", mine.getResetDelay() / 60 + "m")));

        List<String> warnLore = new ArrayList<>();
        for (String s : plugin.getMessages().getStringList("gui.flags.warnings-lore")) {
            if (s.contains("%warnings%")) {
                if (mine.getResetWarnings().isEmpty()) warnLore.add(" §8- §cNone");
                else for (int w : mine.getResetWarnings()) warnLore.add(" §8- §b" + TimeUtil.formatTime(w));
            } else warnLore.add(s.replace("&", "§"));
        }
        inv.setItem(11, createGuiItem(Material.BELL, get(plugin, "gui.flags.warnings"), warnLore));

        inv.setItem(12, createGuiItem(Material.EXPERIENCE_BOTTLE, get(plugin, "gui.flags.percent"), getLore(plugin, "gui.flags.percent-lore", "%value%", mine.getResetPercentage() == -1.0 ? "Default" : mine.getResetPercentage() + "%")));

        List<String> styleLore = new ArrayList<>();
        styleLore.add("§7Changes the visual pacing of the reset.");
        styleLore.add("");
        String[] styles = {"BOTTOM_UP", "INSTANT", "VERTICAL_SLICES", "CENTER_OUT", "OUTSIDE_IN", "CORNER_SWEEP", "WAVE", "DIAMOND", "SPIRAL"};
        for (String s : styles) {
            String niceName;
            switch (s) {
                case "INSTANT": niceName = "Instant (Explode)"; break;
                case "VERTICAL_SLICES": niceName = "Vertical Slices"; break;
                case "CENTER_OUT": niceName = "Center Out"; break;
                case "OUTSIDE_IN": niceName = "Outside In"; break;
                case "CORNER_SWEEP": niceName = "Corner Sweep"; break;
                case "WAVE": niceName = "Wave"; break;
                case "DIAMOND": niceName = "Diamond Expansion"; break;
                case "SPIRAL": niceName = "Spiral"; break;
                default: niceName = "Bottom-Up"; break;
            }
            if (s.equals(mine.getResetStyle())) styleLore.add(get(plugin, "formats.sort-active").replace("%name%", niceName));
            else styleLore.add(get(plugin, "formats.sort-inactive").replace("%name%", niceName));
        }
        styleLore.add("");
        styleLore.add("§bLeft-Click §7to cycle down.");
        styleLore.add("§cRight-Click §7to cycle up.");

        inv.setItem(13, createGuiItem(Material.PISTON, get(plugin, "gui.flags.style"), styleLore));

        Material surfaceIcon = mine.getSurface() != null ? Material.matchMaterial(mine.getSurface()) : Material.GRASS_BLOCK;
        if (surfaceIcon == null) surfaceIcon = Material.GRASS_BLOCK;
        inv.setItem(14, createGuiItem(surfaceIcon, get(plugin, "gui.flags.surface"), getLore(plugin, "gui.flags.surface-lore", "%value%", mine.getSurface() == null ? "None" : com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getSurface()))));

        Material iconMat = mine.getDisplayItem() != null ? Material.matchMaterial(mine.getDisplayItem()) : Material.DIAMOND_PICKAXE;
        if (iconMat == null) iconMat = Material.DIAMOND_PICKAXE;
        inv.setItem(15, createGuiItem(iconMat, get(plugin, "gui.flags.icon"), getLore(plugin, "gui.flags.icon-lore", "%value%", com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getDisplayItem()))));

        inv.setItem(16, createGuiItem(Material.SPONGE, get(plugin, "gui.flags.fillmode"), getLore(plugin, "gui.flags.fillmode-lore", "%state%", mine.isFillMode() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(19, createGuiItem(Material.NOTE_BLOCK, get(plugin, "gui.flags.silent"), getLore(plugin, "gui.flags.silent-lore", "%state%", mine.isSilent() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        List<String> warnModeLore = new ArrayList<>();
        warnModeLore.add("§7Determines if warnings are sent to the");
        warnModeLore.add("§7entire server, or just nearby players.");
        warnModeLore.add("");
        String[] modes = {"GLOBAL", "NEARBY"};
        for (String mode : modes) {
            String nice = mode.equals("GLOBAL") ? "Global" : "Nearby Only";
            if (mode.equalsIgnoreCase(mine.getWarnMode())) warnModeLore.add(get(plugin, "formats.sort-active").replace("%name%", nice));
            else warnModeLore.add(get(plugin, "formats.sort-inactive").replace("%name%", nice));
        }
        warnModeLore.add("");
        warnModeLore.add("§bLeft-Click §7to cycle down.");
        warnModeLore.add("§cRight-Click §7to cycle up.");
        inv.setItem(20, createGuiItem(Material.OAK_SIGN, get(plugin, "gui.flags.warn-global"), warnModeLore));

        inv.setItem(21, createGuiItem(Material.ENDER_EYE, get(plugin, "gui.flags.tp-reset"), getLore(plugin, "gui.flags.tp-reset-lore", "%state%", mine.isTeleportOnReset() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        String schedStr = mine.getResetSchedules().isEmpty() ? "None" : String.join(", ", mine.getResetSchedules());
        inv.setItem(22, createGuiItem(Material.COMPASS, get(plugin, "gui.flags.schedule"), getLore(plugin, "gui.flags.schedule-lore", "%value%", schedStr)));

        inv.setItem(23, createGuiItem(Material.ARMOR_STAND, get(plugin, "gui.flags.hologram"), getLore(plugin, "gui.flags.hologram-lore", "%state%", mine.isHologramEnabled() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));
        inv.setItem(24, createGuiItem(Material.NAME_TAG, get(plugin, "gui.flags.actionbar"), getLore(plugin, "gui.flags.actionbar-lore", "%state%", mine.isActionbarEnabled() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        Material pauseMat = mine.isPaused() ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK;
        inv.setItem(25, createGuiItem(pauseMat, get(plugin, "gui.flags.pause"), getLore(plugin, "gui.flags.pause-lore", "%state%", mine.isPaused() ? get(plugin, "formats.paused") : get(plugin, "formats.running"))));

        inv.setItem(29, createGuiItem(Material.FEATHER, get(plugin, "gui.flags.mine-fly"), getLore(plugin, "gui.flags.mine-fly-lore", "%state%", mine.isMineFly() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));
        inv.setItem(30, createGuiItem(Material.COOKED_BEEF, get(plugin, "gui.flags.hunger"), getLore(plugin, "gui.flags.hunger-lore", "%state%", mine.isHunger() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));
        inv.setItem(31, createGuiItem(Material.BRICKS, get(plugin, "gui.flags.place-blocks"), getLore(plugin, "gui.flags.place-blocks-lore", "%state%", mine.isPlaceBlocks() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));
        inv.setItem(32, createGuiItem(Material.DIAMOND_BOOTS, get(plugin, "gui.flags.fall-damage"), getLore(plugin, "gui.flags.fall-damage-lore", "%state%", mine.isFallDamage() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));
        inv.setItem(33, createGuiItem(Material.IRON_SWORD, get(plugin, "gui.flags.pvp"), getLore(plugin, "gui.flags.pvp-lore", "%state%", mine.isPvp() ? get(plugin, "formats.enabled") : get(plugin, "formats.disabled"))));

        inv.setItem(40, createGuiItem(Material.ARROW, get(plugin, "gui.flags.back")));

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        player.openInventory(inv);
    }

    public static void openBlockEditor(Player player, Mine mine, PrisonMinesX plugin) {
        List<Map.Entry<String, Double>> sortedComposition = new ArrayList<>(mine.getComposition().entrySet());
        sortedComposition.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int size = sortedComposition.size();
        int rows = (int) Math.ceil(size / 9.0);
        if (rows == 0) rows = 1;

        int guiSize = (rows + 1) * 9;
        if (guiSize > 54) guiSize = 54;

        Inventory inv = Bukkit.createInventory(null, guiSize, get(plugin, "gui.blocks.title").replace("%mine%", mine.getName()));

        int slot = 0;
        double usedSpace = 0.0;
        for (Map.Entry<String, Double> entry : sortedComposition) {
            usedSpace += entry.getValue();
            if (slot >= guiSize - 9) break;

            Material mat = Material.matchMaterial(entry.getKey());
            if (mat == null) mat = Material.STONE;

            String niceName = com.sasha.prisonminesx.commands.MineCommand.formatName(entry.getKey());
            inv.setItem(slot++, createGuiItem(mat, get(plugin, "gui.blocks.block-name").replace("%block%", niceName),
                    getLore(plugin, "gui.blocks.block-lore", "%chance%", String.valueOf(entry.getValue()))));
        }

        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = guiSize - 9; i < guiSize; i++) {
            if (i == guiSize - 9 || i == guiSize - 5 || i == guiSize - 1) continue;
            inv.setItem(i, filler);
        }

        inv.setItem(guiSize - 9, createGuiItem(Material.ARROW, get(plugin, "gui.blocks.back")));

        double available = Math.max(0, 100.0 - usedSpace);
        inv.setItem(guiSize - 5, createGuiItem(Material.PAPER, get(plugin, "gui.blocks.info"), getLore(plugin, "gui.blocks.info-lore", "%used%", String.format("%.1f", usedSpace), "%available%", String.format("%.1f", available))));

        inv.setItem(guiSize - 1, createGuiItem(Material.GOLDEN_PICKAXE, get(plugin, "gui.blocks.force-reset"), getLore(plugin, "gui.blocks.force-reset-lore")));

        player.openInventory(inv);
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