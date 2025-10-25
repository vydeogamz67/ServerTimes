package com.servertimes.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TimezoneUtil {
    
    // Common timezone abbreviations mapped to ZoneId - using ConcurrentHashMap for thread safety
    private static final Map<String, ZoneId> TIMEZONE_MAP = new ConcurrentHashMap<>();
    
    static {
        // North American timezones
        TIMEZONE_MAP.put("EST", ZoneId.of("America/New_York"));
        TIMEZONE_MAP.put("CST", ZoneId.of("America/Chicago"));
        TIMEZONE_MAP.put("MST", ZoneId.of("America/Denver"));
        TIMEZONE_MAP.put("PST", ZoneId.of("America/Los_Angeles"));
        TIMEZONE_MAP.put("AST", ZoneId.of("America/Halifax"));
        TIMEZONE_MAP.put("HST", ZoneId.of("Pacific/Honolulu"));
        
        // European timezones
        TIMEZONE_MAP.put("GMT", ZoneId.of("GMT"));
        TIMEZONE_MAP.put("UTC", ZoneId.of("UTC"));
        TIMEZONE_MAP.put("CET", ZoneId.of("Europe/Paris"));
        TIMEZONE_MAP.put("EET", ZoneId.of("Europe/Athens"));
        TIMEZONE_MAP.put("BST", ZoneId.of("Europe/London"));
        
        // Asian timezones
        TIMEZONE_MAP.put("JST", ZoneId.of("Asia/Tokyo"));
        TIMEZONE_MAP.put("KST", ZoneId.of("Asia/Seoul"));
        TIMEZONE_MAP.put("IST", ZoneId.of("Asia/Kolkata"));
        TIMEZONE_MAP.put("CST_CHINA", ZoneId.of("Asia/Shanghai"));
        
        // Australian timezones
        TIMEZONE_MAP.put("AEST", ZoneId.of("Australia/Sydney"));
        TIMEZONE_MAP.put("AWST", ZoneId.of("Australia/Perth"));
        TIMEZONE_MAP.put("ACST", ZoneId.of("Australia/Adelaide"));
    }
    
    /**
     * Get ZoneId from timezone abbreviation
     */
    public static ZoneId getZoneId(String timezoneAbbr) {
        try {
            if (timezoneAbbr == null || timezoneAbbr.trim().isEmpty()) {
                return ZoneId.systemDefault();
            }
            
            String upperAbbr = timezoneAbbr.trim().toUpperCase();
            return TIMEZONE_MAP.getOrDefault(upperAbbr, ZoneId.systemDefault());
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
    
    /**
     * Check if timezone abbreviation is valid
     */
    public static boolean isValidTimezone(String timezoneAbbr) {
        try {
            if (timezoneAbbr == null || timezoneAbbr.trim().isEmpty()) {
                return false;
            }
            return TIMEZONE_MAP.containsKey(timezoneAbbr.trim().toUpperCase());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get all supported timezone abbreviations
     */
    public static Set<String> getSupportedTimezones() {
        try {
            // Return an unmodifiable view to prevent external modification
            return Collections.unmodifiableSet(TIMEZONE_MAP.keySet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    /**
     * Get current time in specified timezone
     */
    public static LocalTime getCurrentTimeInTimezone(ZoneId timezone) {
        try {
            if (timezone == null) {
                timezone = ZoneId.systemDefault();
            }
            return ZonedDateTime.now(timezone).toLocalTime();
        } catch (Exception e) {
            return LocalTime.now();
        }
    }
    
    /**
     * Get current time in specified timezone by abbreviation
     */
    public static LocalTime getCurrentTimeInTimezone(String timezoneAbbr) {
        try {
            ZoneId zoneId = getZoneId(timezoneAbbr);
            return getCurrentTimeInTimezone(zoneId);
        } catch (Exception e) {
            return LocalTime.now();
        }
    }
    
    /**
     * Format timezone for display
     */
    public static String formatTimezoneDisplay(String timezoneAbbr) {
        try {
            if (!isValidTimezone(timezoneAbbr)) {
                return "System Default";
            }
            
            ZoneId zoneId = getZoneId(timezoneAbbr);
            LocalTime currentTime = getCurrentTimeInTimezone(zoneId);
            
            if (zoneId == null || currentTime == null) {
                return "System Default";
            }
            
            return String.format("%s (%s - Current: %02d:%02d)", 
                timezoneAbbr.trim().toUpperCase(), 
                zoneId.getId(),
                currentTime.getHour(),
                currentTime.getMinute()
            );
        } catch (Exception e) {
            return "System Default";
        }
    }
    
    /**
     * Get timezone error message for invalid input
     */
    public static String getTimezoneErrorMessage() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("§cInvalid timezone! Supported timezones:\n");
            sb.append("§eNorth America: §fEST, CST, MST, PST, AST, HST\n");
            sb.append("§eEurope: §fGMT, UTC, CET, EET, BST\n");
            sb.append("§eAsia: §fJST, KST, IST, CST_CHINA\n");
            sb.append("§eAustralia: §fAEST, AWST, ACST");
            return sb.toString();
        } catch (Exception e) {
            return "§cInvalid timezone! Please use a supported timezone abbreviation.";
        }
    }
}