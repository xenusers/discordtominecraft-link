package dev.discordtominecraft.link;

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
    private final String username;
    private final String password;

    public DatabaseManager(String host, int port, String databaseName, String username, String password, boolean sslEnabled) {
        String sslMode = sslEnabled ? "REQUIRED" : "DISABLED";
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?sslMode=" + sslMode + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.username = username;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public void init() throws SQLException {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS links (
                    minecraft_uuid VARCHAR(36) PRIMARY KEY,
                    discord_id VARCHAR(32) NOT NULL,
                    linked_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending_codes (
                    code VARCHAR(16) PRIMARY KEY,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    expires_at BIGINT NOT NULL,
                    INDEX idx_pending_uuid (minecraft_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    public boolean isLinked(UUID uuid) {
        String sql = "SELECT 1 FROM links WHERE minecraft_uuid = ? LIMIT 1";
        try (Connection connection = getConnection();
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
        try (Connection connection = getConnection();
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
        String insert = "INSERT INTO pending_codes(code, minecraft_uuid, expires_at) VALUES (?, ?, ?)";

        try (Connection connection = getConnection()) {
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
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    public void cleanupExpiredCodes() {
        String sql = "DELETE FROM pending_codes WHERE expires_at < ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
