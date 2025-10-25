package com.servertimes.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermissionUtil {
    
    /**
     * Check if a command sender is an operator (has admin permissions)
     */
    public static boolean hasAdminPermission(CommandSender sender) {
        return sender.isOp();
    }
    
    /**
     * Send a no permission message to the sender
     */
    public static void sendNoPermissionMessage(CommandSender sender) {
        sender.sendMessage("§cYou must be an operator to use this command.");
    }
    
    /**
     * Check admin permission and send message if denied
     */
    public static boolean checkAdminPermission(CommandSender sender) {
        if (!hasAdminPermission(sender)) {
            sendNoPermissionMessage(sender);
            return false;
        }
        return true;
    }
}