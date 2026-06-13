package com.sasha.prisonminesx.storage;

import com.sasha.prisonminesx.PrisonMinesX;
import org.bukkit.Bukkit;

public class DatabaseManager {

    private final PrisonMinesX plugin;
    private StorageProvider activeProvider;

    public DatabaseManager(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

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

    public StorageProvider getProvider() {
        return activeProvider;
    }
}