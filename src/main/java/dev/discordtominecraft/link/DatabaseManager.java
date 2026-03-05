package dev.discordtominecraft.link;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DatabaseManager {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path storagePath;

    public DatabaseManager(Path storagePath) {
        this.storagePath = storagePath;
    }

    public synchronized void init() throws IOException {
        if (storagePath.getParent() != null) {
            Files.createDirectories(storagePath.getParent());
        }
        if (!Files.exists(storagePath)) {
            writeStore(new DataStore());
        }
    }

    public synchronized boolean isLinked(UUID uuid) {
        DataStore store = readStore();
        return store.links.containsKey(uuid.toString());
    }

    public synchronized Optional<String> getPendingCode(UUID uuid) {
        DataStore store = readStore();
        String uuidString = uuid.toString();

        for (Map.Entry<String, PendingCodeRecord> entry : store.pending_codes.entrySet()) {
            PendingCodeRecord record = entry.getValue();
            if (!uuidString.equals(record.minecraft_uuid)) {
                continue;
            }
            if (record.expires_at < Instant.now().getEpochSecond()) {
                store.pending_codes.remove(entry.getKey());
                writeStore(store);
                return Optional.empty();
            }
            return Optional.of(entry.getKey());
        }

        return Optional.empty();
    }

    public synchronized void saveCode(String code, UUID uuid, long expiresAt) {
        DataStore store = readStore();
        String uuidString = uuid.toString();
        store.pending_codes.entrySet().removeIf(e -> uuidString.equals(e.getValue().minecraft_uuid));

        PendingCodeRecord record = new PendingCodeRecord();
        record.minecraft_uuid = uuidString;
        record.expires_at = expiresAt;
        store.pending_codes.put(code, record);
        writeStore(store);
    }

    public synchronized void deleteCode(String code) {
        DataStore store = readStore();
        store.pending_codes.remove(code);
        writeStore(store);
    }

    public synchronized void cleanupExpiredCodes() {
        DataStore store = readStore();
        long now = Instant.now().getEpochSecond();
        store.pending_codes.entrySet().removeIf(e -> e.getValue().expires_at < now);
        writeStore(store);
    }

    private DataStore readStore() {
        try {
            if (!Files.exists(storagePath)) {
                return new DataStore();
            }
            String json = Files.readString(storagePath, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new DataStore();
            }
            DataStore parsed = gson.fromJson(json, DataStore.class);
            return parsed == null ? new DataStore() : parsed.withDefaults();
        } catch (Exception e) {
            return new DataStore();
        }
    }

    private void writeStore(DataStore store) {
        try {
            Files.writeString(storagePath, gson.toJson(store.withDefaults()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static class DataStore {
        Map<String, LinkRecord> links = new HashMap<>();
        Map<String, PendingCodeRecord> pending_codes = new HashMap<>();

        DataStore withDefaults() {
            if (links == null) {
                links = new HashMap<>();
            }
            if (pending_codes == null) {
                pending_codes = new HashMap<>();
            }
            return this;
        }
    }

    private static class LinkRecord {
        String discord_id;
        long linked_at;
    }

    private static class PendingCodeRecord {
        String minecraft_uuid;
        long expires_at;
    }
}
