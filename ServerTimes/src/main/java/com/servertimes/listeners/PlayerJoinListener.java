package com.servertimes.listeners;

import com.servertimes.scheduler.ServerScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;

public class PlayerJoinListener implements Listener {
    private final ServerScheduler serverScheduler;
    private final Plugin plugin;
    
    public PlayerJoinListener(ServerScheduler serverScheduler, Plugin plugin) {
        if (serverScheduler == null) {
            throw new IllegalArgumentException("ServerScheduler cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.serverScheduler = serverScheduler;
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        try {
            // Null checks for event safety
            if (event == null) {
                return;
            }
            
            // Check if server should allow joins (no bypass permissions)
            // Use thread-safe method call
            if (!serverScheduler.shouldAllowJoin()) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, 
                    "§cThe server is currently closed.\n§cPlease check the schedule and come back during open hours!");
            }
        } catch (Exception e) {
            // Log error but don't prevent login if there's an unexpected error
            // This prevents the plugin from breaking server functionality
            try {
                plugin.getLogger().severe("Error in PlayerLoginEvent handler: " + e.getMessage());
            } catch (Exception logError) {
                // Fallback logging if even that fails
                System.err.println("Critical error in PlayerLoginEvent handler");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            // Null checks for event safety
            if (event == null) {
                return;
            }
            
            Player player = event.getPlayer();
            if (player == null || !player.isOnline()) {
                return;
            }
            
            // Check server state in a thread-safe manner
            // Cache the server state to avoid multiple calls during the same event
            boolean isServerOpen = serverScheduler.isServerOpen();
            
            if (isServerOpen) {
                // Get next state change info
                String nextChange = serverScheduler.getNextStateChange();
                
                // Validate the message before sending
                if (nextChange != null && !nextChange.trim().isEmpty() && 
                    !nextChange.equals("No scheduled changes")) {
                    
                    // Additional check to ensure player is still online before sending message
                    if (player.isOnline()) {
                        try {
                            player.sendMessage("§7Server schedule: " + nextChange);
                        } catch (Exception messageError) {
                            // Log message sending error but don't propagate
                            plugin.getLogger().warning("Error sending schedule message to player " + 
                                player.getName() + ": " + messageError.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't prevent join if there's an unexpected error
            try {
                plugin.getLogger().severe("Error in PlayerJoinEvent handler: " + e.getMessage());
            } catch (Exception logError) {
                // Fallback logging if even that fails
                System.err.println("Critical error in PlayerJoinEvent handler");
            }
        }
    }
}