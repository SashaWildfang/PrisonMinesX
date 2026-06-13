package com.sasha.prisonminesx;

import com.sasha.prisonminesx.storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrisonMinesX extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // 1. Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // 2. Initialize the Database System
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        getLogger().info("PrisonMinesX has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Safely close database connections
        if (databaseManager != null && databaseManager.getProvider() != null) {
            databaseManager.getProvider().close();
        }

        getLogger().info("PrisonMinesX has been disabled.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}