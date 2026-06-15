package com.sasha.prisonminesx.hooks;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers internal PrisonMinesX variables with PlaceholderAPI.
 * Allows scoreboards, actionbars, and other plugins to display mine stats.
 */
public class PrisonMinesXExpansion extends PlaceholderExpansion {

    private final PrisonMinesX plugin;

    public PrisonMinesXExpansion(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "prisonminesx";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Sasha";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Usage Format: %prisonminesx_mine_<name>_<property>%
        String[] parts = params.split("_");
        if (parts.length >= 3 && parts[0].equalsIgnoreCase("mine")) {
            String mineName = parts[1];
            Mine mine = plugin.getMineManager().getMine(mineName);
            if (mine == null) return "Invalid Mine";

            String property = parts[2].toLowerCase();
            switch (property) {
                // Core Timers & Blocks
                case "timeleft":
                    return TimeUtil.formatTime(mine.getTimeUntilReset());
                case "blocks":
                    return String.valueOf(mine.getMinedBlocks());
                case "totalblocks":
                    return String.valueOf(mine.getTotalBlocks());
                case "percent":
                    return String.format("%.1f", mine.getPercentRemaining());
                case "status":
                    return mine.isPaused() ? "Paused" : "Running";

                // Extended Mine Info
                case "world":
                    return mine.getWorldName();
                case "players":
                    return String.valueOf(mine.getActivePlayers().size());
                case "lifetime_mined":
                    return String.valueOf(mine.getLifetimeMinedBlocks());
                case "lifetime_resets":
                    return String.valueOf(mine.getLifetimeResets());

                // Flags & Configurations
                case "reset_delay":
                    return TimeUtil.formatTime(mine.getResetDelay());
                case "reset_percent":
                    return String.valueOf(mine.getResetPercentage());
                case "reset_style":
                    return com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getResetStyle());
                case "fill_mode":
                    return mine.isFillMode() ? "Enabled" : "Disabled";
                case "is_silent":
                    return mine.isSilent() ? "Yes" : "No";
                case "is_paused":
                    return mine.isPaused() ? "Yes" : "No";
                case "surface_block":
                    return mine.getSurface() == null ? "None" : com.sasha.prisonminesx.commands.MineCommand.formatName(mine.getSurface());
            }
        }
        return null;
    }
}