package com.servertimes.scheduler;

import com.servertimes.ServerTimesPlugin;
import com.servertimes.config.ConfigManager;
import com.servertimes.model.TimeSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerScheduler {
    private final ServerTimesPlugin plugin;
    private final ConfigManager configManager;
    private volatile BukkitTask mainTask;
    private volatile BukkitTask warningTask;
    
    // Thread-safe state variables
    private final AtomicBoolean serverCurrentlyOpen = new AtomicBoolean(true);
    private final AtomicBoolean hasWarnedPlayers = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Synchronization object
    private final Object schedulerLock = new Object();
    
    public ServerScheduler(ServerTimesPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    public void start() {
        synchronized (schedulerLock) {
            if (isRunning.get()) {
                plugin.getLogger().warning("ServerScheduler is already running");
                return;
            }
            
            try {
                // Check server state every 30 seconds
                mainTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            checkServerState();
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error in main scheduler task: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 600L); // 0 delay, 600 ticks = 30 seconds
                
                if (mainTask == null) {
                    plugin.getLogger().severe("Failed to start main scheduler task");
                    return;
                }
                
                // Check for warnings every minute
                warningTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            checkForWarnings();
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error in warning scheduler task: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1200L); // 0 delay, 1200 ticks = 60 seconds
                
                if (warningTask == null) {
                    plugin.getLogger().severe("Failed to start warning scheduler task");
                    if (mainTask != null) {
                        mainTask.cancel();
                        mainTask = null;
                    }
                    return;
                }
                
                isRunning.set(true);
                plugin.getLogger().info("ServerScheduler started");
            } catch (Exception e) {
                plugin.getLogger().severe("Error starting ServerScheduler: " + e.getMessage());
                e.printStackTrace();
                stop(); // Clean up any partially started tasks
            }
        }
    }
    
    public void stop() {
        synchronized (schedulerLock) {
            try {
                isRunning.set(false);
                
                if (mainTask != null) {
                    try {
                        mainTask.cancel();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error cancelling main task: " + e.getMessage());
                    } finally {
                        mainTask = null;
                    }
                }
                
                if (warningTask != null) {
                    try {
                        warningTask.cancel();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error cancelling warning task: " + e.getMessage());
                    } finally {
                        warningTask = null;
                    }
                }
                
                plugin.getLogger().info("ServerScheduler stopped");
            } catch (Exception e) {
                plugin.getLogger().severe("Error stopping ServerScheduler: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void updateSchedule() {
        try {
            // Reset warning state when schedule is updated
            hasWarnedPlayers.set(false);
            
            // Immediately check the new state
            checkServerState();
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating schedule: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkServerState() {
        try {
            if (configManager == null) {
                plugin.getLogger().warning("ConfigManager is null, cannot check server state");
                return;
            }
            
            boolean shouldBeOpen = configManager.isServerOpen();
            boolean currentlyOpen = serverCurrentlyOpen.get();
            
            if (shouldBeOpen && !currentlyOpen) {
                // Server should open
                openServer();
            } else if (!shouldBeOpen && currentlyOpen) {
                // Server should close
                closeServer();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking server state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void checkForWarnings() {
        try {
            if (hasWarnedPlayers.get() || !serverCurrentlyOpen.get()) {
                return;
            }
            
            LocalTime now = LocalTime.now();
            LocalTime nextCloseTime = getNextCloseTime();
            
            if (nextCloseTime != null) {
                long minutesUntilClose = Duration.between(now, nextCloseTime).toMinutes();
                
                if (minutesUntilClose <= 5 && minutesUntilClose > 0) {
                    String warningMessage = "§eServer will close in " + minutesUntilClose + " minute(s)!";
                    Bukkit.broadcastMessage(warningMessage);
                    hasWarnedPlayers.set(true);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking for warnings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openServer() {
        try {
            serverCurrentlyOpen.set(true);
            hasWarnedPlayers.set(false);
            
            // Kick all players with the server open message
            String openMessage = configManager.getServerOpenMessage();
            if (openMessage == null || openMessage.trim().isEmpty()) {
                openMessage = "Server is now open!";
            }
            
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers != null) {
                for (Player player : onlinePlayers) {
                    if (player != null && player.isOnline()) {
                        try {
                            player.kickPlayer(openMessage);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error kicking player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Server opened - all players kicked");
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void closeServer() {
        try {
            serverCurrentlyOpen.set(false);
            hasWarnedPlayers.set(false);
            
            if (configManager.isGracefulShutdown()) {
                // Give a final warning before kicking
                String finalWarning = "§cThe server is now closing!";
                Bukkit.broadcastMessage(finalWarning);
                
                // Wait 5 seconds then kick all players
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        kickAllPlayers();
                    }
                }.runTaskLater(plugin, 100L); // 100 ticks = 5 seconds
            } else {
                // Immediately kick all players
                kickAllPlayers();
            }
            
            plugin.getLogger().info("Server closed according to schedule");
        } catch (Exception e) {
            plugin.getLogger().severe("Error closing server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void warnPlayers(int minutes) {
        try {
            String warningMessage = configManager.getServerClosingWarning(minutes);
            if (warningMessage == null || warningMessage.trim().isEmpty()) {
                warningMessage = "§eServer will close in " + minutes + " minute(s)!";
            }
            Bukkit.broadcastMessage(warningMessage);
            
            plugin.getLogger().info("Warned players: server closing in " + minutes + " minute(s)");
        } catch (Exception e) {
            plugin.getLogger().severe("Error warning players: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void kickAllPlayers() {
        try {
            String kickMessage = configManager.getKickMessage();
            if (kickMessage == null || kickMessage.trim().isEmpty()) {
                kickMessage = "Server is closed!";
            }
            
            // Create a copy of the player list to avoid concurrent modification
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers != null) {
                Player[] players = onlinePlayers.toArray(new Player[0]);
                
                for (Player player : players) {
                    if (player != null && player.isOnline()) {
                        try {
                            // Kick all players when server closes (no bypass permissions)
                            player.kickPlayer(kickMessage);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error kicking player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }
                
                plugin.getLogger().info("Kicked " + players.length + " players (server closed)");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error kicking all players: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isServerOpen() {
        return serverCurrentlyOpen.get();
    }
    
    public boolean shouldAllowJoin() {
        try {
            // Allow join if server is open OR if it should be open according to schedule
            return serverCurrentlyOpen.get() || (configManager != null && configManager.isServerOpen());
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if join should be allowed: " + e.getMessage());
            return false; // Default to not allowing join if there's an error
        }
    }
    
    // Method to manually override server state (for testing or emergency)
    public void setServerOpen(boolean open) {
        try {
            boolean currentlyOpen = serverCurrentlyOpen.get();
            if (open && !currentlyOpen) {
                openServer();
            } else if (!open && currentlyOpen) {
                closeServer();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting server open state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Get next opening/closing time for display purposes
    public String getNextStateChange() {
        try {
            if (configManager == null) {
                return "Configuration not available";
            }
            
            DayOfWeek today = DayOfWeek.from(LocalDate.now());
            LocalTime now = LocalTime.now();
            List<TimeSession> sessions = configManager.getSessionsForDay(today);
            
            if (sessions == null || sessions.isEmpty()) {
                return "No scheduled changes";
            }
            
            boolean currentlyOpen = serverCurrentlyOpen.get();
            
            if (currentlyOpen) {
                // Find when server will close
                for (TimeSession session : sessions) {
                    if (session != null && session.isCurrentlyActive()) {
                        LocalTime endTime = session.getEndTime();
                        if (endTime != null) {
                            return "Closes at " + endTime.toString();
                        }
                    }
                }
            } else {
                // Find when server will open
                for (TimeSession session : sessions) {
                    if (session != null) {
                        LocalTime startTime = session.getStartTime();
                        if (startTime != null && now.isBefore(startTime)) {
                            return "Opens at " + startTime.toString();
                        }
                    }
                }
                
                // Check tomorrow if no more sessions today
                try {
                    DayOfWeek tomorrow = today.plus(1);
                    List<TimeSession> tomorrowSessions = configManager.getSessionsForDay(tomorrow);
                    if (tomorrowSessions != null && !tomorrowSessions.isEmpty()) {
                        TimeSession firstSession = tomorrowSessions.get(0);
                        if (firstSession != null && firstSession.getStartTime() != null) {
                            return "Opens tomorrow at " + firstSession.getStartTime().toString();
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking tomorrow's sessions: " + e.getMessage());
                }
            }
            
            return "No scheduled changes";
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting next state change: " + e.getMessage());
            e.printStackTrace();
            return "Error retrieving schedule";
        }
    }
    
    private LocalTime getNextCloseTime() {
        try {
            if (configManager == null) {
                return null;
            }
            
            DayOfWeek today = DayOfWeek.from(LocalDate.now());
            LocalTime now = LocalTime.now();
            List<TimeSession> sessions = configManager.getSessionsForDay(today);
            
            if (sessions == null || sessions.isEmpty()) {
                return null;
            }
            
            for (TimeSession session : sessions) {
                if (session != null && session.isCurrentlyActive()) {
                    return session.getEndTime();
                }
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting next close time: " + e.getMessage());
            return null;
        }
    }
}