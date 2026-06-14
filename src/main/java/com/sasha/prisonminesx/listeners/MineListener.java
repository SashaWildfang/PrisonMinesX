package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class MineListener implements Listener {

    private final PrisonMinesX plugin;
    private final double defaultPercentage;

    public MineListener(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.defaultPercentage = plugin.getConfig().getDouble("settings.default-reset-percentage", 20.0);
    }

    // NEW: Catch players placing blocks inside the mine to prevent cheating the block counter
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        String worldName = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        for (Mine mine : plugin.getMineManager().getMines()) {
            if (!mine.getWorldName().equals(worldName)) continue;

            if (x >= mine.getMinX() && x <= mine.getMaxX() &&
                    y >= mine.getMinY() && y <= mine.getMaxY() &&
                    z >= mine.getMinZ() && z <= mine.getMaxZ()) {

                // Assign hidden metadata tag to the placed block
                event.getBlock().setMetadata("pmx_placed", new FixedMetadataValue(plugin, true));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMineBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        String worldName = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        for (Mine mine : plugin.getMineManager().getMines()) {
            if (!mine.getWorldName().equals(worldName)) continue;

            if (x >= mine.getMinX() && x <= mine.getMaxX() &&
                    y >= mine.getMinY() && y <= mine.getMaxY() &&
                    z >= mine.getMinZ() && z <= mine.getMaxZ()) {

                // If the block was placed by a player, ignore it and remove the tag
                if (event.getBlock().hasMetadata("pmx_placed")) {
                    event.getBlock().removeMetadata("pmx_placed", plugin);
                    return;
                }

                mine.incrementMinedBlocks();

                if (mine.isHologramEnabled()) {
                    plugin.getHologramManager().updateHologram(mine);
                }

                if (mine.isActionbarEnabled()) {
                    String msg = "§b§l" + mine.getName() + " §8| §e" + mine.getMinedBlocks() + "§7/§e" + mine.getTotalBlocks() + " §7Mined §8| §c" + TimeUtil.formatTime(mine.getTimeUntilReset());
                    for (Player p : loc.getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(loc) <= 2500) {
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        }
                    }
                }

                if (!mine.isPaused()) {
                    double threshold = mine.getResetPercentage() != -1.0 ? mine.getResetPercentage() : defaultPercentage;
                    if (threshold > 0 && mine.getPercentRemaining() <= threshold) {
                        plugin.getMineManager().resetMine(mine.getName());
                    }
                }
                return;
            }
        }
    }
}