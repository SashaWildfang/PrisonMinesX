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

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySQLProvider implements StorageProvider {

    private final PrisonMinesX plugin;
    private HikariDataSource dataSource;
    private final Gson gson;

    public MySQLProvider(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "prisonminesx");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=" + plugin.getConfig().getBoolean("storage.mysql.use-ssl", false));
        config.setUsername(plugin.getConfig().getString("storage.mysql.username", "root"));
        config.setPassword(plugin.getConfig().getString("storage.mysql.password", ""));

        // Premium optimization: Pool sizing
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);

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
            plugin.getLogger().info("MySQL table verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create MySQL table!");
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Mine> loadAllMines() {
        Map<String, Mine> loadedMines = new HashMap<>();
        String sql = "SELECT * FROM prisonminesx_mines;";

        // Note: We load synchronously on startup so the mines are ready before players join
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Type tokens for Gson deserialization
            Type warningType = new TypeToken<List<Integer>>(){}.getType();
            Type compType = new TypeToken<Map<String, Double>>(){}.getType();

            while (rs.next()) {
                String name = rs.getString("name");
                String worldName = rs.getString("world");
                int minX = rs.getInt("minX");
                int minY = rs.getInt("minY");
                int minZ = rs.getInt("minZ");
                int maxX = rs.getInt("maxX");
                int maxY = rs.getInt("maxY");
                int maxZ = rs.getInt("maxZ");

                Mine mine = new Mine(name, worldName, minX, minY, minZ, maxX, maxY, maxZ);
                mine.setResetDelay(rs.getInt("reset_delay"));
                mine.setSilent(rs.getBoolean("silent"));

                // Parse JSON back to objects
                String warningsJson = rs.getString("reset_warnings");
                if (warningsJson != null && !warningsJson.isEmpty()) {
                    List<Integer> warnings = gson.fromJson(warningsJson, warningType);
                    mine.setResetWarnings(warnings != null ? warnings : new ArrayList<>());
                }

                String compJson = rs.getString("composition");
                if (compJson != null && !compJson.isEmpty()) {
                    Map<String, Double> comp = gson.fromJson(compJson, compType);
                    if (comp != null) {
                        for (Map.Entry<String, Double> entry : comp.entrySet()) {
                            mine.addComposition(entry.getKey(), entry.getValue());
                        }
                    }
                }

                // Parse Teleport Location safely
                double tpX = rs.getDouble("tp_x");
                if (!rs.wasNull()) {
                    World world = Bukkit.getWorld(worldName);
                    double tpY = rs.getDouble("tp_y");
                    double tpZ = rs.getDouble("tp_z");
                    float tpYaw = rs.getFloat("tp_yaw");
                    float tpPitch = rs.getFloat("tp_pitch");
                    mine.setTpLocation(new Location(world, tpX, tpY, tpZ, tpYaw, tpPitch));
                }

                loadedMines.put(name, mine);
            }

            plugin.getLogger().info("Successfully loaded " + loadedMines.size() + " mines from MySQL.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load mines from MySQL!");
            e.printStackTrace();
        }

        return loadedMines;
    }

    @Override
    public void saveMine(Mine mine) {
        String sql = "INSERT INTO prisonminesx_mines (name, world, minX, minY, minZ, maxX, maxY, maxZ, reset_delay, reset_warnings, silent, tp_x, tp_y, tp_z, tp_yaw, tp_pitch, composition) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE world=?, minX=?, minY=?, minZ=?, maxX=?, maxY=?, maxZ=?, reset_delay=?, reset_warnings=?, silent=?, tp_x=?, tp_y=?, tp_z=?, tp_yaw=?, tp_pitch=?, composition=?;";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                String warningsJson = gson.toJson(mine.getResetWarnings());
                String compJson = gson.toJson(mine.getComposition());

                // Set values for the INSERT part
                ps.setString(1, mine.getName());
                ps.setString(2, mine.getWorldName());
                ps.setInt(3, mine.getMinX());
                ps.setInt(4, mine.getMinY());
                ps.setInt(5, mine.getMinZ());
                ps.setInt(6, mine.getMaxX());
                ps.setInt(7, mine.getMaxY());
                ps.setInt(8, mine.getMaxZ());
                ps.setInt(9, mine.getResetDelay());
                ps.setString(10, warningsJson);
                ps.setBoolean(11, mine.isSilent());

                if (mine.getTpLocation() != null) {
                    ps.setDouble(12, mine.getTpLocation().getX());
                    ps.setDouble(13, mine.getTpLocation().getY());
                    ps.setDouble(14, mine.getTpLocation().getZ());
                    ps.setFloat(15, mine.getTpLocation().getYaw());
                    ps.setFloat(16, mine.getTpLocation().getPitch());
                } else {
                    ps.setNull(12, java.sql.Types.DOUBLE);
                    ps.setNull(13, java.sql.Types.DOUBLE);
                    ps.setNull(14, java.sql.Types.DOUBLE);
                    ps.setNull(15, java.sql.Types.FLOAT);
                    ps.setNull(16, java.sql.Types.FLOAT);
                }

                ps.setString(17, compJson);

                // Set values for the ON DUPLICATE KEY UPDATE part
                ps.setString(18, mine.getWorldName());
                ps.setInt(19, mine.getMinX());
                ps.setInt(20, mine.getMinY());
                ps.setInt(21, mine.getMinZ());
                ps.setInt(22, mine.getMaxX());
                ps.setInt(23, mine.getMaxY());
                ps.setInt(24, mine.getMaxZ());
                ps.setInt(25, mine.getResetDelay());
                ps.setString(26, warningsJson);
                ps.setBoolean(27, mine.isSilent());

                if (mine.getTpLocation() != null) {
                    ps.setDouble(28, mine.getTpLocation().getX());
                    ps.setDouble(29, mine.getTpLocation().getY());
                    ps.setDouble(30, mine.getTpLocation().getZ());
                    ps.setFloat(31, mine.getTpLocation().getYaw());
                    ps.setFloat(32, mine.getTpLocation().getPitch());
                } else {
                    ps.setNull(28, java.sql.Types.DOUBLE);
                    ps.setNull(29, java.sql.Types.DOUBLE);
                    ps.setNull(30, java.sql.Types.DOUBLE);
                    ps.setNull(31, java.sql.Types.FLOAT);
                    ps.setNull(32, java.sql.Types.FLOAT);
                }

                ps.setString(33, compJson);

                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save mine to MySQL: " + mine.getName());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void deleteMine(String mineName) {
        String sql = "DELETE FROM prisonminesx_mines WHERE name = ?;";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, mineName);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete mine from MySQL: " + mineName);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}