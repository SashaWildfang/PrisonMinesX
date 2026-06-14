package com.sasha.prisonminesx;

import com.sasha.prisonminesx.commands.MineCommand;
import com.sasha.prisonminesx.commands.MineTabCompleter;
import com.sasha.prisonminesx.hooks.PrisonMinesXExpansion;
import com.sasha.prisonminesx.listeners.ChatListener;
import com.sasha.prisonminesx.listeners.GUIListener;
import com.sasha.prisonminesx.listeners.MineListener;
import com.sasha.prisonminesx.listeners.WorldListener;
import com.sasha.prisonminesx.managers.HologramManager;
import com.sasha.prisonminesx.managers.MineManager;
import com.sasha.prisonminesx.storage.DatabaseManager;
import com.sasha.prisonminesx.tasks.MineTimerTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PrisonMinesX extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MineManager mineManager;
    private HologramManager hologramManager;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupMessagesFile();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        this.mineManager = new MineManager(this);
        this.hologramManager = new HologramManager(this);

        this.mineManager.loadActiveWorlds();

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        if (getCommand("prisonmines") != null) {
            getCommand("prisonmines").setExecutor(new MineCommand(this));
            getCommand("prisonmines").setTabCompleter(new MineTabCompleter(this));
        }

        new MineTimerTask(this).runTaskTimer(this, 20L, 20L);

        // Hook into PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrisonMinesXExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        getLogger().info("PrisonMinesX has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) hologramManager.removeAll();
        if (databaseManager != null && databaseManager.getProvider() != null) {
            databaseManager.getProvider().close();
        }
        getLogger().info("PrisonMinesX has been disabled.");
    }

    private void setupMessagesFile() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadPlugin() {
        reloadConfig();
        setupMessagesFile();
        getLogger().info("Configurations and messages reloaded.");
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MineManager getMineManager() { return mineManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public FileConfiguration getMessages() { return messagesConfig; }
}