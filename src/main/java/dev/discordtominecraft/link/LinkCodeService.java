package dev.discordtominecraft.link;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public class LinkCodeService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DatabaseManager databaseManager;
    private final int codeLength;
    private final long codeExpirySeconds;

    public LinkCodeService(DatabaseManager databaseManager, int codeLength, long codeExpirySeconds) {
        this.databaseManager = databaseManager;
        this.codeLength = Math.max(4, codeLength);
        this.codeExpirySeconds = Math.max(120, codeExpirySeconds);
    }

    public String generateAndStore(UUID uuid) throws SQLException {
        String code = generateCode();
        long expiresAt = Instant.now().getEpochSecond() + codeExpirySeconds;
        databaseManager.saveCode(code, uuid, expiresAt);
        return code;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
