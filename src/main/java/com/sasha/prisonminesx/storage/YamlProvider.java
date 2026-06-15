package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.MineSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YamlProvider implements StorageProvider {

    private final PrisonMinesX plugin;
    private final File minesFolder;

    public YamlProvider(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.minesFolder = new File(plugin.getDataFolder(), "mines");
    }

    @Override
    public void init() {
        if (!minesFolder.exists()) {
            minesFolder.mkdirs();
            plugin.getLogger().info("Created mines directory for YAML storage.");
        }
    }

    @Override
    public Map<String, Mine> loadAllMines() {
        return fetchMines(null);
    }

    @Override
    public Map<String, Mine> loadMinesByWorld(String worldName) {
        return fetchMines(worldName);
    }

    private Map<String, Mine> fetchMines(String targetWorld) {
        Map<String, Mine> loadedMines = new HashMap<>();
        File[] files = minesFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) return loadedMines;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String world = config.getString("world");

            if (targetWorld == null || (world != null && world.equals(targetWorld))) {
                String mineName = file.getName().replace(".yml", "");
                try {
                    Mine mine = MineSerializer.deserializeFromYaml(mineName, config);
                    loadedMines.put(mineName, mine);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load mine: " + mineName);
                    e.printStackTrace();
                }
            }
        }
        return loadedMines;
    }

    @Override
    public void saveMine(Mine mine) {
        File file = new File(minesFolder, mine.getName() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        MineSerializer.serializeToYaml(mine, config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMine(String mineName) {
        File file = new File(minesFolder, mineName + ".yml");
        if (file.exists()) file.delete();
    }

    @Override
    public void close() {
        // Nothing specific needs to be closed for standard YAML operations
    }
}