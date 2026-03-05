package dev.discordtominecraft.link;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class DiscordLinkPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerGateListener playerGateListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        String host = config.getString("database.host", "127.0.0.1");
        int port = config.getInt("database.port", 3306);
        String name = config.getString("database.name", "minecraft");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");
        boolean ssl = config.getBoolean("database.ssl", true);

        int codeLength = config.getInt("code.length", 6);
        long codeExpirySeconds = config.getLong("code.expiry-seconds", 600L);

        if (password.isBlank() || "CHANGE_ME".equals(password)) {
            getLogger().severe("Set database.password in plugins/DiscordLinkGate/config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(host, port, name, username, password, ssl);

        try {
            databaseManager.init();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize MySQL link database: " + e.getMessage() + " (try database.ssl: true)");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LinkCodeService linkCodeService = new LinkCodeService(databaseManager, codeLength, codeExpirySeconds);
        playerGateListener = new PlayerGateListener(databaseManager, linkCodeService, codeExpirySeconds);
        getServer().getPluginManager().registerEvents(playerGateListener, this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, databaseManager::cleanupExpiredCodes, 20L * 60, 20L * 60);
        getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(playerGateListener::refreshStatus), 20L * 3, 20L * 5);

        getLogger().info("DiscordLinkGate enabled with MySQL backend.");
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
