package com.sasha.prisonminesx.listeners;

import com.sasha.prisonminesx.PrisonMinesX;
import com.sasha.prisonminesx.api.events.MineBlockBreakEvent;
import com.sasha.prisonminesx.api.events.MinePostResetEvent;
import com.sasha.prisonminesx.models.Mine;
import com.sasha.prisonminesx.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.HashMap;

/**
 * Handles core game mechanics inside the mine.
 * Greatly optimized with Spatial Hashing backing getMineAt.
 */
public class MineListener implements Listener {

    private final PrisonMinesX plugin;
    private final double defaultPercentage;

    public MineListener(PrisonMinesX plugin) {
        this.plugin = plugin;
        this.defaultPercentage = plugin.getConfig().getDouble("settings.default-reset-percentage", 20.0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        Mine mine = plugin.getMineManager().getMineAt(loc);
        if (mine != null) {
            // Enforce Place Blocks flag
            if (!mine.isPlaceBlocks() && !event.getPlayer().hasPermission("prisonminesx.admin.place")) {
                event.setCancelled(true);
                String prefix = plugin.getMessages().getString("prefix", "");
                String msg = plugin.getMessages().getString("mine.place-disabled", "&cYou cannot place blocks in this mine!");
                String finalMessage = ChatColor.translateAlternateColorCodes('&', prefix + msg);
                event.getPlayer().sendMessage(finalMessage);
                return;
            }
            // Tag blocks so they do not count towards the total mined logic
            event.getBlock().setMetadata("pmx_placed", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMineBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        // O(1) lookup via Spatial Hashing
        Mine mine = plugin.getMineManager().getMineAt(loc);

        if (mine != null) {
            if (event.getBlock().hasMetadata("pmx_placed")) {
                event.getBlock().removeMetadata("pmx_placed", plugin);
                return;
            }

            MineBlockBreakEvent apiEvent = new MineBlockBreakEvent(mine, player, event.getBlock());
            Bukkit.getPluginManager().callEvent(apiEvent);

            if (apiEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            boolean customFortune = plugin.getConfig().getBoolean("settings.fortune-all-blocks", false);
            boolean autoPickup = plugin.getConfig().getBoolean("settings.auto-pickup-blocks", false);

            if (autoPickup || customFortune) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                int fortuneLvl = hand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
                Collection<ItemStack> drops = event.getBlock().getDrops(hand);

                if (autoPickup && event.getExpToDrop() > 0) {
                    player.giveExp(event.getExpToDrop());
                    event.setExpToDrop(0);
                }

                event.setDropItems(false);

                for (ItemStack drop : drops) {
                    int amount = drop.getAmount();

                    if (customFortune && fortuneLvl > 0) {
                        int multiplier = new java.util.Random().nextInt(fortuneLvl + 2);
                        if (multiplier == 0) multiplier = 1;
                        amount *= multiplier;
                    }

                    drop.setAmount(amount);

                    if (autoPickup) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                        for (ItemStack left : leftover.values()) loc.getWorld().dropItemNaturally(loc, left);
                    } else {
                        loc.getWorld().dropItemNaturally(loc, drop);
                    }
                }
            }

            mine.incrementMinedBlocks();
            mine.incrementLifetimeMinedBlocks();

            Mine.PlayerRecord record = mine.getActivePlayers().get(player.getUniqueId());
            if (record != null) {
                record.blocksMined++;
                record.lastMined = System.currentTimeMillis();
                String blockName = com.sasha.prisonminesx.commands.MineCommand.formatName(event.getBlock().getType().name());
                record.specificBlocksMined.put(blockName, record.specificBlocksMined.getOrDefault(blockName, 0) + 1);
            }

            if (mine.isHologramEnabled()) {
                plugin.getHologramManager().updateHologram(mine);
            }

            if (mine.isActionbarEnabled()) {
                String rawMsg = plugin.getConfig().getString("actionbar-format", "&9&l%mine% &8| &b%mined%&8/&b%total% &7Mined &8| &7Resets in: &b%time%")
                        .replace("%mine%", mine.getName())
                        .replace("%mined%", String.valueOf(mine.getMinedBlocks()))
                        .replace("%total%", String.valueOf(mine.getTotalBlocks()))
                        .replace("%time%", TimeUtil.formatTime(mine.getTimeUntilReset()));

                String msg = ChatColor.translateAlternateColorCodes('&', rawMsg);
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
        }
    }

    /**
     * Resolves the Async Teleport Bug.
     * Teleports players OUT of the mine strictly AFTER the FAWE reset mathematically completes.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinePostReset(MinePostResetEvent event) {
        Mine mine = event.getMine();
        if (mine.isTeleportOnReset() && mine.getTpLocation() != null) {
            World w = Bukkit.getWorld(mine.getWorldName());
            if (w != null) {
                String prefix = plugin.getMessages().getString("prefix", "");
                String warning = plugin.getMessages().getString("mine.suffocation-warning", "&cWarning: Teleporting you safely out of the resetting mine!");
                String fullMsg = ChatColor.translateAlternateColorCodes('&', prefix + warning);

                for (Player p : w.getPlayers()) {
                    if (p.getLocation().getBlockX() >= mine.getMinX() && p.getLocation().getBlockX() <= mine.getMaxX() &&
                            p.getLocation().getBlockY() >= mine.getMinY() && p.getLocation().getBlockY() <= mine.getMaxY() &&
                            p.getLocation().getBlockZ() >= mine.getMinZ() && p.getLocation().getBlockZ() <= mine.getMaxZ()) {

                        p.teleport(mine.getTpLocation());
                        p.sendMessage(fullMsg);
                    }
                }
            }
        }
    }
}