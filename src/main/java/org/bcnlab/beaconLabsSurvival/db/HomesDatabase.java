package org.bcnlab.beaconLabsSurvival.db;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class HomesDatabase {
    private final String url;

    public HomesDatabase(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS homes (uuid TEXT, name TEXT, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, PRIMARY KEY(uuid, name))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void setHome(Player player, String name, Location loc) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        ) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteHome(Player player, String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM homes WHERE uuid = ? AND name = ?")
        ) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Location getHome(Player player, String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM homes WHERE uuid = ? AND name = ?")
        ) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String world = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    return new Location(org.bukkit.Bukkit.getWorld(world), x, y, z, yaw, pitch);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getHomes(Player player) {
        List<String> homes = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM homes WHERE uuid = ?")
        ) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    homes.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    public int countHomes(Player player) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM homes WHERE uuid = ?")
        ) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
