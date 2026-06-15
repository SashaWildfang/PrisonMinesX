package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.models.Mine;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Handles region-specific overrides for Players standing within mine bounds.
 * Uses EventPriority.HIGHEST to override other protection plugins like WorldGuard.
 */
public class PlayerListener implements Listener {

    private final PrisonMinesX plugin;

    public PlayerListener(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

    /** Disables hunger depletion if the mine's 'hunger' flag is set to false. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        Mine mine = plugin.getMineManager().getMineAt(player.getLocation());
        if (mine != null && !mine.isHunger()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    /** Nullifies fall damage if the 'fallDamage' flag is disabled, computing blocks below feet. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();

            // Check exact position and 1 block below to eliminate edge cases where
            // the client registers impact on the boundary top of the mine immediately upon falling in.
            Location loc = player.getLocation();
            Mine mineAtFeet = plugin.getMineManager().getMineAt(loc);
            Mine mineBelow = plugin.getMineManager().getMineAt(loc.clone().subtract(0, 1, 0));

            Mine targetMine = (mineAtFeet != null) ? mineAtFeet : mineBelow;

            if (targetMine != null && !targetMine.isFallDamage()) {
                event.setCancelled(true);
            }
        }
    }

    /** Prevents Player vs Player combat if the mine's 'pvp' flag is disabled. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Mine mine = plugin.getMineManager().getMineAt(event.getEntity().getLocation());
            if (mine != null && !mine.isPvp()) {
                event.setCancelled(true);

                String prefix = plugin.getMessages().getString("prefix", "");
                String msg = plugin.getMessages().getString("mine.pvp-disabled", "&cCombat is disabled in this mine!");
                String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);
                event.getDamager().sendMessage(finalMessage);
            }
        }
    }
}