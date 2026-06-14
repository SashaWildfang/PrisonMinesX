package com.sasha.prisonminesx.utils;

import com.sasha.prisonminesx.models.Mine;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Map;

public class FAWEResetEngine {

    public static void resetMineAsync(Mine mine) {
        World bukkitWorld = Bukkit.getWorld(mine.getWorldName());
        if (bukkitWorld == null) return;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ());
        BlockVector3 max = BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ());
        Region region = new CuboidRegion(weWorld, min, max);

        Pattern compositionPattern = buildPattern(mine.getComposition());

        // FastMode is set to FALSE so Air blocks actively overwrite existing physical blocks
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld)
                .fastMode(false)
                .build()) {

            editSession.setBlocks(region, compositionPattern);
            editSession.flushSession();

        } catch (Exception e) {
            e.printStackTrace();
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