package com.servertimes.config;

import com.servertimes.ServerTimesPlugin;
import com.servertimes.model.TimeSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ConfigManager {
    private final ServerTimesPlugin plugin;
    private volatile FileConfiguration config;
    private File configFile;
    private final Map<DayOfWeek, List<TimeSession>> schedule;
    private final Object configLock = new Object();
    private final Object scheduleLock = new Object();
    
    // Configuration keys
    private static final String SCHEDULE_PATH = "schedule";
    private static final String MESSAGES_PATH = "messages";
    private static final String SETTINGS_PATH = "settings";
    
    public ConfigManager(ServerTimesPlugin plugin) {
        this.plugin = plugin;
        this.schedule = new EnumMap<>(DayOfWeek.class);
        
        // Initialize empty schedule for all days
        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day, new ArrayList<>());
        }
    }
    
    public void loadConfig() {
        synchronized (configLock) {
            try {
                // Create config file if it doesn't exist
                configFile = new File(plugin.getDataFolder(), "config.yml");
                if (!configFile.exists()) {
                    File dataFolder = plugin.getDataFolder();
                    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                        plugin.getLogger().severe("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
                        return;
                    }
                    plugin.saveResource("config.yml", false);
                }
                
                // Validate file exists and is readable
                if (!configFile.exists() || !configFile.canRead()) {
                    plugin.getLogger().severe("Config file does not exist or is not readable: " + configFile.getAbsolutePath());
                    return;
                }
                
                config = YamlConfiguration.loadConfiguration(configFile);
                loadScheduleFromConfig();
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading configuration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void loadScheduleFromConfig() {
        synchronized (scheduleLock) {
            try {
                if (config == null) {
                    plugin.getLogger().warning("Config is null, cannot load schedule");
                    return;
                }
                
                for (DayOfWeek day : DayOfWeek.values()) {
                    String dayName = day.name().toLowerCase();
                    List<String> sessions = config.getStringList(SCHEDULE_PATH + "." + dayName);
                    
                    if (sessions == null) {
                        sessions = new ArrayList<>();
                    }
                    
                    List<TimeSession> daySessions = new ArrayList<>();
                    for (String sessionStr : sessions) {
                        if (sessionStr == null || sessionStr.trim().isEmpty()) {
                            continue;
                        }
                        
                        try {
                            String[] times = sessionStr.split("-");
                            if (times.length == 2) {
                                String startTime = times[0].trim();
                                String endTime = times[1].trim();
                                
                                if (!startTime.isEmpty() && !endTime.isEmpty()) {
                                    TimeSession session = new TimeSession(startTime, endTime);
                                    daySessions.add(session);
                                }
                            } else {
                                plugin.getLogger().warning("Invalid session format in config for " + dayName + ": " + sessionStr + " (expected format: start-end)");
                            }
                        } catch (DateTimeParseException e) {
                            plugin.getLogger().warning("Invalid time format in config for " + dayName + ": " + sessionStr + " - " + e.getMessage());
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error parsing session for " + dayName + ": " + sessionStr + " - " + e.getMessage());
                        }
                    }
                    schedule.put(day, daySessions);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading schedule from config: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void saveConfig() {
        synchronized (configLock) {
            synchronized (scheduleLock) {
                try {
                    if (config == null) {
                        plugin.getLogger().warning("Config is null, cannot save");
                        return;
                    }
                    
                    if (configFile == null) {
                        plugin.getLogger().warning("Config file is null, cannot save");
                        return;
                    }
                    
                    // Save schedule to config
                    for (DayOfWeek day : DayOfWeek.values()) {
                        String dayName = day.name().toLowerCase();
                        List<String> sessionStrings = new ArrayList<>();
                        
                        List<TimeSession> daySessions = schedule.get(day);
                        if (daySessions != null) {
                            for (TimeSession session : daySessions) {
                                if (session != null) {
                                    try {
                                        String configString = session.toConfigString();
                                        if (configString != null && !configString.trim().isEmpty()) {
                                            sessionStrings.add(configString);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Error converting session to config string for " + dayName + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        config.set(SCHEDULE_PATH + "." + dayName, sessionStrings);
                    }
                    
                    // Validate file is writable before saving
                    if (!configFile.canWrite() && configFile.exists()) {
                        plugin.getLogger().severe("Config file is not writable: " + configFile.getAbsolutePath());
                        return;
                    }
                    
                    config.save(configFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save config file: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    plugin.getLogger().severe("Unexpected error saving config: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public List<TimeSession> getSessionsForDay(DayOfWeek day) {
        synchronized (scheduleLock) {
            if (day == null) {
                return new ArrayList<>();
            }
            List<TimeSession> sessions = schedule.get(day);
            return sessions != null ? new ArrayList<>(sessions) : new ArrayList<>();
        }
    }
    
    public void setSessionsForDay(DayOfWeek day, List<TimeSession> sessions) {
        if (day == null) {
            plugin.getLogger().warning("Cannot set sessions for null day");
            return;
        }
        
        synchronized (scheduleLock) {
            List<TimeSession> sessionsCopy = sessions != null ? new ArrayList<>(sessions) : new ArrayList<>();
            schedule.put(day, sessionsCopy);
        }
        saveConfig();
    }
    
    public void addSessionForDay(DayOfWeek day, TimeSession session) {
        if (day == null || session == null) {
            plugin.getLogger().warning("Cannot add null session or session for null day");
            return;
        }
        
        synchronized (scheduleLock) {
            List<TimeSession> sessions = schedule.get(day);
            if (sessions != null) {
                sessions.add(session);
            }
        }
        saveConfig();
    }
    
    public void removeSessionForDay(DayOfWeek day, int sessionIndex) {
        if (day == null) {
            plugin.getLogger().warning("Cannot remove session for null day");
            return;
        }
        
        synchronized (scheduleLock) {
            List<TimeSession> sessions = schedule.get(day);
            if (sessions != null && sessionIndex >= 0 && sessionIndex < sessions.size()) {
                sessions.remove(sessionIndex);
            }
        }
        saveConfig();
    }
    
    public void clearSessionsForDay(DayOfWeek day) {
        if (day == null) {
            plugin.getLogger().warning("Cannot clear sessions for null day");
            return;
        }
        
        synchronized (scheduleLock) {
            List<TimeSession> sessions = schedule.get(day);
            if (sessions != null) {
                sessions.clear();
            }
        }
        saveConfig();
    }
    
    public boolean isServerOpen(DayOfWeek day) {
        List<TimeSession> sessions = schedule.get(day);
        return sessions.stream().anyMatch(TimeSession::isCurrentlyActive);
    }
    
    public boolean isServerOpen() {
        try {
            DayOfWeek currentDay = java.time.LocalDateTime.now().getDayOfWeek();
            java.time.LocalTime currentTime = java.time.LocalTime.now();
            
            synchronized (scheduleLock) {
                List<TimeSession> sessions = schedule.get(currentDay);
                if (sessions != null) {
                    for (TimeSession session : sessions) {
                        if (session != null && session.isCurrentlyActive()) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if server is open: " + e.getMessage());
            return false; // Default to closed on error
        }
    }
    
    public String getKickMessage() {
        return config.getString(MESSAGES_PATH + ".server_closed", 
            "§cThe server is currently closed. Please check the schedule and come back during open hours!");
    }
    
    public String getJoinDeniedMessage() {
        return config.getString(MESSAGES_PATH + ".join_denied", 
            "§cThe server is currently closed. Please check the schedule and come back during open hours!");
    }
    
    public String getServerOpeningMessage() {
        return config.getString(MESSAGES_PATH + ".server_opening", 
            "§aThe server is now open! Welcome!");
    }
    
    public String getServerClosingWarning(int minutes) {
        return config.getString(MESSAGES_PATH + ".server_closing_warning", 
            "§eThe server will close in " + minutes + " minute(s)!")
            .replace("{minutes}", String.valueOf(minutes));
    }
    
    public int getWarningTime() {
        return config.getInt(SETTINGS_PATH + ".warning_minutes", 5);
    }
    
    public boolean isGracefulShutdown() {
        return config.getBoolean(SETTINGS_PATH + ".graceful_shutdown", true);
    }
    
    public String getServerClosedMessage() {
        synchronized (configLock) {
            if (config == null) {
                return "&cThe server is currently closed.";
            }
            return config.getString("messages.server-closed", "&cThe server is currently closed.");
        }
    }
    
    public String getServerOpenMessage() {
        synchronized (configLock) {
            if (config == null) {
                return "&aThe server is currently open.";
            }
            return config.getString("messages.server-open", "&aThe server is currently open.");
        }
    }
    
    public String getTimezoneSetMessage() {
        synchronized (configLock) {
            if (config == null) {
                return "&aYour timezone has been set to {timezone}.";
            }
            return config.getString("messages.timezone-set", "&aYour timezone has been set to {timezone}.");
        }
    }
    
    public String getTimezoneResetMessage() {
        synchronized (configLock) {
            if (config == null) {
                return "&aYour timezone has been reset.";
            }
            return config.getString("messages.timezone-reset", "&aYour timezone has been reset.");
        }
    }
    
    public String getInvalidTimezoneMessage() {
        synchronized (configLock) {
            if (config == null) {
                return "&cInvalid timezone. Use /timezone list to see available timezones.";
            }
            return config.getString("messages.invalid-timezone", "&cInvalid timezone. Use /timezone list to see available timezones.");
        }
    }
    
    public Map<DayOfWeek, List<TimeSession>> getFullSchedule() {
        synchronized (scheduleLock) {
            Map<DayOfWeek, List<TimeSession>> scheduleCopy = new HashMap<>();
            for (Map.Entry<DayOfWeek, List<TimeSession>> entry : schedule.entrySet()) {
                List<TimeSession> sessions = entry.getValue();
                scheduleCopy.put(entry.getKey(), sessions != null ? new ArrayList<>(sessions) : new ArrayList<>());
            }
            return scheduleCopy;
        }
    }
}