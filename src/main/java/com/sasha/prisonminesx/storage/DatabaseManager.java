package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.PrisonMinesX;

/**
 * Abstraction layer to route data serialization requests to the user's configured backend type.
 */
public class DatabaseManager {

    private final PrisonMinesX plugin;
    private StorageProvider activeProvider;

    public DatabaseManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    /** Evaluates the config.yml 'storage.type' and launches the corresponding logic. */
    public void initialize() {
        String type = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();

        switch (type) {
            case "MYSQL":
                activeProvider = new MySQLProvider(plugin);
                plugin.getLogger().info("Database hooked: MySQL");
                break;
            case "SQLITE":
                activeProvider = new SQLiteProvider(plugin);
                plugin.getLogger().info("Database hooked: SQLite");
                break;
            case "YAML":
            default:
                activeProvider = new YamlProvider(plugin);
                plugin.getLogger().info("Database hooked: YAML (Local Files)");
                break;
        }

        // Run the setup logic (create tables/files)
        activeProvider.init();
    }

    /** @return The active implementation of StorageProvider. */
    public StorageProvider getProvider() {
        return activeProvider;
    }
}