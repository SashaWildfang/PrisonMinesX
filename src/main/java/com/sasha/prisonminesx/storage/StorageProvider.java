package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.models.Mine;
import java.util.Map;

public interface StorageProvider {

    /**
     * Initializes the storage (creates files, tables, or pools connections)
     */
    void init();

    /**
     * Loads all mines from the storage medium into memory
     */
    Map<String, Mine> loadAllMines();

    /**
     * Saves a specific mine to the storage medium
     */
    void saveMine(Mine mine);

    /**
     * Deletes a mine from the storage medium
     */
    void deleteMine(String mineName);

    /**
     * Closes connections or saves final states on plugin disable
     */
    void close();
}