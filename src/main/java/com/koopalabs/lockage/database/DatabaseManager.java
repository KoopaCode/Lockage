package com.koopalabs.lockage.database;

import com.koopalabs.lockage.Lockage;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final Lockage plugin;
    private Connection connection;

    public DatabaseManager(Lockage plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "database.db");
        boolean needsInit = !dataFolder.exists();
        
        try {
            if (needsInit) {
                dataFolder.getParentFile().mkdirs();
                dataFolder.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            
            // Create or verify tables
            verifyDatabase();
            
            if (needsInit) {
                plugin.getLogger().info("Database created successfully!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            plugin.getLogger().severe("Attempting to repair database...");
            repairDatabase();
        }
    }

    private void verifyDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // Check if tables exist and have correct structure
            DatabaseMetaData meta = connection.getMetaData();
            boolean hasLockedChests = false;
            boolean hasKeys = false;
            
            ResultSet tables = meta.getTables(null, null, "locked_chests", null);
            hasLockedChests = tables.next();
            tables.close();
            
            tables = meta.getTables(null, null, "keys", null);
            hasKeys = tables.next();
            tables.close();

            if (!hasLockedChests || !hasKeys) {
                createTables();
            }

            // Verify data integrity
            cleanupInvalidEntries();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database verification failed: " + e.getMessage());
            repairDatabase();
        }
    }

    private void cleanupInvalidEntries() {
        try {
            // Remove entries with invalid worlds
            String sql = "DELETE FROM locked_chests WHERE world NOT IN (SELECT name FROM worlds)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int removed = pstmt.executeUpdate();
                if (removed > 0) {
                    plugin.getLogger().warning("Removed " + removed + " chest locks from invalid worlds");
                }
            }

            // Remove orphaned keys
            sql = "DELETE FROM keys WHERE key_id NOT IN (SELECT key_id FROM locked_chests)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int removed = pstmt.executeUpdate();
                if (removed > 0) {
                    plugin.getLogger().warning("Removed " + removed + " orphaned keys");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clean up invalid entries: " + e.getMessage());
        }
    }

    private void repairDatabase() {
        try {
            // Close existing connection
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            // Backup existing database if it exists
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            if (dbFile.exists()) {
                File backupFile = new File(plugin.getDataFolder(), "database.db.backup");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                dbFile.renameTo(backupFile);
                plugin.getLogger().warning("Created database backup: database.db.backup");
            }

            // Create new database
            dbFile.createNewFile();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            createTables();
            plugin.getLogger().info("Database repaired successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to repair database: " + e.getMessage());
            plugin.getLogger().severe("Please contact an administrator!");
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Table for locked chests
            stmt.execute("CREATE TABLE IF NOT EXISTS locked_chests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "owner UUID NOT NULL," +
                    "key_id TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Table for keys
            stmt.execute("CREATE TABLE IF NOT EXISTS keys (" +
                    "key_id TEXT PRIMARY KEY," +
                    "owner UUID NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Create indices for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chest_location ON locked_chests(world, x, y, z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chest_key ON locked_chests(key_id)");
            
            // Create worlds table to track valid worlds
            stmt.execute("CREATE TABLE IF NOT EXISTS worlds (" +
                    "name TEXT PRIMARY KEY," +
                    "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }

    public void addLockedChest(Location location, UUID owner, String keyId) {
        String sql = "INSERT INTO locked_chests (world, x, y, z, owner, key_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.setString(5, owner.toString());
            pstmt.setString(6, keyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add locked chest: " + e.getMessage());
        }
    }

    public boolean isChestLocked(Location location) {
        String sql = "SELECT * FROM locked_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check locked chest: " + e.getMessage());
            return false;
        }
    }

    public boolean isKeyValidForChest(Location location, String keyId) {
        String sql = "SELECT * FROM locked_chests WHERE world = ? AND x = ? AND y = ? AND z = ? AND key_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.setString(5, keyId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check key validity: " + e.getMessage());
            return false;
        }
    }

    public void removeLock(Location location) {
        String sql = "DELETE FROM locked_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, location.getWorld().getName());
            pstmt.setInt(2, location.getBlockX());
            pstmt.setInt(3, location.getBlockY());
            pstmt.setInt(4, location.getBlockZ());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove lock: " + e.getMessage());
        }
    }
} 