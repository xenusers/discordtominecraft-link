package dev.discordtominecraft.link;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;

public class DiscordLinkPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerGateListener playerGateListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String configuredStorage = config.getString("storage.file", "logins.json");
        Path storagePath = Path.of(configuredStorage);
        if (!storagePath.isAbsolute()) {
            storagePath = getDataFolder().toPath().resolve(configuredStorage);
        }

        int codeLength = config.getInt("code.length", 6);
        long codeExpirySeconds = config.getLong("code.expiry-seconds", 600L);

        databaseManager = new DatabaseManager(storagePath);

        try {
            databaseManager.init();
        } catch (IOException e) {
            getLogger().severe("Failed to initialize login storage: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LinkCodeService linkCodeService = new LinkCodeService(databaseManager, codeLength, codeExpirySeconds);
        playerGateListener = new PlayerGateListener(databaseManager, linkCodeService, codeExpirySeconds);
        getServer().getPluginManager().registerEvents(playerGateListener, this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, databaseManager::cleanupExpiredCodes, 20L * 60, 20L * 60);
        getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(playerGateListener::refreshStatus), 20L * 3, 20L * 5);

        getLogger().info("DiscordLinkGate enabled with JSON storage at: " + storagePath);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("linkcode")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        playerGateListener.regenerateCode(player);
        return true;
    }
}
