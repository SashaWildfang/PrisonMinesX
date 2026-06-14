package com.sasha.prisonminesx.hooks;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PrisonMinesXExpansion extends PlaceholderExpansion {

    private final PrisonMinesX plugin;

    public PrisonMinesXExpansion(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "prisonmines";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
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
        // Formats:
        // %prisonmines_[mine]_remaining% -> "85.2%"
        // %prisonmines_[mine]_time% -> "5m 20s"
        // %prisonmines_[mine]_blocks% -> "1402"
        // %prisonmines_[mine]_status% -> "Running"

        String[] args = params.split("_");
        if (args.length < 2) return null;

        String mineName = args[0];
        String stat = args[1];

        Mine mine = plugin.getMineManager().getMine(mineName);
        if (mine == null) return "N/A";

        switch (stat.toLowerCase()) {
            case "remaining":
                return String.format("%.1f", mine.getPercentRemaining()) + "%";
            case "time":
                return TimeUtil.formatTime(mine.getTimeUntilReset());
            case "blocks":
                return String.valueOf(mine.getMinedBlocks());
            case "total":
                return String.valueOf(mine.getTotalBlocks());
            case "status":
                return mine.isPaused() ? "Paused" : "Running";
            default:
                return null;
        }
    }
}