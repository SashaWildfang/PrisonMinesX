package com.sasha.prisonminesx.tasks;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks player presence within mine boundaries specifically for Flight toggling,
 * region transitions, and Active Player GUI menus.
 * Due to Spatial Hashing, checking player locations against every mine is extremely performant!
 */
public class PlayerTrackerTask extends BukkitRunnable {

    private final PrisonMinesX plugin;
    private final Map<UUID, String> currentMineMap = new HashMap<>();
    private final Set<UUID> grantedFlight = new HashSet<>();

    public PlayerTrackerTask(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    private String getMsg(String path) {
        return plugin.getMessages().getString(path, "").replace("&", "§");
    }

    @Override
    public void run() {
        boolean notify = plugin.getConfig().getBoolean("settings.notify-flight-changes", true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Evaluates instantly through chunk mapping
            Mine insideMine = plugin.getMineManager().getMineAt(p.getLocation());
            String prevMineName = currentMineMap.get(p.getUniqueId());
            String currMineName = insideMine != null ? insideMine.getName() : null;

            // Transitioning Out of a Mine
            if (prevMineName != null && !prevMineName.equals(currMineName)) {
                Mine m = plugin.getMineManager().getMine(prevMineName);
                if (m != null) m.removeActivePlayer(p.getUniqueId());
            }

            if (insideMine != null) {
                insideMine.addActivePlayer(p.getUniqueId());
                currentMineMap.put(p.getUniqueId(), currMineName);

                // Flight requires 2 blocks depth minimum internally (prevents border cheating)
                boolean canFly = false;
                if (insideMine.isMineFly() || p.hasPermission("prisonminesx.minefly")) {
                    if (p.getLocation().getY() <= insideMine.getMaxY() - 0.5) {
                        canFly = true;
                    }
                }

                if (canFly) {
                    if (!p.getAllowFlight()) {
                        p.setAllowFlight(true);
                        grantedFlight.add(p.getUniqueId());
                        if (notify) p.sendMessage(getMsg("prefix") + getMsg("mine.flight-enabled"));
                    }
                } else if (grantedFlight.contains(p.getUniqueId())) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    grantedFlight.remove(p.getUniqueId());
                    if (notify) p.sendMessage(getMsg("prefix") + getMsg("mine.flight-disabled"));
                }
            } else {
                if (prevMineName != null) currentMineMap.remove(p.getUniqueId());

                if (grantedFlight.contains(p.getUniqueId())) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    grantedFlight.remove(p.getUniqueId());
                    if (notify) p.sendMessage(getMsg("prefix") + getMsg("mine.flight-disabled"));
                }
            }
        }

        // Live refresh logic for Active Players menu
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getOpenInventory() != null && viewer.getOpenInventory().getTopInventory() != null) {
                String title = viewer.getOpenInventory().getTitle();
                String activeBase = ChatColor.stripColor(getMsg("gui.active-players.title")).split("%mine%")[0].trim();

                if (ChatColor.stripColor(title).startsWith(activeBase)) {
                    String mineName = ChatColor.stripColor(title).replace(activeBase, "").split("\\|")[0].trim();
                    Mine m = plugin.getMineManager().getMine(mineName);
                    if (m != null) {
                        int page = 1;
                        try {
                            page = Integer.parseInt(ChatColor.stripColor(title).split("Pg ")[1].replace(")", "").trim());
                        } catch (Exception ignored) {}

                        MineGUI.refreshActivePlayersMenu(viewer, m, plugin, page, viewer.getOpenInventory().getTopInventory());
                    }
                }
            }
        }
    }
}