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

public class PlayerListener implements Listener {

    private final PrisonMinesX plugin;

    public PlayerListener(PrisonMinesX plugin) {
        this.plugin = plugin;
    }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();

            // Check exact position and 1 block below to eliminate edge cases where
            // the client registers impact on the boundary top of the mine.
            Location loc = player.getLocation();
            Mine mineAtFeet = plugin.getMineManager().getMineAt(loc);
            Mine mineBelow = plugin.getMineManager().getMineAt(loc.clone().subtract(0, 1, 0));

            Mine targetMine = (mineAtFeet != null) ? mineAtFeet : mineBelow;

            if (targetMine != null && !targetMine.isFallDamage()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Mine mine = plugin.getMineManager().getMineAt(event.getEntity().getLocation());
            if (mine != null && !mine.isPvp()) {
                event.setCancelled(true);

                String prefix = plugin.getMessages().getString("prefix", "");
                String msg = plugin.getMessages().getString("mine.pvp-disabled", "&cCombat is disabled in this mine!");

                // Combine prefix and message first, then translate colors for the whole string
                String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);
                event.getDamager().sendMessage(finalMessage);
            }
        }
    }
}