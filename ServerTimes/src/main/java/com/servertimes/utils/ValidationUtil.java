package com.servertimes.utils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class ValidationUtil {
    
    private static final List<String> VALID_DAYS = Arrays.asList(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    );
    
    /**
     * Parse a day string to DayOfWeek enum
     */
    public static DayOfWeek parseDayOfWeek(String dayStr) {
        if (dayStr == null || dayStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return DayOfWeek.valueOf(dayStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Parse and validate a day string
     */
    public static int parseDay(String dayStr) {
        try {
            if (dayStr == null || dayStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Day cannot be null or empty");
            }
            
            dayStr = dayStr.trim().toLowerCase();
            
            // Additional validation for extremely long strings
            if (dayStr.length() > 20) {
                throw new IllegalArgumentException("Day string too long: " + dayStr);
            }
            
            // Check for day names
            switch (dayStr) {
                case "monday": case "mon": return 1;
                case "tuesday": case "tue": return 2;
                case "wednesday": case "wed": return 3;
                case "thursday": case "thu": return 4;
                case "friday": case "fri": return 5;
                case "saturday": case "sat": return 6;
                case "sunday": case "sun": return 7;
            }
            
            // Try to parse as number
            try {
                int day = Integer.parseInt(dayStr);
                if (day < 1 || day > 7) {
                    throw new IllegalArgumentException("Day must be between 1-7");
                }
                return day;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid day format: " + dayStr);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid day format: " + dayStr, e);
        }
    }
    
    /**
     * Validate if a day string is valid
     */
    public static boolean isValidDay(String dayStr) {
        return dayStr != null && VALID_DAYS.contains(dayStr.toLowerCase().trim());
    }
    
    /**
     * Parse and validate a time string
     */
    public static LocalTime parseTime(String timeStr) {
        try {
            if (timeStr == null || timeStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Time cannot be null or empty");
            }
            
            timeStr = timeStr.trim().toLowerCase();
            
            // Additional validation for extremely long strings
            if (timeStr.length() > 20) {
                throw new IllegalArgumentException("Time string too long: " + timeStr);
            }
            
            // Handle AM/PM format
            if (timeStr.contains("am") || timeStr.contains("pm")) {
                return parseAmPmTime(timeStr);
            }
            
            // Handle 24-hour format
            if (timeStr.contains(":")) {
                return parse24HourTime(timeStr);
            }
            
            // Handle compact format (e.g., "1430" for 14:30)
            if (timeStr.matches("\\d{3,4}")) {
                return parseCompactTime(timeStr);
            }
            
            throw new IllegalArgumentException("Invalid time format: " + timeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr, e);
        }
    }
    
    /**
     * Parse AM/PM time format
     */
    private static LocalTime parseAmPmTime(String timeStr) {
        try {
            boolean isPM = timeStr.contains("pm");
            timeStr = timeStr.replace("am", "").replace("pm", "").trim();
            
            String[] parts = timeStr.split(":");
            if (parts.length == 0 || parts.length > 2) {
                throw new IllegalArgumentException("Invalid AM/PM time format");
            }
            
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            
            // Validate ranges
            if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid time values for AM/PM format");
            }
            
            // Convert to 24-hour format
            if (isPM && hour != 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                hour = 0;
            }
            
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in AM/PM time");
        }
    }
    
    /**
     * Parse 24-hour time format
     */
    private static LocalTime parse24HourTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid 24-hour time format");
            }
            
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid time values for 24-hour format");
            }
            
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in 24-hour time");
        }
    }
    
    /**
     * Parse compact time format (e.g., "1430" for 14:30)
     */
    private static LocalTime parseCompactTime(String timeStr) {
        try {
            int hour, minute;
            if (timeStr.length() == 3) {
                // Format: HMM (e.g., 930 for 9:30)
                hour = Integer.parseInt(timeStr.substring(0, 1));
                minute = Integer.parseInt(timeStr.substring(1, 3));
            } else {
                // Format: HHMM (e.g., 1430 for 14:30)
                hour = Integer.parseInt(timeStr.substring(0, 2));
                minute = Integer.parseInt(timeStr.substring(2, 4));
            }
            
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid time values for compact format");
            }
            
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid compact time format");
        }
    }

    /**
     * Validate if a time string is in correct format
     */
    public static boolean isValidTimeFormat(String timeStr) {
        try {
            parseTime(timeStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    /**
     * Validate if a time string is valid
     */
    public static boolean isValidTime(String timeStr) {
        try {
            parseTime(timeStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate if start time is before end time (considering midnight crossover)
     */
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        // Allow sessions that cross midnight
        return !startTime.equals(endTime);
    }
    
    /**
     * Get a user-friendly error message for invalid time format
     */
    public static String getTimeFormatErrorMessage() {
        return "§cInvalid time format. Supported formats:\n" +
               "§7- 24-hour: 21:00, 9:30\n" +
               "§7- 12-hour: 9pm, 9:30pm, 9am\n" +
               "§7- Compact: 2130, 930";
    }
    
    /**
     * Get a user-friendly error message for invalid day
     */
    public static String getDayFormatErrorMessage() {
        return "§cInvalid day. Valid days: monday, tuesday, wednesday, thursday, friday, saturday, sunday";
    }
    
    /**
     * Validate session number for removal commands
     */
    public static boolean isValidSessionNumber(String numberStr, int maxSessions) {
        try {
            int number = Integer.parseInt(numberStr);
            return number >= 1 && number <= maxSessions;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}