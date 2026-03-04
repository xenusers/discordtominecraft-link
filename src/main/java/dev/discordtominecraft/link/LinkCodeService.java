package dev.discordtominecraft.link;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public class LinkCodeService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_SECONDS = 10 * 60;

    private final DatabaseManager databaseManager;

    public LinkCodeService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public String generateAndStore(UUID uuid) throws SQLException {
        String code = generateCode();
        long expiresAt = Instant.now().getEpochSecond() + CODE_EXPIRY_SECONDS;
        databaseManager.saveCode(code, uuid, expiresAt);
        return code;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
