package com.servertimes.commands;

import com.servertimes.ServerTimesPlugin;
import com.servertimes.data.PlayerDataManager;
import com.servertimes.utils.TimezoneUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TimezoneCommand implements CommandExecutor, TabCompleter {
    private final ServerTimesPlugin plugin;
    private final PlayerDataManager playerDataManager;
    
    public TimezoneCommand(ServerTimesPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validate input parameters
        if (sender == null) {
            plugin.getLogger().warning("TimezoneCommand received null sender");
            return true;
        }
        
        if (args == null) {
            sender.sendMessage("§cInvalid command arguments.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Additional safety check for player
        if (player.getUniqueId() == null) {
            sender.sendMessage("§cError: Unable to identify player.");
            plugin.getLogger().warning("Player with null UUID attempted to use timezone command");
            return true;
        }
        
        if (args.length == 0) {
            // Show current timezone
            try {
                String currentTimezone = playerDataManager.getPlayerTimezone(player.getUniqueId());
                if (currentTimezone != null && !currentTimezone.trim().isEmpty()) {
                    sender.sendMessage("§aYour current timezone: " + TimezoneUtil.formatTimezoneDisplay(currentTimezone));
                } else {
                    sender.sendMessage("§eYou haven't set a timezone yet. Use §f/timezone <timezone>§e to set one.");
                    sender.sendMessage("§eExample: §f/timezone EST");
                }
                showAvailableTimezones(sender);
            } catch (Exception e) {
                sender.sendMessage("§cError retrieving your timezone information.");
                plugin.getLogger().warning("Error retrieving timezone for player " + player.getName() + ": " + e.getMessage());
            }
            return true;
        }
        
        if (args.length == 1) {
            // Validate the timezone argument
            if (args[0] == null || args[0].trim().isEmpty()) {
                sender.sendMessage("§cTimezone argument cannot be empty.");
                sender.sendMessage("§cUsage: /timezone [timezone|reset]");
                showAvailableTimezones(sender);
                return true;
            }
            
            String timezone = args[0].trim();
            
            // Check for excessively long input (potential security issue)
            if (timezone.length() > 50) {
                sender.sendMessage("§cTimezone name is too long. Please use a valid timezone identifier.");
                showAvailableTimezones(sender);
                return true;
            }
            
            // Check for invalid characters that could cause issues
            if (!timezone.matches("^[a-zA-Z0-9_/-]+$")) {
                sender.sendMessage("§cInvalid characters in timezone name. Use only letters, numbers, underscores, hyphens, and forward slashes.");
                showAvailableTimezones(sender);
                return true;
            }
            
            try {
                if (timezone.equalsIgnoreCase("reset") || timezone.equalsIgnoreCase("clear")) {
                    playerDataManager.removePlayerTimezone(player.getUniqueId());
                    sender.sendMessage("§aYour timezone has been reset to server default.");
                    return true;
                }
                
                if (TimezoneUtil.isValidTimezone(timezone)) {
                    playerDataManager.setPlayerTimezone(player.getUniqueId(), timezone.toUpperCase());
                    sender.sendMessage("§aTimezone set to: " + TimezoneUtil.formatTimezoneDisplay(timezone));
                } else {
                    sender.sendMessage(TimezoneUtil.getTimezoneErrorMessage());
                    showAvailableTimezones(sender);
                }
            } catch (Exception e) {
                sender.sendMessage("§cError setting timezone: " + e.getMessage());
                plugin.getLogger().warning("Error setting timezone for player " + player.getName() + ": " + e.getMessage());
            }
            return true;
        }
        
        if (args.length > 1) {
            sender.sendMessage("§cToo many arguments! Timezone should be a single word.");
            sender.sendMessage("§cUsage: /timezone [timezone|reset]");
            sender.sendMessage("§eExample: §f/timezone EST");
            showAvailableTimezones(sender);
            return true;
        }
        
        sender.sendMessage("§cUsage: /timezone [timezone|reset]");
        sender.sendMessage("§eExample: §f/timezone EST");
        showAvailableTimezones(sender);
        return true;
    }
    
    private void showAvailableTimezones(CommandSender sender) {
        sender.sendMessage("§7Available timezones:");
        sender.sendMessage("§eNorth America: §fEST, CST, MST, PST, AST, HST");
        sender.sendMessage("§eEurope: §fGMT, UTC, CET, EET, BST");
        sender.sendMessage("§eAsia: §fJST, KST, IST, CST_CHINA");
        sender.sendMessage("§eAustralia: §fAEST, AWST, ACST");
        sender.sendMessage("§7Use §f/timezone reset§7 to clear your timezone setting.");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            // Validate input parameters
            if (sender == null || args == null) {
                return new ArrayList<>();
            }
            
            if (args.length == 1) {
                Set<String> supportedTimezones = TimezoneUtil.getSupportedTimezones();
                if (supportedTimezones == null) {
                    return new ArrayList<>();
                }
                
                List<String> completions = new ArrayList<>(supportedTimezones);
                completions.add("reset");
                completions.add("clear");
                
                // Validate the input argument
                if (args[0] == null) {
                    return completions;
                }
                
                String input = args[0].toLowerCase().trim();
                
                // Filter completions based on input
                return completions.stream()
                        .filter(tz -> tz != null && tz.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in TimezoneCommand tab completion: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
}