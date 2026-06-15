package com.sasha.prisonminesx.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the logic required to parse user strings into computable ticks/seconds.
 */
public class TimeUtil {

    /**
     * Parses "1h 30m" into 5400 seconds using RegEx mapping.
     */
    public static int parseTimeToSeconds(String input) {
        input = input.trim().toLowerCase();

        // Defaults to minutes if missing string identifiers
        if (input.matches("\\d+")) return Integer.parseInt(input) * 60;

        int totalSeconds = 0;
        Matcher m = Pattern.compile("(\\d+)\\s*([hms])").matcher(input);
        boolean found = false;

        while (m.find()) {
            found = true;
            int val = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            if (unit.equals("h")) totalSeconds += val * 3600;
            else if (unit.equals("m")) totalSeconds += val * 60;
            else if (unit.equals("s")) totalSeconds += val;
        }

        return found ? totalSeconds : -1;
    }

    /**
     * Formats 5400 seconds into a beautiful "1h 30m" tag for the UI.
     */
    public static String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0) sb.append(s).append("s");
        return sb.toString().trim();
    }
}