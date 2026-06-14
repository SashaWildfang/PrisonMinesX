package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.models.Mine;
import java.util.Map;

public interface StorageProvider {
    void init();
    Map<String, Mine> loadAllMines();
    Map<String, Mine> loadMinesByWorld(String worldName); // PREMIUM: Lazy Loading Hook
    void saveMine(Mine mine);
    void deleteMine(String mineName);
    void close();
}