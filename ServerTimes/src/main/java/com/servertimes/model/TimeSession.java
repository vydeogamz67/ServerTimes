package com.servertimes.model;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import com.servertimes.utils.TimezoneUtil;

public class TimeSession {
    private LocalTime startTime;
    private LocalTime endTime;
    
    public TimeSession(LocalTime startTime, LocalTime endTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public TimeSession(String startTimeStr, String endTimeStr) throws DateTimeParseException {
        try {
            this.startTime = parseTime(startTimeStr);
            this.endTime = parseTime(endTimeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format in TimeSession: " + startTimeStr + " - " + endTimeStr, e);
        }
    }
    
    private LocalTime parseTime(String timeStr) throws DateTimeParseException {
        try {
            // Support multiple time formats: HH:mm, H:mm, HHam/pm, H:am/pm
            if (timeStr == null || timeStr.trim().isEmpty()) {
                throw new DateTimeParseException("Time string is empty", timeStr != null ? timeStr : "null", 0);
            }
            
            timeStr = timeStr.toLowerCase().trim();
            
            // Additional validation for extremely long strings
            if (timeStr.length() > 20) {
                throw new DateTimeParseException("Time string too long", timeStr, 0);
            }
            
            if (timeStr.contains("am") || timeStr.contains("pm")) {
                boolean isPM = timeStr.contains("pm");
                timeStr = timeStr.replace("am", "").replace("pm", "").trim();
                
                String[] parts = timeStr.split(":");
                if (parts.length == 0 || parts.length > 2) {
                    throw new DateTimeParseException("Invalid AM/PM time format", timeStr, 0);
                }
                
                try {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    
                    // Validate ranges
                    if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                        throw new DateTimeParseException("Invalid time values for AM/PM format", timeStr, 0);
                    }
                    
                    // Convert to 24-hour format
                    if (isPM && hour != 12) {
                        hour += 12;
                    } else if (!isPM && hour == 12) {
                        hour = 0;
                    }
                    
                    return LocalTime.of(hour, minute);
                } catch (NumberFormatException e) {
                    throw new DateTimeParseException("Invalid number format in AM/PM time", timeStr, 0);
                }
            } else {
                try {
                    // Try parsing as H:mm format first
                    return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
                } catch (Exception e) {
                    try {
                        // Try parsing as HH:mm format
                        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
                    } catch (Exception e2) {
                        throw new DateTimeParseException("Invalid 24-hour time format", timeStr, 0);
                    }
                }
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unexpected error parsing time: " + timeStr, e);
        }
    }
    
    public boolean isCurrentlyActive() {
        try {
            if (startTime == null || endTime == null) {
                return false;
            }
            
            LocalTime now = LocalTime.now();
            if (now == null) {
                return false;
            }
            
            // Handle sessions that cross midnight
            if (startTime.isAfter(endTime)) {
                return now.isAfter(startTime) || now.isBefore(endTime) || now.equals(startTime);
            } else {
                return (now.isAfter(startTime) || now.equals(startTime)) && now.isBefore(endTime);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if session is currently active in a specific timezone
     */
    public boolean isCurrentlyActive(ZoneId timezone) {
        try {
            if (startTime == null || endTime == null) {
                return false;
            }
            
            if (timezone == null) {
                timezone = ZoneId.systemDefault();
            }
            
            LocalTime now = TimezoneUtil.getCurrentTimeInTimezone(timezone);
            if (now == null) {
                return false;
            }
            
            // Handle sessions that cross midnight
            if (startTime.isAfter(endTime)) {
                return now.isAfter(startTime) || now.isBefore(endTime) || now.equals(startTime);
            } else {
                return (now.isAfter(startTime) || now.equals(startTime)) && now.isBefore(endTime);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if session is currently active in a specific timezone by abbreviation
     */
    public boolean isCurrentlyActive(String timezoneAbbr) {
        try {
            if (timezoneAbbr == null || timezoneAbbr.trim().isEmpty()) {
                return isCurrentlyActive();
            }
            
            ZoneId zoneId = TimezoneUtil.getZoneId(timezoneAbbr);
            return isCurrentlyActive(zoneId);
        } catch (Exception e) {
            return false;
        }
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public void setStartTime(LocalTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        this.startTime = startTime;
    }
    
    public void setEndTime(LocalTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        this.endTime = endTime;
    }
    
    @Override
    public String toString() {
        try {
            if (startTime == null || endTime == null) {
                return "Invalid TimeSession (null times)";
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return String.format("%s-%s", 
                startTime.format(formatter), 
                endTime.format(formatter)
            );
        } catch (Exception e) {
            return "Invalid TimeSession";
        }
    }
    
    public String toConfigString() {
        try {
            if (startTime == null || endTime == null) {
                return "00:00-00:00";
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return String.format("%s-%s", 
                startTime.format(formatter), 
                endTime.format(formatter)
            );
        } catch (Exception e) {
            return "00:00-00:00";
        }
    }
}