package com.sasha.prisonminesx.storage;

import com.google.gson.Gson;
import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQLiteProvider implements StorageProvider {

    private final PrisonMinesX plugin;
    private HikariDataSource dataSource;
    private final Gson gson;

    public SQLiteProvider(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();

        // Ensure the data folder exists before creating the DB file
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File dbFile = new File(plugin.getDataFolder(), "mines.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports 1 concurrent writer safely

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS prisonminesx_mines (" +
                "name VARCHAR(64) PRIMARY KEY, " +
                "world VARCHAR(64), " +
                "minX INT, minY INT, minZ INT, " +
                "maxX INT, maxY INT, maxZ INT, " +
                "reset_delay INT, " +
                "reset_warnings TEXT, " +
                "silent BOOLEAN, " +
                "tp_x DOUBLE, tp_y DOUBLE, tp_z DOUBLE, tp_yaw FLOAT, tp_pitch FLOAT, " +
                "composition TEXT);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
            plugin.getLogger().info("SQLite table verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create SQLite table!");
            e.printStackTrace();
        }
    }

    @Override
    public void saveMine(Mine mine) {
        String sql = "INSERT OR REPLACE INTO prisonminesx_mines (name, world, minX, minY, minZ, maxX, maxY, maxZ, reset_delay, reset_warnings, silent, tp_x, tp_y, tp_z, tp_yaw, tp_pitch, composition) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Binding logic will go here for all 17 parameters
                ps.setString(1, mine.getName());
                // ... (we'll flesh out the exact bindings when we build the full save/load logic)

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save mine to SQLite: " + mine.getName());
                e.printStackTrace();
            }
        });
    }

    @Override
    public Map<String, Mine> loadAllMines() {
        // Will implement the ResultSet parser next!
        return new HashMap<>();
    }

    @Override
    public void deleteMine(String mineName) {
        // Simple DELETE FROM prisonminesx_mines WHERE name = ?
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}