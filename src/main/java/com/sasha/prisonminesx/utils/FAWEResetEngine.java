package com.sasha.prisonminesx.utils;

import com.fastasyncworldedit.core.FaweAPI;
import com.sasha.prisonminesx.PrisonMinesX;
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

    /**
     * Resets a mine asynchronously using FastAsyncWorldEdit.
     * * @param mine The mine to reset
     */
    public static void resetMineAsync(Mine mine) {
        World bukkitWorld = Bukkit.getWorld(mine.getWorldName());
        if (bukkitWorld == null) return;

        // 1. Define the 3D Boundaries of the Mine
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ());
        BlockVector3 max = BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ());
        Region region = new CuboidRegion(weWorld, min, max);

        // 2. Build the Block Composition Pattern
        Pattern compositionPattern = buildPattern(mine.getComposition());

        // 3. Execute the FAWE EditSession Asynchronously
        // FAWE handles the async dispatch internally, ensuring zero server lag.
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(weWorld)
                .fastMode(true) // Premium optimization: Skips unnecessary lighting checks during generation
                .build()) {

            editSession.setBlocks(region, compositionPattern);
            editSession.flushSession();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts our Bukkit String->Double map into a WorldEdit RandomPattern.
     */
    private static Pattern buildPattern(Map<String, Double> composition) {
        RandomPattern randomPattern = new RandomPattern();

        if (composition == null || composition.isEmpty()) {
            // Failsafe: If no blocks are set, fill with Air to clear the mine
            randomPattern.add(BlockTypes.AIR.getDefaultState(), 100.0);
            return randomPattern;
        }

        for (Map.Entry<String, Double> entry : composition.entrySet()) {
            Material mat = Material.matchMaterial(entry.getKey());
            if (mat != null && mat.isBlock()) {
                // Adapt the Bukkit Material to a WorldEdit BlockState
                randomPattern.add(BukkitAdapter.asBlockType(mat).getDefaultState(), entry.getValue());
            }
        }

        return randomPattern;
    }
}