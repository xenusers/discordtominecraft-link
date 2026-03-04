package dev.discordtominecraft.link;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlayerGateListener implements Listener {
    private final DatabaseManager databaseManager;
    private final LinkCodeService linkCodeService;
    private final Set<UUID> blockedPlayers = new HashSet<>();
    private final long expirySeconds;

    public PlayerGateListener(DatabaseManager databaseManager, LinkCodeService linkCodeService, long expirySeconds) {
        this.databaseManager = databaseManager;
        this.linkCodeService = linkCodeService;
        this.expirySeconds = Math.max(120, expirySeconds);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (databaseManager.isLinked(uuid)) {
            blockedPlayers.remove(uuid);
            player.sendMessage(Component.text("§aDiscord already linked. You are free to move."));
            return;
        }

        blockedPlayers.add(uuid);
        String code = getOrCreateCode(player);
        sendLinkMessage(player, code);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!blockedPlayers.contains(player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            event.setTo(from);
            player.sendActionBar(Component.text("§cLink your account first. Use /link <code> on Discord."));
        }
    }

    public void refreshStatus(Player player) {
        UUID uuid = player.getUniqueId();
        if (databaseManager.isLinked(uuid)) {
            blockedPlayers.remove(uuid);
            return;
        }

        blockedPlayers.add(uuid);
        String code = getOrCreateCode(player);
        player.sendActionBar(Component.text("§eUse /link " + code + " in Discord to unlock movement."));
    }

    public void regenerateCode(Player player) {
        if (databaseManager.isLinked(player.getUniqueId())) {
            player.sendMessage(Component.text("§aYou are already linked."));
            return;
        }

        try {
            String code = linkCodeService.generateAndStore(player.getUniqueId());
            sendLinkMessage(player, code);
        } catch (SQLException e) {
            player.sendMessage(Component.text("§cCould not generate a new code right now."));
        }
    }

    private String getOrCreateCode(Player player) {
        Optional<String> existing = databaseManager.getPendingCode(player.getUniqueId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return linkCodeService.generateAndStore(player.getUniqueId());
        } catch (SQLException e) {
            player.sendMessage(Component.text("§cCould not create your Discord link code."));
            return "ERROR";
        }
    }

    private void sendLinkMessage(Player player, String code) {
        long minutes = Math.max(1, expirySeconds / 60);
        player.sendMessage(Component.text("§cYour account is not linked. You cannot move yet."));
        player.sendMessage(Component.text("§eDiscord command: /link " + code));
        player.sendMessage(Component.text("§7Code expires in " + minutes + " minutes. Use /linkcode in-game to regenerate."));
    }
}
