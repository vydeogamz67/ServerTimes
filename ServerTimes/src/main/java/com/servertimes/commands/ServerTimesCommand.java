package com.servertimes.commands;

import com.servertimes.ServerTimesPlugin;
import com.servertimes.config.ConfigManager;
import com.servertimes.data.PlayerDataManager;
import com.servertimes.model.TimeSession;
import com.servertimes.scheduler.ServerScheduler;
import com.servertimes.utils.PermissionUtil;
import com.servertimes.utils.TimezoneUtil;
import com.servertimes.utils.ValidationUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerTimesCommand implements CommandExecutor, TabCompleter {
    private final ServerTimesPlugin plugin;
    private final ConfigManager configManager;
    private final ServerScheduler serverScheduler;
    private final PlayerDataManager playerDataManager;
    
    public ServerTimesCommand(ServerTimesPlugin plugin, ConfigManager configManager, ServerScheduler serverScheduler, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.serverScheduler = serverScheduler;
        this.playerDataManager = playerDataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validate sender
        if (sender == null) {
            return false;
        }
        
        if (args == null || args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        // Validate first argument
        if (args[0] == null || args[0].trim().isEmpty()) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase().trim();
        
        // Allow status command for all players
        if (subCommand.equals("status")) {
            return handleStatusCommand(sender);
        }
        
        // All other commands require operator permissions
        if (!PermissionUtil.checkAdminPermission(sender)) {
            return true;
        }
        
        switch (subCommand) {
            case "set":
                return handleSetCommand(sender, args);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender, args);
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /servertimes set <day> <start-time> <end-time>");
            sender.sendMessage("§cExample: /servertimes set monday 9pm 10pm");
            return true;
        }
        
        // Validate arguments are not null or empty
        for (int i = 1; i < args.length; i++) {
            if (args[i] == null || args[i].trim().isEmpty()) {
                sender.sendMessage("§cInvalid argument at position " + (i + 1) + ". Arguments cannot be empty.");
                return true;
            }
        }
        
        DayOfWeek day = ValidationUtil.parseDayOfWeek(args[1].trim());
        if (day == null) {
            sender.sendMessage(ValidationUtil.getDayFormatErrorMessage());
            return true;
        }
        
        try {
            TimeSession session = new TimeSession(args[2].trim(), args[3].trim());
            List<TimeSession> sessions = new ArrayList<>();
            sessions.add(session);
            configManager.setSessionsForDay(day, sessions);
            
            sender.sendMessage("§aSet server times for " + day.name().toLowerCase() + ": " + session.toString());
            serverScheduler.updateSchedule();
        } catch (DateTimeParseException e) {
            sender.sendMessage(ValidationUtil.getTimeFormatErrorMessage());
        } catch (Exception e) {
            sender.sendMessage("§cError setting server times: " + e.getMessage());
            plugin.getLogger().warning("Error in handleSetCommand: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /servertimes add <day> <start-time> <end-time>");
            sender.sendMessage("§cExample: /servertimes add sunday 7pm 8pm");
            return true;
        }
        
        // Validate arguments are not null or empty
        for (int i = 1; i < args.length; i++) {
            if (args[i] == null || args[i].trim().isEmpty()) {
                sender.sendMessage("§cInvalid argument at position " + (i + 1) + ". Arguments cannot be empty.");
                return true;
            }
        }
        
        DayOfWeek day = ValidationUtil.parseDayOfWeek(args[1].trim());
        if (day == null) {
            sender.sendMessage(ValidationUtil.getDayFormatErrorMessage());
            return true;
        }
        
        try {
            TimeSession session = new TimeSession(args[2].trim(), args[3].trim());
            configManager.addSessionForDay(day, session);
            
            sender.sendMessage("§aAdded session for " + day.name().toLowerCase() + ": " + session.toString());
            serverScheduler.updateSchedule();
        } catch (DateTimeParseException e) {
            sender.sendMessage(ValidationUtil.getTimeFormatErrorMessage());
        } catch (Exception e) {
            sender.sendMessage("§cError adding session: " + e.getMessage());
            plugin.getLogger().warning("Error in handleAddCommand: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("§cUsage: /servertimes remove <day> [session-number]");
            sender.sendMessage("§cExample: /servertimes remove monday 1");
            sender.sendMessage("§cOmit session number to remove all sessions for the day");
            return true;
        }
        
        // Validate day argument
        if (args[1] == null || args[1].trim().isEmpty()) {
            sender.sendMessage("§cDay argument cannot be empty.");
            return true;
        }
        
        // Validate session number argument if provided
        if (args.length == 3 && (args[2] == null || args[2].trim().isEmpty())) {
            sender.sendMessage("§cSession number argument cannot be empty.");
            return true;
        }
        
        DayOfWeek day = ValidationUtil.parseDayOfWeek(args[1].trim());
        if (day == null) {
            sender.sendMessage(ValidationUtil.getDayFormatErrorMessage());
            return true;
        }
        
        if (args.length == 2) {
            // Remove all sessions for the day
            configManager.clearSessionsForDay(day);
            sender.sendMessage("§aRemoved all sessions for " + day.name().toLowerCase());
        } else {
            // Remove specific session
            try {
                int sessionNumber = Integer.parseInt(args[2].trim()) - 1; // Convert to 0-based index
                List<TimeSession> sessions = configManager.getSessionsForDay(day);
                
                if (sessions == null || sessions.isEmpty()) {
                    sender.sendMessage("§cNo sessions found for " + day.name().toLowerCase() + ".");
                    return true;
                }
                
                if (sessionNumber < 0 || sessionNumber >= sessions.size()) {
                    sender.sendMessage("§cInvalid session number. Use /servertimes list " + day.name().toLowerCase() + " to see available sessions.");
                    return true;
                }
                
                TimeSession removedSession = sessions.get(sessionNumber);
                configManager.removeSessionForDay(day, sessionNumber);
                sender.sendMessage("§aRemoved session " + (sessionNumber + 1) + " for " + day.name().toLowerCase() + ": " + removedSession.toString());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid session number. Must be a number.");
                return true;
            } catch (Exception e) {
                sender.sendMessage("§cError removing session: " + e.getMessage());
                plugin.getLogger().warning("Error in handleRemoveCommand: " + e.getMessage());
                return true;
            }
        }
        
        serverScheduler.updateSchedule();
        return true;
    }
    
    private boolean handleListCommand(CommandSender sender, String[] args) {
        try {
            if (args.length == 1) {
                // List all days
                sender.sendMessage("§6=== Server Schedule ===");
                Map<DayOfWeek, List<TimeSession>> schedule = configManager.getFullSchedule();
                
                for (DayOfWeek day : DayOfWeek.values()) {
                    List<TimeSession> sessions = schedule.get(day);
                    String dayName = day.name().toLowerCase();
                    dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                    
                    if (sessions == null || sessions.isEmpty()) {
                        sender.sendMessage("§7" + dayName + ": §cClosed");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("§a").append(dayName).append(": §f");
                        for (int i = 0; i < sessions.size(); i++) {
                            TimeSession session = sessions.get(i);
                            if (session != null) {
                                if (i > 0) sb.append(", ");
                                sb.append(session.toString());
                            }
                        }
                        sender.sendMessage(sb.toString());
                    }
                }
            } else if (args.length == 2) {
                // List specific day
                if (args[1] == null || args[1].trim().isEmpty()) {
                    sender.sendMessage("§cDay argument cannot be empty.");
                    return true;
                }
                
                DayOfWeek day = ValidationUtil.parseDayOfWeek(args[1].trim());
                if (day == null) {
                    sender.sendMessage(ValidationUtil.getDayFormatErrorMessage());
                    return true;
                }
                
                List<TimeSession> sessions = configManager.getSessionsForDay(day);
                String dayName = day.name().toLowerCase();
                dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                
                sender.sendMessage("§6=== " + dayName + " Schedule ===");
                if (sessions == null || sessions.isEmpty()) {
                    sender.sendMessage("§cNo sessions scheduled for " + dayName);
                } else {
                    for (int i = 0; i < sessions.size(); i++) {
                        TimeSession session = sessions.get(i);
                        if (session != null) {
                            sender.sendMessage("§a" + (i + 1) + ". §f" + session.toString());
                        }
                    }
                }
            } else {
                sender.sendMessage("§cUsage: /servertimes list [day]");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError listing sessions: " + e.getMessage());
            plugin.getLogger().warning("Error in handleListCommand: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        try {
            configManager.loadConfig();
            serverScheduler.updateSchedule();
            sender.sendMessage("§aConfiguration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cError reloading configuration: " + e.getMessage());
            plugin.getLogger().warning("Error in handleReloadCommand: " + e.getMessage());
        }
        return true;
    }
    
    private boolean handleStatusCommand(CommandSender sender) {
        try {
            DayOfWeek today = DayOfWeek.from(java.time.LocalDate.now());
            
            // Get player's timezone if they are a player
            String playerTimezone = null;
            ZoneId timezone = ZoneId.systemDefault();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                playerTimezone = playerDataManager.getPlayerTimezone(player.getUniqueId());
                if (playerTimezone != null) {
                    timezone = TimezoneUtil.getZoneId(playerTimezone);
                }
            }
            
            // Check if server is open in the player's timezone
            List<TimeSession> todaySessions = configManager.getSessionsForDay(today);
            final String finalPlayerTimezone = playerTimezone; // Make effectively final for lambda
            boolean isOpen = todaySessions.stream().anyMatch(session -> 
                finalPlayerTimezone != null ? session.isCurrentlyActive(finalPlayerTimezone) : session.isCurrentlyActive());
            
            sender.sendMessage("§6=== Server Status ===");
            sender.sendMessage("§fCurrent status: " + (isOpen ? "§aOPEN" : "§cCLOSED"));
            sender.sendMessage("§fToday is: §a" + today.name().toLowerCase());
            
            if (finalPlayerTimezone != null) {
                sender.sendMessage("§fYour timezone: §e" + TimezoneUtil.formatTimezoneDisplay(finalPlayerTimezone));
            } else {
                sender.sendMessage("§fTimezone: §7Server Default (use §f/timezone§7 to set yours)");
            }
            
            if (todaySessions.isEmpty()) {
                sender.sendMessage("§fToday's schedule: §cNo sessions");
            } else {
                sender.sendMessage("§fToday's schedule:");
                for (int i = 0; i < todaySessions.size(); i++) {
                    TimeSession session = todaySessions.get(i);
                    boolean sessionActive = finalPlayerTimezone != null ? 
                        session.isCurrentlyActive(finalPlayerTimezone) : session.isCurrentlyActive();
                    String status = sessionActive ? "§a[ACTIVE]" : "§7[INACTIVE]";
                    sender.sendMessage("  §f" + (i + 1) + ". " + session.toString() + " " + status);
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cError retrieving server status: " + e.getMessage());
            plugin.getLogger().warning("Error in handleStatusCommand: " + e.getMessage());
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== ServerTimes Commands ===");
        sender.sendMessage("§a/servertimes set <day> <start> <end> §7- Set server times for a day");
        sender.sendMessage("§a/servertimes add <day> <start> <end> §7- Add additional session for a day");
        sender.sendMessage("§a/servertimes remove <day> [session#] §7- Remove session(s) for a day");
        sender.sendMessage("§a/servertimes list [day] §7- List current schedule");
        sender.sendMessage("§a/servertimes status §7- Check current server status");
        sender.sendMessage("§a/servertimes reload §7- Reload configuration");
        sender.sendMessage("§7Time formats: 21:00, 9pm, 9:30pm");
        sender.sendMessage("§7Days: monday, tuesday, wednesday, thursday, friday, saturday, sunday");
    }
    

    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // All players can see status command
            List<String> subCommands = new ArrayList<>();
            subCommands.add("status");
            
            // Only operators can see admin commands
            if (PermissionUtil.hasAdminPermission(sender)) {
                subCommands.addAll(Arrays.asList("set", "add", "remove", "list", "reload", "help"));
            }
            
            return subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Only operators can tab complete admin commands
        if (!PermissionUtil.hasAdminPermission(sender)) {
            return completions;
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || 
                                       args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("list"))) {
            // Days of week
            List<String> days = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
            return days.stream()
                    .filter(day -> day.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            // Time suggestions for start time
            return Arrays.asList("9am", "10am", "12pm", "1pm", "6pm", "7pm", "8pm", "9pm", "10pm");
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            // Time suggestions for end time
            return Arrays.asList("10am", "11am", "1pm", "2pm", "7pm", "8pm", "9pm", "10pm", "11pm", "12am");
        }
        
        return completions;
    }
}