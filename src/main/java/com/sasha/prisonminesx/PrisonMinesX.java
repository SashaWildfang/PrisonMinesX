package com.sasha.prisonminesx;

import com.sasha.prisonminesx.api.PrisonMinesAPI;
import com.sasha.prisonminesx.commands.MineCommand;
import com.sasha.prisonminesx.commands.MineTabCompleter;
import com.sasha.prisonminesx.gui.MineGUI;
import com.sasha.prisonminesx.hooks.PrisonMinesXExpansion;
import com.sasha.prisonminesx.listeners.ChatListener;
import com.sasha.prisonminesx.listeners.GUIListener;
import com.sasha.prisonminesx.listeners.MineListener;
import com.sasha.prisonminesx.listeners.PlayerListener;
import com.sasha.prisonminesx.listeners.WorldListener;
import com.sasha.prisonminesx.managers.HologramManager;
import com.sasha.prisonminesx.managers.MineManager;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.storage.DatabaseManager;
import com.sasha.prisonminesx.tasks.MineTimerTask;
import com.sasha.prisonminesx.tasks.PlayerTrackerTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * PrisonMinesX
 * Developed by SashaTheSnep
 * The main core class responsible for initializing managers, tasks, and registering listeners.
 */
public final class PrisonMinesX extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MineManager mineManager;
    private HologramManager hologramManager;
    private FileConfiguration messagesConfig;
    private PrisonMinesAPI api;

    @Override
    public void onEnable() {
        // Generates the default configuration files if they do not exist
        saveDefaultConfig();
        setupMessagesFile();

        // CACHE FIX: Generates the O(1) GUI Title Map to prevent massive string operations
        MineGUI.cacheTitles(this);

        // Initialize the Storage infrastructure (MySQL, SQLite, YAML)
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        // Initialize core management objects
        this.mineManager = new MineManager(this);
        this.hologramManager = new HologramManager(this);
        this.api = new PrisonMinesAPI(this);

        // Fetch all mines natively from the persistent storage
        this.mineManager.loadActiveWorlds();

        // Register core mechanics
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Bind Command handling
        if (getCommand("prisonmines") != null) {
            getCommand("prisonmines").setExecutor(new MineCommand(this));
            getCommand("prisonmines").setTabCompleter(new MineTabCompleter(this));
        }

        // Initialize asynchronous tasks
        new MineTimerTask(this).runTaskTimer(this, 20L, 20L);
        new PlayerTrackerTask(this).runTaskTimer(this, 10L, 10L);

        // Attempt PlaceholderAPI soft-depend injection
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrisonMinesXExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        getLogger().info("PrisonMinesX has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Destroy all holograms to prevent ghosting on server reloads
        if (hologramManager != null) hologramManager.removeAll();

        // Safely close active database connections
        if (databaseManager != null && databaseManager.getProvider() != null) {
            databaseManager.getProvider().close();
        }
        getLogger().info("PrisonMinesX has been disabled.");
    }

    /**
     * Initializes the messages.yml configuration natively using standard Bukkit API.
     */
    private void setupMessagesFile() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Reloads configuration mappings and flushes the cache directly from persistent storage.
     * Invoked via `/pmines reload`
     */
    public void reloadPlugin() {
        saveDefaultConfig();
        reloadConfig();
        setupMessagesFile();

        // Refresh GUI cache logic
        MineGUI.cacheTitles(this);

        // Close inventories to prevent interaction with orphaned objects
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().contains("Mine")) {
                p.closeInventory();
            }
        }

        // Erase holograms prior to wiping cache
        if (hologramManager != null) {
            hologramManager.removeAll();
        }

        // Re-authenticate storage
        if (databaseManager != null) {
            databaseManager.initialize();
        }

        // Repopulate memory safely from storage
        if (mineManager != null) {
            mineManager.loadActiveWorlds();
            for (Mine m : mineManager.getMines()) {
                if (m.isHologramEnabled()) {
                    hologramManager.updateHologram(m);
                }
            }
        }

        getLogger().info("Configurations, messages, and active mines reloaded.");
    }

    // --- Accessor Methods ---
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MineManager getMineManager() { return mineManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public FileConfiguration getMessages() { return messagesConfig; }
    public PrisonMinesAPI getAPI() { return api; }
}