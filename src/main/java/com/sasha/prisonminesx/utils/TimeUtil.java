package com.sasha.prisonminesx.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    public static int parseTimeToSeconds(String input) {
        input = input.trim().toLowerCase();

        if (input.matches("\\d+")) return Integer.parseInt(input) * 60; // Default to minutes

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