package com.sasha.prisonminesx.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        File dbFile = new File(plugin.getDataFolder(), "mines.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);

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
                "composition TEXT, " +
                "display_item VARCHAR(64), " +
                "reset_percentage DOUBLE, " +
                "fill_mode BOOLEAN, " +
                "surface VARCHAR(64), " +
                "teleport_on_reset BOOLEAN, " +
                "hologram_enabled BOOLEAN, " +
                "actionbar_enabled BOOLEAN, " +
                "warn_global BOOLEAN, " +
                "paused BOOLEAN);";

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
    public Map<String, Mine> loadAllMines() {
        Map<String, Mine> loadedMines = new HashMap<>();
        String sql = "SELECT * FROM prisonminesx_mines;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            Type warningType = new TypeToken<List<Integer>>(){}.getType();
            Type compType = new TypeToken<Map<String, Double>>(){}.getType();

            while (rs.next()) {
                String name = rs.getString("name");
                String worldName = rs.getString("world");
                Mine mine = new Mine(name, worldName, rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ"));

                mine.setResetDelay(rs.getInt("reset_delay"));
                mine.setSilent(rs.getBoolean("silent"));

                mine.setDisplayItem(rs.getString("display_item"));
                mine.setResetPercentage(rs.getDouble("reset_percentage"));
                mine.setFillMode(rs.getBoolean("fill_mode"));
                mine.setSurface(rs.getString("surface"));
                mine.setTeleportOnReset(rs.getBoolean("teleport_on_reset"));
                mine.setHologramEnabled(rs.getBoolean("hologram_enabled"));
                mine.setActionbarEnabled(rs.getBoolean("actionbar_enabled"));
                mine.setWarnGlobal(rs.getBoolean("warn_global"));
                mine.setPaused(rs.getBoolean("paused"));

                String warningsJson = rs.getString("reset_warnings");
                if (warningsJson != null && !warningsJson.isEmpty()) {
                    List<Integer> warnings = gson.fromJson(warningsJson, warningType);
                    mine.setResetWarnings(warnings != null ? warnings : new ArrayList<>());
                }

                String compJson = rs.getString("composition");
                if (compJson != null && !compJson.isEmpty()) {
                    Map<String, Double> comp = gson.fromJson(compJson, compType);
                    if (comp != null) {
                        for (Map.Entry<String, Double> entry : comp.entrySet()) mine.addComposition(entry.getKey(), entry.getValue());
                    }
                }

                double tpX = rs.getDouble("tp_x");
                if (!rs.wasNull()) {
                    World world = Bukkit.getWorld(worldName);
                    mine.setTpLocation(new Location(world, tpX, rs.getDouble("tp_y"), rs.getDouble("tp_z"), rs.getFloat("tp_yaw"), rs.getFloat("tp_pitch")));
                }

                loadedMines.put(name, mine);
            }
            plugin.getLogger().info("Successfully loaded " + loadedMines.size() + " mines from SQLite.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load mines from SQLite!");
            e.printStackTrace();
        }
        return loadedMines;
    }

    @Override
    public void saveMine(Mine mine) {
        String sql = "INSERT OR REPLACE INTO prisonminesx_mines (name, world, minX, minY, minZ, maxX, maxY, maxZ, reset_delay, reset_warnings, silent, tp_x, tp_y, tp_z, tp_yaw, tp_pitch, composition, display_item, reset_percentage, fill_mode, surface, teleport_on_reset, hologram_enabled, actionbar_enabled, warn_global, paused) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mine.getName());
                ps.setString(2, mine.getWorldName());
                ps.setInt(3, mine.getMinX());
                ps.setInt(4, mine.getMinY());
                ps.setInt(5, mine.getMinZ());
                ps.setInt(6, mine.getMaxX());
                ps.setInt(7, mine.getMaxY());
                ps.setInt(8, mine.getMaxZ());
                ps.setInt(9, mine.getResetDelay());
                ps.setString(10, gson.toJson(mine.getResetWarnings()));
                ps.setBoolean(11, mine.isSilent());

                if (mine.getTpLocation() != null) {
                    ps.setDouble(12, mine.getTpLocation().getX());
                    ps.setDouble(13, mine.getTpLocation().getY());
                    ps.setDouble(14, mine.getTpLocation().getZ());
                    ps.setFloat(15, mine.getTpLocation().getYaw());
                    ps.setFloat(16, mine.getTpLocation().getPitch());
                } else {
                    ps.setNull(12, java.sql.Types.DOUBLE); ps.setNull(13, java.sql.Types.DOUBLE); ps.setNull(14, java.sql.Types.DOUBLE);
                    ps.setNull(15, java.sql.Types.FLOAT); ps.setNull(16, java.sql.Types.FLOAT);
                }

                ps.setString(17, gson.toJson(mine.getComposition()));
                ps.setString(18, mine.getDisplayItem());
                ps.setDouble(19, mine.getResetPercentage());
                ps.setBoolean(20, mine.isFillMode());
                ps.setString(21, mine.getSurface());
                ps.setBoolean(22, mine.isTeleportOnReset());
                ps.setBoolean(23, mine.isHologramEnabled());
                ps.setBoolean(24, mine.isActionbarEnabled());
                ps.setBoolean(25, mine.isWarnGlobal());
                ps.setBoolean(26, mine.isPaused());

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save mine to SQLite: " + mine.getName());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void deleteMine(String mineName) {
        String sql = "DELETE FROM prisonminesx_mines WHERE name = ?;";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, mineName);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete mine from SQLite: " + mineName);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}