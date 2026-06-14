package com.sasha.prisonminesx.utils;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class FAWEResetEngine {

    public static void resetMineLayered(PrisonMinesX plugin, Mine mine) {
        World bukkitWorld = Bukkit.getWorld(mine.getWorldName());
        if (bukkitWorld == null) return;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        Pattern compositionPattern = buildPattern(mine.getComposition());
        boolean isEmpty = (compositionPattern == null);

        int minY = mine.getMinY();
        int maxY = mine.getMaxY();
        int layersPerTick = 10; // Process 10 Y-levels per tick to prevent TPS drops

        new BukkitRunnable() {
            int currentY = minY;

            @Override
            public void run() {
                if (currentY > maxY) {
                    // Apply surface block on the very last tick after filling is done
                    if (mine.getSurface() != null) {
                        applySurface(weWorld, mine);
                    }
                    this.cancel();
                    return;
                }

                int chunkMaxY = Math.min(currentY + layersPerTick - 1, maxY);
                BlockVector3 min = BlockVector3.at(mine.getMinX(), currentY, mine.getMinZ());
                BlockVector3 max = BlockVector3.at(mine.getMaxX(), chunkMaxY, mine.getMaxZ());
                Region chunkRegion = new CuboidRegion(weWorld, min, max);

                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                        .world(weWorld)
                        .fastMode(!isEmpty) // Keep fast mode OFF if wiping to Air
                        .build()) {

                    if (mine.isFillMode()) {
                        editSession.setMask(new BlockTypeMask(editSession.getExtent(), BlockTypes.AIR));
                    }

                    if (isEmpty) {
                        editSession.setBlocks(chunkRegion, BlockTypes.AIR.getDefaultState());
                    } else {
                        editSession.setBlocks(chunkRegion, compositionPattern);
                    }
                    editSession.flushSession();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                currentY += layersPerTick; // Move up for the next tick
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L); // Run every 1 tick
    }

    private static void applySurface(com.sk89q.worldedit.world.World weWorld, Mine mine) {
        Material surfaceMat = Material.matchMaterial(mine.getSurface());
        if (surfaceMat != null) {
            BlockVector3 surfaceMin = BlockVector3.at(mine.getMinX(), mine.getMaxY(), mine.getMinZ());
            BlockVector3 surfaceMax = BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ());
            Region surfaceRegion = new CuboidRegion(weWorld, surfaceMin, surfaceMax);

            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).fastMode(true).build()) {
                editSession.setMask(null);
                editSession.setBlocks(surfaceRegion, BukkitAdapter.asBlockType(surfaceMat).getDefaultState());
                editSession.flushSession();
            }
        }
    }

    private static Pattern buildPattern(Map<String, Double> composition) {
        RandomPattern randomPattern = new RandomPattern();
        double totalWeight = 0;

        if (composition != null && !composition.isEmpty()) {
            for (Map.Entry<String, Double> entry : composition.entrySet()) {
                if (entry.getValue() > 0) {
                    Material mat = Material.matchMaterial(entry.getKey());
                    if (mat != null && mat.isBlock()) {
                        randomPattern.add(BukkitAdapter.asBlockType(mat).getDefaultState(), entry.getValue());
                        totalWeight += entry.getValue();
                    }
                }
            }
        }

        // If the mine is empty, explicitly return a 100% Air pattern to wipe it
        if (totalWeight <= 0) {
            randomPattern.add(BlockTypes.AIR.getDefaultState(), 100.0);
            return randomPattern;
        }

        // If the total composition is less than 100%, fill the exact remaining space with Air
        if (totalWeight < 100.0) {
            double remainingAir = 100.0 - totalWeight;
            randomPattern.add(BlockTypes.AIR.getDefaultState(), remainingAir);
        }

        return randomPattern;
    }
}