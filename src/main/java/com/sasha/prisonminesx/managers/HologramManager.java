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

        String pausedFlag = mine.isPaused() ? plugin.getConfig().getString("hologram-format.paused-placeholder", "&7(PAUSED)") : "";

        String l1 = plugin.getConfig().getString("hologram-format.line-1", "&b&l%mine% Mine %paused%")
                .replace("%mine%", mine.getName())
                .replace("%paused%", pausedFlag).replace("&", "§").trim();
        stands.get(0).setCustomName(l1);

        String l2 = plugin.getConfig().getString("hologram-format.line-2", "&e%mined%/%total% Blocks Mined &7(&a%percent%% Left&7)")
                .replace("%mined%", String.valueOf(mine.getMinedBlocks()))
                .replace("%total%", String.valueOf(mine.getTotalBlocks()))
                .replace("%percent%", String.format("%.1f", mine.getPercentRemaining())).replace("&", "§");
        stands.get(1).setCustomName(l2);

        String l3 = stands.get(2).getCustomName();
        String flashMsg = plugin.getConfig().getString("hologram-format.forced-reset-line", "&a&lMine was Forcibly reset").replace("&", "§");

        if (l3 == null || !l3.equals(flashMsg)) {
            stands.get(2).setCustomName(plugin.getConfig().getString("hologram-format.line-3", "&7Resets in &c%time%")
                    .replace("%time%", TimeUtil.formatTime(mine.getTimeUntilReset())).replace("&", "§"));
        }
    }

    public void flashForcedReset(Mine mine) {
        if (!mine.isHologramEnabled() || !mine.isSetup()) return;
        List<ArmorStand> stands = holograms.get(mine.getName());
        String flashMsg = plugin.getConfig().getString("hologram-format.forced-reset-line", "&a&lMine was Forcibly reset").replace("&", "§");

        if (stands != null && stands.size() >= 3) {
            stands.get(2).setCustomName(flashMsg);

            // 3 seconds (60 ticks) as requested
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (mine.isHologramEnabled() && holograms.containsKey(mine.getName())) {
                    stands.get(2).setCustomName(plugin.getConfig().getString("hologram-format.line-3", "&7Resets in &c%time%")
                            .replace("%time%", TimeUtil.formatTime(mine.getTimeUntilReset())).replace("&", "§"));
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