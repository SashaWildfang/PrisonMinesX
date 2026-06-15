package com.sasha.prisonminesx.tasks;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Iterates over all active mines globally once per second.
 * Decrements active countdown timers, triggers resets, and syncs open UI elements.
 */
public class MineTimerTask extends BukkitRunnable {

    private final PrisonMinesX plugin;
    private final boolean skipEmpty;
    private final int timeOffset;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MineTimerTask(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.skipEmpty = plugin.getConfig().getBoolean("settings.skip-empty-resets", true);
        this.timeOffset = plugin.getConfig().getInt("settings.timezone-offset", 0);
    }

    @Override
    public void run() {
        LocalTime now = LocalTime.now().plusHours(timeOffset);
        String currentTime = now.format(timeFormatter);

        for (Mine mine : plugin.getMineManager().getMines()) {
            if (mine.isHologramEnabled()) {
                plugin.getHologramManager().updateHologram(mine);
            }

            if (mine.isPaused()) continue;

            boolean triggeredCron = false;
            // Iterate over daily real-world schedule strings
            for (String sched : mine.getResetSchedules()) {
                String targetTime = sched.length() == 5 ? sched + ":00" : sched;

                for (int warningSeconds : mine.getResetWarnings()) {
                    String warningTriggerTime = now.plusSeconds(warningSeconds).format(timeFormatter);

                    if (targetTime.equals(warningTriggerTime) && !mine.isSilent()) {
                        if (!(skipEmpty && mine.getMinedBlocks() == 0)) {
                            sendWarning(mine, warningSeconds);
                        }
                    }
                }

                if (targetTime.equals(currentTime)) {
                    triggeredCron = true;
                    break;
                }
            }

            // Fire reset based on Cron scheduling
            if (triggeredCron) {
                if (skipEmpty && mine.getMinedBlocks() == 0) continue;

                // Force sync block updates on main thread to avoid FAWE async initialization crash!
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getMineManager().resetMine(mine.getName()));
                mine.setTimeUntilReset(mine.getResetDelay());
                continue;
            }

            if (mine.getResetDelay() <= 0) continue;

            int currentSeconds = mine.getTimeUntilReset() - 1;

            if (currentSeconds <= 0) {
                if (!(skipEmpty && mine.getMinedBlocks() == 0)) {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getMineManager().resetMine(mine.getName()));
                }
                mine.setTimeUntilReset(mine.getResetDelay());
            } else {
                mine.setTimeUntilReset(currentSeconds);
                if (!mine.isSilent() && mine.getResetWarnings().contains(currentSeconds)) {
                    if (!(skipEmpty && mine.getMinedBlocks() == 0)) {
                        sendWarning(mine, currentSeconds);
                    }
                }
            }

            // Sync Actionbar tracking dynamically
            if (mine.isActionbarEnabled()) {
                String msg = plugin.getConfig().getString("actionbar-format", "&9&l%mine% &8| &b%mined%&8/&b%total% &7Mined &8| &7Resets in: &b%time%")
                        .replace("%mine%", mine.getName())
                        .replace("%mined%", String.valueOf(mine.getMinedBlocks()))
                        .replace("%total%", String.valueOf(mine.getTotalBlocks()))
                        .replace("%time%", TimeUtil.formatTime(currentSeconds))
                        .replace("&", "§");
                sendRadiusMessage(mine, msg, true);
            }
        }

        // Live GUI Updates
        String mainBase = MineGUI.getBaseTitle(plugin, "gui.main.title");
        String statsBase = MineGUI.getBaseTitle(plugin, "gui.stats.title");
        String warpsBase = MineGUI.getBaseTitle(plugin, "gui.warps.title");

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            org.bukkit.inventory.InventoryView view = viewer.getOpenInventory();
            if (view.getTopInventory().getType() != org.bukkit.event.inventory.InventoryType.CHEST) continue;

            String title = view.getTitle();
            String cleanTitle = org.bukkit.ChatColor.stripColor(title);

            if (!mainBase.isEmpty() && cleanTitle.startsWith(mainBase)) {
                int page = 1;
                try {
                    if (title.contains("Pg ")) page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                } catch (Exception ignored) {}
                MineGUI.refreshMainMenu(view.getTopInventory(), viewer, plugin, page);
            }
            else if (!statsBase.isEmpty() && cleanTitle.startsWith(statsBase)) {
                int page = 1;
                try {
                    if (title.contains("Pg ")) page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                } catch (Exception ignored) {}
                MineGUI.refreshStatsMenu(view.getTopInventory(), viewer, plugin, page);
            }
            else if (!warpsBase.isEmpty() && cleanTitle.startsWith(warpsBase)) {
                int page = 1;
                try {
                    if (title.contains("Pg ")) page = Integer.parseInt(cleanTitle.split("Pg ")[1].replace(")", "").trim());
                } catch (Exception ignored) {}

                List<Mine> accessibleMines = new ArrayList<>();
                for (Mine m : plugin.getMineManager().getMines()) {
                    if (viewer.hasPermission("prisonminesx.admin") || viewer.hasPermission("prisonminesx.mine.all") || viewer.hasPermission("prisonminesx.mine." + m.getName().toLowerCase())) {
                        accessibleMines.add(m);
                    }
                }
                MineGUI.refreshWarpsMenu(view.getTopInventory(), viewer, plugin, page, accessibleMines);
            }
        }
    }

    private void sendWarning(Mine mine, int seconds) {
        String warningMsg = plugin.getMessages().getString("mine.reset-warning", "&7The &3%mine% &7mine will refresh in &b%time%&7!")
                .replace("%mine%", mine.getName())
                .replace("%time%", TimeUtil.formatTime(seconds))
                .replace("&", "§");

        if (mine.getWarnMode().equalsIgnoreCase("GLOBAL")) {
            Bukkit.broadcastMessage(warningMsg);
        } else {
            sendRadiusMessage(mine, warningMsg, false);
        }
    }

    private void sendRadiusMessage(Mine mine, String msg, boolean isActionBar) {
        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return;

        org.bukkit.Location center = new org.bukkit.Location(w,
                mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0,
                mine.getMinY(),
                mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0);

        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 2500) {
                if (isActionBar) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                } else {
                    p.sendMessage(msg);
                }
            }
        }
    }
}