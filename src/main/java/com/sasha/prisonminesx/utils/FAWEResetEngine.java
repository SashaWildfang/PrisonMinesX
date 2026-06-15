package com.sasha.prisonminesx.utils;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.api.events.MinePostResetEvent;
import com.sasha.prisonminesx.models.Mine;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FAWEResetEngine {

    private static class BlockPool {
        private final List<BlockState> blocks = new ArrayList<>();
        private final List<Double> thresholds = new ArrayList<>();
        private final Random random = new Random();

        public BlockPool(Map<String, Double> composition) {
            double runningTotal = 0;
            if (composition != null) {
                for (Map.Entry<String, Double> entry : composition.entrySet()) {
                    if (entry.getValue() > 0) {
                        Material mat = Material.matchMaterial(entry.getKey());
                        if (mat != null && mat.isBlock()) {
                            runningTotal += entry.getValue();
                            blocks.add(BukkitAdapter.asBlockType(mat).getDefaultState());
                            thresholds.add(runningTotal);
                        }
                    }
                }
            }
        }

        public BlockState pickRandom() {
            if (blocks.isEmpty()) return BlockTypes.AIR.getDefaultState();

            // Evaluates purely out of 100%.
            // If runningTotal only reached 20%, rolling an 80 yields AIR.
            double roll = random.nextDouble() * 100.0;
            for (int i = 0; i < thresholds.size(); i++) {
                if (roll <= thresholds.get(i)) return blocks.get(i);
            }
            return BlockTypes.AIR.getDefaultState();
        }
    }

    public static void resetMineLayered(PrisonMinesX plugin, Mine mine) {
        World bukkitWorld = Bukkit.getWorld(mine.getWorldName());
        if (bukkitWorld == null) return;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockPool pool = new BlockPool(mine.getComposition());

        if (mine.getSchematic() != null && !mine.getSchematic().isEmpty()) {
            resetFromSchematic(plugin, mine, weWorld, pool);
            return;
        }

        boolean isEmpty = (mine.getComposition() == null || mine.getComposition().isEmpty());
        String style = mine.getResetStyle();

        if ("INSTANT".equalsIgnoreCase(style)) {
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).fastMode(true).build()) {
                Region region = new CuboidRegion(weWorld, BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ()), BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ()));
                if (mine.isFillMode()) editSession.setMask(new BlockTypeMask(editSession.getExtent(), BlockTypes.AIR));

                for (BlockVector3 pt : region) {
                    editSession.setBlock(pt, isEmpty ? BlockTypes.AIR.getDefaultState() : pool.pickRandom());
                }

                applySurface(editSession, weWorld, mine);
                editSession.flushSession();
            } catch (Exception e) { e.printStackTrace(); }

            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new MinePostResetEvent(mine)));
            return;
        }

        long animSpeed = plugin.getConfig().getLong("settings.reset-animation-speed", 2L);
        long animDelayMs = animSpeed * 50L;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int totalFrames = 20;
            List<BlockVector3>[] frames = new ArrayList[totalFrames];
            for (int i = 0; i < totalFrames; i++) frames[i] = new ArrayList<>();

            int w = mine.getMaxX() - mine.getMinX();
            int h = mine.getMaxY() - mine.getMinY();
            int d = mine.getMaxZ() - mine.getMinZ();

            for (int x = 0; x <= w; x++) {
                for (int y = 0; y <= h; y++) {
                    for (int z = 0; z <= d; z++) {
                        int frame = getAnimationFrame(x, y, z, w, h, d, style, totalFrames);
                        frames[frame].add(BlockVector3.at(mine.getMinX() + x, mine.getMinY() + y, mine.getMinZ() + z));
                    }
                }
            }

            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .fastMode(!isEmpty)
                    .build()) {

                if (mine.isFillMode()) {
                    editSession.setMask(new BlockTypeMask(editSession.getExtent(), BlockTypes.AIR));
                }

                for (int currentTick = 0; currentTick < totalFrames; currentTick++) {
                    List<BlockVector3> blockList = frames[currentTick];

                    if (blockList != null && !blockList.isEmpty()) {
                        for (BlockVector3 vec : blockList) {
                            editSession.setBlock(vec, isEmpty ? BlockTypes.AIR.getDefaultState() : pool.pickRandom());
                        }
                        editSession.flushSession();
                        blockList.clear();
                    }
                    frames[currentTick] = null;

                    try {
                        Thread.sleep(animDelayMs);
                    } catch (InterruptedException ignored) {}
                }

                applySurface(editSession, weWorld, mine);
                editSession.flushSession();

                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new MinePostResetEvent(mine)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static int getAnimationFrame(int x, int y, int z, int w, int h, int d, String style, int totalFrames) {
        double safeW = Math.max(1.0, w);
        double safeH = Math.max(1.0, h);
        double safeD = Math.max(1.0, d);

        double cx = Math.max(0.001, w / 2.0);
        double cz = Math.max(0.001, d / 2.0);

        double value = 0;

        switch (style.toUpperCase()) {
            case "VERTICAL_SLICES": value = (double) x / safeW; break;
            case "CENTER_OUT": value = Math.max(Math.abs(x - cx) / cx, Math.abs(z - cz) / cz); break;
            case "OUTSIDE_IN": value = 1.0 - Math.max(Math.abs(x - cx) / cx, Math.abs(z - cz) / cz); break;
            case "CORNER_SWEEP": value = (x / safeW + z / safeD) / 2.0; break;
            case "WAVE":
                double wave = (x / safeW) + Math.sin((z / safeD) * Math.PI * 2) * 0.2;
                value = (wave + 0.2) / 1.4;
                break;
            case "DIAMOND":
                double manhattan = Math.abs(x - cx) + Math.abs(z - cz);
                value = manhattan / (cx + cz);
                break;
            case "SPIRAL":
                double angle = Math.atan2(z - cz, x - cx);
                double normAngle = (angle + Math.PI) / (2 * Math.PI);
                double rad = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                double normRad = rad / Math.sqrt(cx * cx + cz * cz);
                double swirl = normAngle * 2.0 + normRad * 3.0;
                value = swirl % 1.0;
                if (value < 0) value += 1.0;
                break;
            case "TOP_DOWN": value = 1.0 - (y / safeH); break;
            case "BOTTOM_UP":
            default: value = y / safeH; break;
        }

        int tick = (int) Math.round(value * (totalFrames - 1));
        return Math.max(0, Math.min(totalFrames - 1, tick));
    }

    private static void resetFromSchematic(PrisonMinesX plugin, Mine mine, com.sk89q.worldedit.world.World weWorld, BlockPool pool) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File schemFile = new File("plugins/FastAsyncWorldEdit/schematics/" + mine.getSchematic());
            if (!schemFile.exists()) return;
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) return;

            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                Clipboard clipboard = reader.read();
                try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {

                    Region mineRegion = new CuboidRegion(weWorld, BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ()), BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ()));

                    // 1. Instantly clear the entire mine boundary BEFORE pasting!
                    editSession.setBlocks(mineRegion, BlockTypes.AIR.getDefaultState());

                    // 2. Calculate the exact offset so the schematic pastes perfectly.
                    BlockVector3 toPaste = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ())
                            .add(clipboard.getOrigin().subtract(clipboard.getMinimumPoint()));

                    // 3. Paste the clipboard operation.
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(toPaste)
                            .ignoreAirBlocks(false)
                            .build();

                    Operations.complete(operation);

                    // 4. Find all Sponges in the CLIPBOARD and overwrite them in the edit session!
                    Material placeholderMat = Material.matchMaterial(plugin.getConfig().getString("settings.schematic-placeholder-block", "SPONGE"));
                    if (placeholderMat != null) {
                        BlockState phState = BukkitAdapter.asBlockType(placeholderMat).getDefaultState();
                        BlockVector3 mineMin = BlockVector3.at(mine.getMinX(), mine.getMinY(), mine.getMinZ());
                        BlockVector3 clipMin = clipboard.getMinimumPoint();

                        for (BlockVector3 pt : clipboard.getRegion()) {
                            if (clipboard.getBlock(pt).getBlockType().equals(phState.getBlockType())) {
                                BlockVector3 worldPt = mineMin.add(pt.subtract(clipMin));

                                // Ensure we only overwrite blocks strictly within the mine boundary
                                if (mineRegion.contains(worldPt)) {
                                    editSession.setBlock(worldPt, pool.pickRandom());
                                }
                            }
                        }
                    }

                    // Push the combined clear/paste/replace transaction to the world simultaneously.
                    editSession.flushSession();
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new MinePostResetEvent(mine)));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private static void applySurface(EditSession editSession, com.sk89q.worldedit.world.World weWorld, Mine mine) {
        if (mine.getSurface() != null) {
            Material surfaceMat = Material.matchMaterial(mine.getSurface());
            if (surfaceMat != null) {
                Region surfaceRegion = new CuboidRegion(weWorld, BlockVector3.at(mine.getMinX(), mine.getMaxY(), mine.getMinZ()), BlockVector3.at(mine.getMaxX(), mine.getMaxY(), mine.getMaxZ()));
                editSession.setMask(null);
                try { editSession.setBlocks(surfaceRegion, BukkitAdapter.asBlockType(surfaceMat).getDefaultState()); } catch (Exception ignored) {}
            }
        }
    }
}