package com.sasha.prisonminesx.managers;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramManager {

    private final PrisonMinesX plugin;
    private final Map<String, List<ArmorStand>> holograms = new HashMap<>();

    public HologramManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    public void updateHologram(Mine mine) {
        if (!mine.isHologramEnabled() || !mine.isSetup()) {
            removeHologram(mine.getName(), mine);
            return;
        }

        List<ArmorStand> stands = holograms.get(mine.getName());

        if (stands != null) {
            for (ArmorStand as : stands) {
                if (!as.isValid() || as.isDead()) {
                    removeHologram(mine.getName(), mine);
                    stands = null;
                    break;
                }
            }
        }

        if (stands == null || stands.isEmpty()) {
            stands = spawnHologram(mine);
            if (stands == null) return;
        }

        // Line 1: Dynamic Mine Title with Paused Status
        String title = "§b§l" + mine.getName() + " Mine";
        if (mine.isPaused()) {
            title += " §7(PAUSED)";
        }
        stands.get(0).setCustomName(title);

        // Line 2: Blocks
        int total = mine.getTotalBlocks();
        int mined = mine.getMinedBlocks();
        double percent = mine.getPercentRemaining();
        stands.get(1).setCustomName("§e" + mined + "/" + total + " Blocks Mined §7(§a" + String.format("%.1f", percent) + "% Left§7)");

        // Line 3: Resets In (FIXED: Added null check to prevent NPE on initial spawn)
        String line3 = stands.get(2).getCustomName();
        if (line3 == null || !line3.contains("Forcibly reset")) {
            stands.get(2).setCustomName("§7Resets in §c" + TimeUtil.formatTime(mine.getTimeUntilReset()));
        }
    }

    // Flashes a manual reset message for 1 second, then reverts to the countdown
    public void flashForcedReset(Mine mine) {
        if (!mine.isHologramEnabled() || !mine.isSetup()) return;
        List<ArmorStand> stands = holograms.get(mine.getName());

        if (stands != null && stands.size() >= 3) {
            stands.get(2).setCustomName("§a§lMine was Forcibly reset");

            // Revert back to normal after 20 ticks (1 second)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (mine.isHologramEnabled() && holograms.containsKey(mine.getName())) {
                    stands.get(2).setCustomName("§7Resets in §c" + TimeUtil.formatTime(mine.getTimeUntilReset()));
                }
            }, 60L);
        }
    }

    private List<ArmorStand> spawnHologram(Mine mine) {
        removeHologram(mine.getName(), mine);

        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return null;

        double x = mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0 + 0.5;
        double y = mine.getMaxY() + 2.5;
        double z = mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0 + 0.5;

        List<ArmorStand> stands = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ArmorStand as = w.spawn(new Location(w, x, y - (i * 0.3), z), ArmorStand.class);
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.addScoreboardTag("pmx_holo_" + mine.getName());
            stands.add(as);
        }
        holograms.put(mine.getName(), stands);
        return stands;
    }

    public void removeHologram(String mineName, Mine mine) {
        List<ArmorStand> stands = holograms.remove(mineName);
        if (stands != null) {
            for (ArmorStand as : stands) {
                if (as.isValid()) as.remove();
            }
        }

        if (mine != null && mine.getWorldName() != null) {
            World w = Bukkit.getWorld(mine.getWorldName());
            if (w != null) {
                for (Entity entity : w.getEntitiesByClass(ArmorStand.class)) {
                    if (entity.getScoreboardTags().contains("pmx_holo_" + mineName)) {
                        entity.remove();
                    }
                }
            }
        }
    }

    public void removeAll() {
        for (List<ArmorStand> stands : holograms.values()) {
            for (ArmorStand as : stands) {
                if (as.isValid()) as.remove();
            }
        }
        holograms.clear();
    }
}