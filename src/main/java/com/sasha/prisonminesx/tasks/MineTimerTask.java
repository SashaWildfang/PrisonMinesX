package com.sasha.prisonminesx.tasks;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MineTimerTask extends BukkitRunnable {

    private final PrisonMinesX plugin;

    public MineTimerTask(PrisonMinesX plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        for (Mine mine : plugin.getMineManager().getMines()) {
            if (mine.isHologramEnabled()) {
                plugin.getHologramManager().updateHologram(mine);
            }

            if (mine.isPaused() || mine.getResetDelay() <= 0) continue;

            int timeRemaining = mine.getTimeUntilReset() - 1;

            if (timeRemaining <= 0) {
                plugin.getMineManager().resetMine(mine.getName());
                continue;
            }

            mine.setTimeUntilReset(timeRemaining);

            if (mine.isActionbarEnabled()) {
                sendRadiusMessage(mine, "§b§l" + mine.getName() + " §8| §e" + mine.getMinedBlocks() + "§7/§e" +
                        mine.getTotalBlocks() + " §7Mined §8| §c" + TimeUtil.formatTime(timeRemaining), true);
            }

            if (!mine.isSilent() && mine.getResetWarnings().contains(timeRemaining)) {
                String warningMsg = plugin.getMessages().getString("mine.reset-warning")
                        .replace("%mine%", mine.getName())
                        .replace("%time%", TimeUtil.formatTime(timeRemaining))
                        .replace("&", "§");

                if (mine.isWarnGlobal()) {
                    Bukkit.broadcastMessage(warningMsg);
                } else {
                    sendRadiusMessage(mine, warningMsg, false);
                }
            }
        }
    }

    private void sendRadiusMessage(Mine mine, String msg, boolean isActionBar) {
        World w = Bukkit.getWorld(mine.getWorldName());
        if (w == null) return;

        org.bukkit.Location center = new org.bukkit.Location(w,
                mine.getMinX() + (mine.getMaxX() - mine.getMinX()) / 2.0,
                mine.getMinY(),
                mine.getMinZ() + (mine.getMaxZ() - mine.getMinZ()) / 2.0);

        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 2500) {
                if (isActionBar) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                } else {
                    p.sendMessage(msg);
                }
            }
        }
    }
}