package com.servertimes;

import com.servertimes.commands.ServerTimesCommand;
import com.servertimes.commands.TimezoneCommand;
import com.servertimes.config.ConfigManager;
import com.servertimes.data.PlayerDataManager;
import com.servertimes.listeners.PlayerJoinListener;
import com.servertimes.scheduler.ServerScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerTimesPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private ServerScheduler serverScheduler;
    private PlayerDataManager playerDataManager;
    
    @Override
    public void onEnable() {
        try {
            // Initialize configuration
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            // Initialize player data manager
            playerDataManager = new PlayerDataManager(this);
            playerDataManager.loadPlayerData();
            
            // Initialize scheduler
            serverScheduler = new ServerScheduler(this, configManager);
            
            // Register commands
            if (getCommand("servertimes") != null) {
                getCommand("servertimes").setExecutor(new ServerTimesCommand(this, configManager, serverScheduler, playerDataManager));
            } else {
                getLogger().severe("Failed to register 'servertimes' command - check plugin.yml!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            if (getCommand("timezone") != null) {
                getCommand("timezone").setExecutor(new TimezoneCommand(this, playerDataManager));
            } else {
                getLogger().severe("Failed to register 'timezone' command - check plugin.yml!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(serverScheduler, this), this);
            
            // Start the scheduler
            serverScheduler.start();
            
            getLogger().info("ServerTimes plugin has been enabled!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable ServerTimes plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Stop scheduler first to prevent new tasks
            if (serverScheduler != null) {
                serverScheduler.stop();
                serverScheduler = null;
            }
            
            // Save player data
            if (playerDataManager != null) {
                playerDataManager.savePlayerData();
                playerDataManager = null;
            }
            
            // Save configuration if needed
            if (configManager != null) {
                configManager.saveConfig();
                configManager = null;
            }
            
            getLogger().info("ServerTimes plugin has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during plugin disable: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ServerScheduler getServerScheduler() {
        return serverScheduler;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}