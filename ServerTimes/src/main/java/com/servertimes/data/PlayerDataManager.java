package com.servertimes.data;

import com.servertimes.ServerTimesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ServerTimesPlugin plugin;
    private volatile FileConfiguration playerData;
    private volatile File playerDataFile;
    private final Map<UUID, String> playerTimezones;
    
    // Synchronization objects
    private final Object dataLock = new Object();
    private final Object fileLock = new Object();
    
    private static final String TIMEZONE_PATH = "timezones";
    
    public PlayerDataManager(ServerTimesPlugin plugin) {
        this.plugin = plugin;
        this.playerTimezones = new ConcurrentHashMap<>();
    }
    
    public void loadPlayerData() {
        synchronized (fileLock) {
            try {
                // Validate plugin data folder
                File dataFolder = plugin.getDataFolder();
                if (dataFolder == null) {
                    plugin.getLogger().severe("Plugin data folder is null");
                    return;
                }
                
                // Create player data file if it doesn't exist
                playerDataFile = new File(dataFolder, "playerdata.yml");
                if (!playerDataFile.exists()) {
                    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                        plugin.getLogger().severe("Could not create plugin data directory: " + dataFolder.getAbsolutePath());
                        return;
                    }
                    
                    try {
                        if (!playerDataFile.createNewFile()) {
                            plugin.getLogger().severe("Could not create player data file: " + playerDataFile.getAbsolutePath());
                            return;
                        }
                    } catch (IOException e) {
                        plugin.getLogger().severe("Could not create player data file: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
                
                // Validate file is readable
                if (!playerDataFile.canRead()) {
                    plugin.getLogger().severe("Player data file is not readable: " + playerDataFile.getAbsolutePath());
                    return;
                }
                
                synchronized (dataLock) {
                    playerData = YamlConfiguration.loadConfiguration(playerDataFile);
                    if (playerData == null) {
                        plugin.getLogger().severe("Failed to load player data configuration");
                        return;
                    }
                }
                
                loadTimezonesFromConfig();
            } catch (Exception e) {
                plugin.getLogger().severe("Unexpected error loading player data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void loadTimezonesFromConfig() {
        synchronized (dataLock) {
            try {
                if (playerData == null) {
                    plugin.getLogger().warning("Player data is null, cannot load timezones");
                    return;
                }
                
                if (playerData.contains(TIMEZONE_PATH)) {
                    var timezoneSection = playerData.getConfigurationSection(TIMEZONE_PATH);
                    if (timezoneSection != null) {
                        for (String uuidStr : timezoneSection.getKeys(false)) {
                            if (uuidStr == null || uuidStr.trim().isEmpty()) {
                                plugin.getLogger().warning("Empty UUID string in player data");
                                continue;
                            }
                            
                            try {
                                UUID uuid = UUID.fromString(uuidStr.trim());
                                String timezone = playerData.getString(TIMEZONE_PATH + "." + uuidStr);
                                if (timezone != null && !timezone.trim().isEmpty()) {
                                    playerTimezones.put(uuid, timezone.trim());
                                } else {
                                    plugin.getLogger().warning("Empty timezone for UUID: " + uuidStr);
                                }
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in player data: " + uuidStr + " - " + e.getMessage());
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error processing timezone for UUID " + uuidStr + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading timezones from config: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void savePlayerData() {
        synchronized (fileLock) {
            synchronized (dataLock) {
                try {
                    if (playerData == null) {
                        plugin.getLogger().warning("Player data is null, cannot save");
                        return;
                    }
                    
                    if (playerDataFile == null) {
                        plugin.getLogger().warning("Player data file is null, cannot save");
                        return;
                    }
                    
                    // Validate file is writable before saving
                    if (!playerDataFile.canWrite() && playerDataFile.exists()) {
                        plugin.getLogger().severe("Player data file is not writable: " + playerDataFile.getAbsolutePath());
                        return;
                    }
                    
                    // Save timezones to config
                    for (Map.Entry<UUID, String> entry : playerTimezones.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            try {
                                playerData.set(TIMEZONE_PATH + "." + entry.getKey().toString(), entry.getValue());
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error saving timezone for UUID " + entry.getKey() + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    playerData.save(playerDataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save player data file: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    plugin.getLogger().severe("Unexpected error saving player data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Set a player's timezone preference
     */
    public void setPlayerTimezone(UUID playerId, String timezone) {
        if (playerId == null) {
            plugin.getLogger().warning("Cannot set timezone for null player ID");
            return;
        }
        
        if (timezone == null || timezone.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot set null or empty timezone for player " + playerId);
            return;
        }
        
        try {
            String trimmedTimezone = timezone.trim();
            // Validate timezone length to prevent excessive memory usage
            if (trimmedTimezone.length() > 100) {
                plugin.getLogger().warning("Timezone too long for player " + playerId + ": " + trimmedTimezone.length() + " characters");
                return;
            }
            
            playerTimezones.put(playerId, trimmedTimezone);
            savePlayerData();
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting timezone for player " + playerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get a player's timezone preference
     */
    public String getPlayerTimezone(UUID playerId) {
        if (playerId == null) {
            plugin.getLogger().warning("Cannot get timezone for null player ID");
            return null;
        }
        
        try {
            return playerTimezones.get(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting timezone for player " + playerId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if player has a timezone set
     */
    public boolean hasTimezone(UUID playerId) {
        if (playerId == null) {
            plugin.getLogger().warning("Cannot check timezone for null player ID");
            return false;
        }
        
        try {
            return playerTimezones.containsKey(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking timezone for player " + playerId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove a player's timezone preference
     */
    public void removePlayerTimezone(UUID playerId) {
        if (playerId == null) {
            plugin.getLogger().warning("Cannot remove timezone for null player ID");
            return;
        }
        
        try {
            playerTimezones.remove(playerId);
            
            synchronized (dataLock) {
                if (playerData != null) {
                    playerData.set(TIMEZONE_PATH + "." + playerId.toString(), null);
                }
            }
            
            savePlayerData();
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing timezone for player " + playerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get all player timezones
     */
    public Map<UUID, String> getAllPlayerTimezones() {
        try {
            return new HashMap<>(playerTimezones);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting all player timezones: " + e.getMessage());
            return new HashMap<>();
        }
    }
}