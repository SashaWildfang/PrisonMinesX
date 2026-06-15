package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.models.Mine;
import java.util.Map;

/**
 * Interface guaranteeing standardization across MySQL, SQLite, and Yaml database mechanisms.
 */
public interface StorageProvider {
    void init();
    Map<String, Mine> loadAllMines();

    // Lazy Loading Hook allows DBs to selectively query mines by world.
    Map<String, Mine> loadMinesByWorld(String worldName);

    void saveMine(Mine mine);
    void deleteMine(String mineName);
    void close();
}