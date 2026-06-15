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

/**
 * Local SQLite infrastructure utilizing HikariCP wrapper.
 * Replaces MySQL's "ON DUPLICATE KEY UPDATE" with SQLite's "INSERT OR REPLACE INTO"
 */
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
        config.setMaximumPoolSize(1); // SQLite locks on write, pooling must remain strictly 1

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS prisonminesx_mines (" +
                "name VARCHAR(64) PRIMARY KEY, " +
                "description VARCHAR(255), " +
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
                "warn_mode VARCHAR(16) DEFAULT 'GLOBAL', " +
                "paused BOOLEAN, " +
                "lifetime_mined BIGINT, " +
                "lifetime_resets INT, " +
                "schematic VARCHAR(128), " +
                "reset_style VARCHAR(32), " +
                "reset_schedules TEXT, " +
                "mine_fly BOOLEAN, " +
                "warp_delay INT, " +
                "hunger BOOLEAN, " +
                "fall_damage BOOLEAN, " +
                "pvp BOOLEAN, " +
                "place_blocks BOOLEAN, " +
                "holo_x DOUBLE, holo_y DOUBLE, holo_z DOUBLE);";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();

            try (PreparedStatement upgrade = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN place_blocks BOOLEAN DEFAULT 0;")) { upgrade.execute(); } catch (SQLException ignored) { }
            try (PreparedStatement upgrade2 = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN warn_mode VARCHAR(16) DEFAULT 'GLOBAL';")) { upgrade2.execute(); } catch (SQLException ignored) { }
            try (PreparedStatement upgrade3 = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN holo_x DOUBLE;")) { upgrade3.execute(); } catch (SQLException ignored) { }
            try (PreparedStatement upgrade4 = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN holo_y DOUBLE;")) { upgrade4.execute(); } catch (SQLException ignored) { }
            try (PreparedStatement upgrade5 = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN holo_z DOUBLE;")) { upgrade5.execute(); } catch (SQLException ignored) { }
            try (PreparedStatement upgrade6 = conn.prepareStatement("ALTER TABLE prisonminesx_mines ADD COLUMN description VARCHAR(255) DEFAULT NULL;")) { upgrade6.execute(); } catch (SQLException ignored) { }

            plugin.getLogger().info("SQLite table verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create SQLite table!");
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Mine> loadAllMines() {
        return fetchMinesFromDB("SELECT * FROM prisonminesx_mines;", null);
    }

    @Override
    public Map<String, Mine> loadMinesByWorld(String worldName) {
        return fetchMinesFromDB("SELECT * FROM prisonminesx_mines WHERE world = ?;", worldName);
    }

    private Map<String, Mine> fetchMinesFromDB(String sql, String worldFilter) {
        Map<String, Mine> loadedMines = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (worldFilter != null) {
                ps.setString(1, worldFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                Type warningType = new TypeToken<List<Integer>>(){}.getType();
                Type compType = new TypeToken<Map<String, Double>>(){}.getType();
                Type schedType = new TypeToken<List<String>>(){}.getType();

                while (rs.next()) {
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    Mine mine = new Mine(name, worldName, rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ"));

                    mine.setDescription(rs.getString("description"));
                    mine.setResetDelay(rs.getInt("reset_delay"));
                    mine.setSilent(rs.getBoolean("silent"));

                    mine.setDisplayItem(rs.getString("display_item"));
                    mine.setResetPercentage(rs.getDouble("reset_percentage"));
                    mine.setFillMode(rs.getBoolean("fill_mode"));
                    mine.setSurface(rs.getString("surface"));
                    mine.setTeleportOnReset(rs.getBoolean("teleport_on_reset"));
                    mine.setHologramEnabled(rs.getBoolean("hologram_enabled"));
                    mine.setActionbarEnabled(rs.getBoolean("actionbar_enabled"));

                    String wMode = rs.getString("warn_mode");
                    if (wMode != null) mine.setWarnMode(wMode);

                    mine.setPaused(rs.getBoolean("paused"));
                    mine.setSchematic(rs.getString("schematic"));
                    String rStyle = rs.getString("reset_style");
                    if (rStyle != null) mine.setResetStyle(rStyle);

                    mine.setMineFly(rs.getBoolean("mine_fly"));
                    mine.setWarpDelay(rs.getInt("warp_delay"));
                    mine.setHunger(rs.getBoolean("hunger"));
                    mine.setFallDamage(rs.getBoolean("fall_damage"));
                    mine.setPvp(rs.getBoolean("pvp"));
                    mine.setPlaceBlocks(rs.getBoolean("place_blocks"));
                    mine.setLifetimeMinedBlocks(rs.getLong("lifetime_mined"));
                    mine.setLifetimeResets(rs.getInt("lifetime_resets"));

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

                    String schedJson = rs.getString("reset_schedules");
                    if (schedJson != null && !schedJson.isEmpty()) {
                        List<String> schedules = gson.fromJson(schedJson, schedType);
                        mine.setResetSchedules(schedules != null ? schedules : new ArrayList<>());
                    }

                    double tpX = rs.getDouble("tp_x");
                    if (!rs.wasNull()) {
                        World world = Bukkit.getWorld(worldName);
                        mine.setTpLocation(new Location(world, tpX, rs.getDouble("tp_y"), rs.getDouble("tp_z"), rs.getFloat("tp_yaw"), rs.getFloat("tp_pitch")));
                    }

                    double hX = rs.getDouble("holo_x");
                    if (!rs.wasNull()) {
                        World world = Bukkit.getWorld(worldName);
                        mine.setHologramLocation(new Location(world, hX, rs.getDouble("holo_y"), rs.getDouble("holo_z")));
                    }

                    loadedMines.put(name, mine);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load mines from SQLite!");
            e.printStackTrace();
        }
        return loadedMines;
    }

    @Override
    public void saveMine(Mine mine) {
        String sql = "INSERT OR REPLACE INTO prisonminesx_mines (name, description, world, minX, minY, minZ, maxX, maxY, maxZ, reset_delay, reset_warnings, silent, tp_x, tp_y, tp_z, tp_yaw, tp_pitch, composition, display_item, reset_percentage, fill_mode, surface, teleport_on_reset, hologram_enabled, actionbar_enabled, warn_mode, paused, lifetime_mined, lifetime_resets, schematic, reset_style, reset_schedules, mine_fly, warp_delay, hunger, fall_damage, pvp, place_blocks, holo_x, holo_y, holo_z) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mine.getName());
                ps.setString(2, mine.getDescription());
                ps.setString(3, mine.getWorldName());
                ps.setInt(4, mine.getMinX());
                ps.setInt(5, mine.getMinY());
                ps.setInt(6, mine.getMinZ());
                ps.setInt(7, mine.getMaxX());
                ps.setInt(8, mine.getMaxY());
                ps.setInt(9, mine.getMaxZ());
                ps.setInt(10, mine.getResetDelay());
                ps.setString(11, gson.toJson(mine.getResetWarnings()));
                ps.setBoolean(12, mine.isSilent());

                if (mine.getTpLocation() != null) {
                    ps.setDouble(13, mine.getTpLocation().getX());
                    ps.setDouble(14, mine.getTpLocation().getY());
                    ps.setDouble(15, mine.getTpLocation().getZ());
                    ps.setFloat(16, mine.getTpLocation().getYaw());
                    ps.setFloat(17, mine.getTpLocation().getPitch());
                } else {
                    ps.setNull(13, java.sql.Types.DOUBLE); ps.setNull(14, java.sql.Types.DOUBLE); ps.setNull(15, java.sql.Types.DOUBLE);
                    ps.setNull(16, java.sql.Types.FLOAT); ps.setNull(17, java.sql.Types.FLOAT);
                }

                ps.setString(18, gson.toJson(mine.getComposition()));
                ps.setString(19, mine.getDisplayItem());
                ps.setDouble(20, mine.getResetPercentage());
                ps.setBoolean(21, mine.isFillMode());
                ps.setString(22, mine.getSurface());
                ps.setBoolean(23, mine.isTeleportOnReset());
                ps.setBoolean(24, mine.isHologramEnabled());
                ps.setBoolean(25, mine.isActionbarEnabled());
                ps.setString(26, mine.getWarnMode());
                ps.setBoolean(27, mine.isPaused());
                ps.setLong(28, mine.getLifetimeMinedBlocks());
                ps.setInt(29, mine.getLifetimeResets());
                ps.setString(30, mine.getSchematic());
                ps.setString(31, mine.getResetStyle());
                ps.setString(32, gson.toJson(mine.getResetSchedules()));
                ps.setBoolean(33, mine.isMineFly());
                ps.setInt(34, mine.getWarpDelay());
                ps.setBoolean(35, mine.isHunger());
                ps.setBoolean(36, mine.isFallDamage());
                ps.setBoolean(37, mine.isPvp());
                ps.setBoolean(38, mine.isPlaceBlocks());

                if (mine.getHologramLocation() != null) {
                    ps.setDouble(39, mine.getHologramLocation().getX());
                    ps.setDouble(40, mine.getHologramLocation().getY());
                    ps.setDouble(41, mine.getHologramLocation().getZ());
                } else {
                    ps.setNull(39, java.sql.Types.DOUBLE); ps.setNull(40, java.sql.Types.DOUBLE); ps.setNull(41, java.sql.Types.DOUBLE);
                }

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