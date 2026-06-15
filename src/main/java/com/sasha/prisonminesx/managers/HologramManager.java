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
import java.util.List;
import java.util.Set;

public class HologramManager {

    private final PrisonMinesX plugin;

    public HologramManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    public void updateHologram(Mine mine) {
        if (!mine.isHologramEnabled() || !mine.isSetup()) {
            removeHologram(mine.getName(), mine);
            return;
        }

        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return;

        double expectedX = mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0 + 0.5;
        double expectedY = mine.getMaxY() + 2.5;
        double expectedZ = mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0 + 0.5;

        // Overlay with custom location if it has been specifically moved
        Location holoLoc = mine.getHologramLocation();
        if (holoLoc != null && holoLoc.getWorld() != null && holoLoc.getWorld().getName().equals(w.getName())) {
            expectedX = holoLoc.getX();
            expectedY = holoLoc.getY();
            expectedZ = holoLoc.getZ();
        }

        int chunkX = (int) expectedX >> 4;
        int chunkZ = (int) expectedZ >> 4;

        if (!w.isChunkLoaded(chunkX, chunkZ)) return;

        ArmorStand[] stands = new ArmorStand[3];
        List<ArmorStand> ghostsToKill = new ArrayList<>();

        for (Entity ent : w.getChunkAt(chunkX, chunkZ).getEntities()) {
            if (ent instanceof ArmorStand) {
                Set<String> tags = ent.getScoreboardTags();

                if (tags.contains("pmx_holo_" + mine.getName() + "_0")) {
                    if (stands[0] == null) stands[0] = (ArmorStand) ent; else ghostsToKill.add((ArmorStand) ent);
                }
                else if (tags.contains("pmx_holo_" + mine.getName() + "_1")) {
                    if (stands[1] == null) stands[1] = (ArmorStand) ent; else ghostsToKill.add((ArmorStand) ent);
                }
                else if (tags.contains("pmx_holo_" + mine.getName() + "_2")) {
                    if (stands[2] == null) stands[2] = (ArmorStand) ent; else ghostsToKill.add((ArmorStand) ent);
                }
                else if (tags.contains("pmx_holo_" + mine.getName())) {
                    ghostsToKill.add((ArmorStand) ent);
                }
            }
        }

        for (ArmorStand ghost : ghostsToKill) {
            ghost.remove();
        }

        for (int i = 0; i < 3; i++) {
            if (stands[i] == null) {
                Location loc = new Location(w, expectedX, expectedY - (i * 0.3), expectedZ);
                int finalI = i;
                ArmorStand as = w.spawn(loc, ArmorStand.class, entity -> {
                    entity.setVisible(false);
                    entity.setGravity(false);
                    entity.setMarker(true);
                    entity.setCustomNameVisible(true);
                    entity.addScoreboardTag("pmx_holo_" + mine.getName() + "_" + finalI);
                });
                stands[i] = as;
            }
        }

        Location topLoc = stands[0].getLocation();
        if (topLoc.distanceSquared(new Location(w, expectedX, expectedY, expectedZ)) > 0.1) {
            for (int i = 0; i < 3; i++) {
                stands[i].teleport(new Location(w, expectedX, expectedY - (i * 0.3), expectedZ));
            }
        }

        // Line 1
        String pausedFlag = mine.isPaused() ? plugin.getConfig().getString("hologram-format.paused-placeholder", "&7(PAUSED)") : "";
        String l1 = plugin.getConfig().getString("hologram-format.line-1", "&9&l%mine% Mine %paused%")
                .replace("%mine%", mine.getName())
                .replace("%paused%", pausedFlag).replace("&", "§").trim();
        stands[0].setCustomName(l1);

        // Line 2
        String l2 = plugin.getConfig().getString("hologram-format.line-2", "&b%mined%&8/&b%total% &7Blocks Mined &8(&b%percent%% &7Left&8)")
                .replace("%mined%", String.valueOf(mine.getMinedBlocks()))
                .replace("%total%", String.valueOf(mine.getTotalBlocks()))
                .replace("%percent%", String.format("%.1f", mine.getPercentRemaining())).replace("&", "§");
        stands[1].setCustomName(l2);

        // Line 3 (Only updates if flash cooldown has passed)
        if (System.currentTimeMillis() >= mine.getHologramFlashUntil()) {
            String timerMsg = plugin.getConfig().getString("hologram-format.line-3", "&7Resets in &b%time%")
                    .replace("%time%", TimeUtil.formatTime(mine.getTimeUntilReset())).replace("&", "§");

            String l3 = stands[2].getCustomName();
            if (l3 == null || !l3.equals(timerMsg)) {
                stands[2].setCustomName(timerMsg);
            }
        }
    }

    public void flashForcedReset(Mine mine) {
        if (!mine.isHologramEnabled() || !mine.isSetup()) return;

        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return;

        mine.setHologramFlashUntil(System.currentTimeMillis() + 3000);

        double expectedX = mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0 + 0.5;
        double expectedZ = mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0 + 0.5;

        Location holoLoc = mine.getHologramLocation();
        if (holoLoc != null && holoLoc.getWorld() != null && holoLoc.getWorld().getName().equals(w.getName())) {
            expectedX = holoLoc.getX();
            expectedZ = holoLoc.getZ();
        }

        if (!w.isChunkLoaded((int)expectedX >> 4, (int)expectedZ >> 4)) return;

        String flashMsg = plugin.getConfig().getString("hologram-format.forced-reset-line", "&3&lMine was forcibly reset!").replace("&", "§");

        for (Entity ent : w.getChunkAt((int)expectedX >> 4, (int)expectedZ >> 4).getEntities()) {
            if (ent instanceof ArmorStand && ent.getScoreboardTags().contains("pmx_holo_" + mine.getName() + "_2")) {
                ent.setCustomName(flashMsg);
                break;
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateHologram(mine), 65L);
    }

    public void removeHologram(String mineName, Mine mine) {
        if (mine == null || mine.getWorldName() == null) return;
        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return;

        for (org.bukkit.Chunk chunk : w.getLoadedChunks()) {
            for (Entity ent : chunk.getEntities()) {
                if (ent instanceof ArmorStand) {
                    Set<String> tags = ent.getScoreboardTags();
                    if (tags.contains("pmx_holo_" + mineName + "_0") ||
                            tags.contains("pmx_holo_" + mineName + "_1") ||
                            tags.contains("pmx_holo_" + mineName + "_2") ||
                            tags.contains("pmx_holo_" + mineName)) {
                        ent.remove();
                    }
                }
            }
        }
    }

    public void removeAll() {
        for (World w : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : w.getLoadedChunks()) {
                for (Entity ent : chunk.getEntities()) {
                    if (ent instanceof ArmorStand) {
                        for (String tag : ent.getScoreboardTags()) {
                            if (tag.startsWith("pmx_holo_")) {
                                ent.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}