package dev.discordtominecraft.link;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public class DiscordLinkPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerGateListener playerGateListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        File databaseFile = new File(getDataFolder(), "linking.db");
        databaseManager = new DatabaseManager(databaseFile);

        try {
            databaseManager.init();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize link database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LinkCodeService linkCodeService = new LinkCodeService(databaseManager);
        playerGateListener = new PlayerGateListener(databaseManager, linkCodeService);
        getServer().getPluginManager().registerEvents(playerGateListener, this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, databaseManager::cleanupExpiredCodes, 20L * 60, 20L * 60);
        getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(playerGateListener::refreshStatus), 20L * 3, 20L * 5);

        getLogger().info("DiscordLinkGate enabled.");
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
