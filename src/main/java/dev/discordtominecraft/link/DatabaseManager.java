package dev.discordtominecraft.link;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class DatabaseManager {
    private final String jdbcUrl;

    public DatabaseManager(File databaseFile) {
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    public void init() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS links (
                    minecraft_uuid TEXT PRIMARY KEY,
                    discord_id TEXT NOT NULL,
                    linked_at INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending_codes (
                    code TEXT PRIMARY KEY,
                    minecraft_uuid TEXT NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pending_uuid ON pending_codes(minecraft_uuid)");
        }
    }

    public boolean isLinked(UUID uuid) {
        String sql = "SELECT 1 FROM links WHERE minecraft_uuid = ? LIMIT 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public Optional<String> getPendingCode(UUID uuid) {
        String sql = "SELECT code, expires_at FROM pending_codes WHERE minecraft_uuid = ? LIMIT 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                long expiresAt = rs.getLong("expires_at");
                if (expiresAt < Instant.now().getEpochSecond()) {
                    deleteCode(rs.getString("code"));
                    return Optional.empty();
                }
                return Optional.of(rs.getString("code"));
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public void saveCode(String code, UUID uuid, long expiresAt) throws SQLException {
        String deleteOld = "DELETE FROM pending_codes WHERE minecraft_uuid = ?";
        String insert = "INSERT OR REPLACE INTO pending_codes(code, minecraft_uuid, expires_at) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            connection.setAutoCommit(false);
            try (PreparedStatement deletePs = connection.prepareStatement(deleteOld);
                 PreparedStatement insertPs = connection.prepareStatement(insert)) {
                deletePs.setString(1, uuid.toString());
                deletePs.executeUpdate();

                insertPs.setString(1, code);
                insertPs.setString(2, uuid.toString());
                insertPs.setLong(3, expiresAt);
                insertPs.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteCode(String code) throws SQLException {
        String sql = "DELETE FROM pending_codes WHERE code = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    public void cleanupExpiredCodes() {
        String sql = "DELETE FROM pending_codes WHERE expires_at < ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
