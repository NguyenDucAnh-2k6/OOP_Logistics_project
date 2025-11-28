package com.oop.logistics.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtil {

    private static final Pattern RELATIVE_PATTERN = 
        Pattern.compile("(\\d+)\\s+(minute|hour|day|month|year)", Pattern.CASE_INSENSITIVE);

    /**
     * Converts raw Facebook timestamp (can be relative or formatted) to a standard LocalDateTime string.
     */
    public static String parseFacebookTime(String rawTime) {
        if (rawTime == null) {
            return LocalDateTime.now().toString(); // Fallback
        }
        
        String lowerTime = rawTime.toLowerCase(Locale.US);

        // 1. Handle Relative times (e.g., "5 hours ago", "2 days ago")
        Matcher matcher = RELATIVE_PATTERN.matcher(lowerTime);
        if (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            LocalDateTime result = LocalDateTime.now();
            
            if (unit.contains("minute")) result = result.minus(value, ChronoUnit.MINUTES);
            else if (unit.contains("hour")) result = result.minus(value, ChronoUnit.HOURS);
            else if (unit.contains("day")) result = result.minus(value, ChronoUnit.DAYS);
            else if (unit.contains("month")) result = result.minus(value, ChronoUnit.MONTHS);
            else if (unit.contains("year")) result = result.minus(value, ChronoUnit.YEARS);

            return result.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        // 2. Handle Vietnamese Formats (Common Facebook date format example)
        try {
            // Example format: "Thứ Ba, 21 tháng Tám, 2025 lúc 12:30 CH"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MMMM, yyyy 'lúc' hh:mm a", new Locale("vi", "VN"));
            LocalDateTime dateTime = LocalDateTime.parse(rawTime, formatter);
            return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}
        
        // 3. Handle ISO standard (already present or fallback from other platforms)
        try {
            return LocalDateTime.parse(rawTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString();
        } catch (Exception ignored) {}
        
        // 4. Fallback: Return raw string or current time
        return LocalDateTime.now().toString(); 
    }
}