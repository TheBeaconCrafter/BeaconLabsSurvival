package org.bcnlab.beaconLabsSurvival.db;

import java.sql.*;
import java.util.*;
import java.util.UUID;

public class ChunksDatabase {
    private final String url;

    public ChunksDatabase(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS claims (world TEXT, chunkX INT, chunkZ INT, owner TEXT, PRIMARY KEY(world, chunkX, chunkZ))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS trusted (world TEXT, chunkX INT, chunkZ INT, trusted TEXT, PRIMARY KEY(world, chunkX, chunkZ, trusted))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS notify_prefs (uuid TEXT PRIMARY KEY, enabled INTEGER)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS denied (world TEXT, chunkX INT, chunkZ INT, denied TEXT, PRIMARY KEY(world, chunkX, chunkZ, denied))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public boolean claimChunk(String world, int chunkX, int chunkZ, UUID owner) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO claims (world, chunkX, chunkZ, owner) VALUES (?, ?, ?, ?)")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, owner.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean unclaimChunk(String world, int chunkX, int chunkZ, UUID owner) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM claims WHERE world = ? AND chunkX = ? AND chunkZ = ? AND owner = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, owner.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public UUID getChunkOwner(String world, int chunkX, int chunkZ) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT owner FROM claims WHERE world = ? AND chunkX = ? AND chunkZ = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("owner"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Notification preferences ---
    public boolean isNotifyEnabled(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT enabled FROM notify_prefs WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("enabled") != 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // default: enabled
    }

    public void setNotifyEnabled(UUID uuid, boolean enabled) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO notify_prefs (uuid, enabled) VALUES (?, ?);")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Deny/Allow system ---
    public boolean denyPlayer(String world, int chunkX, int chunkZ, String playerOrStar) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO denied (world, chunkX, chunkZ, denied) VALUES (?, ?, ?, ?)")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, playerOrStar);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean allowPlayer(String world, int chunkX, int chunkZ, String playerOrStar) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM denied WHERE world = ? AND chunkX = ? AND chunkZ = ? AND denied = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, playerOrStar);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isDenied(String world, int chunkX, int chunkZ, UUID uuid) {
        // Trust always overrides deny
        if (getTrusted(world, chunkX, chunkZ).contains(uuid)) return false;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT denied FROM denied WHERE world = ? AND chunkX = ? AND chunkZ = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String denied = rs.getString("denied");
                if (denied.equals("*") || denied.equals(uuid.toString())) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getDenied(String world, int chunkX, int chunkZ) {
        List<String> deniedList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT denied FROM denied WHERE world = ? AND chunkX = ? AND chunkZ = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                deniedList.add(rs.getString("denied"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deniedList;
    }

    public boolean setDenyAll(String world, int chunkX, int chunkZ) {
        return denyPlayer(world, chunkX, chunkZ, "*");
    }
    public boolean clearDenyAll(String world, int chunkX, int chunkZ) {
        return allowPlayer(world, chunkX, chunkZ, "*");
    }
    public boolean isDenyAll(String world, int chunkX, int chunkZ) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT denied FROM denied WHERE world = ? AND chunkX = ? AND chunkZ = ? AND denied = '*'")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isChunkClaimed(String world, int chunkX, int chunkZ) {
        return getChunkOwner(world, chunkX, chunkZ) != null;
    }

    public int getClaimedCount(UUID owner) {
        String sql = "SELECT COUNT(*) FROM claims WHERE owner = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean trustPlayer(String world, int chunkX, int chunkZ, UUID trusted) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO trusted (world, chunkX, chunkZ, trusted) VALUES (?, ?, ?, ?)")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, trusted.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean untrustPlayer(String world, int chunkX, int chunkZ, UUID trusted) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM trusted WHERE world = ? AND chunkX = ? AND chunkZ = ? AND trusted = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setString(4, trusted.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<UUID> getTrusted(String world, int chunkX, int chunkZ) {
        List<UUID> trusted = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT trusted FROM trusted WHERE world = ? AND chunkX = ? AND chunkZ = ?")
        ) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trusted.add(UUID.fromString(rs.getString("trusted")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trusted;
    }
}
