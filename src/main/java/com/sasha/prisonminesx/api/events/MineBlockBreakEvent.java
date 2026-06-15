package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player breaks a valid block inside a PrisonMinesX mine.
 * Can be cancelled to prevent the plugin from tracking the block break.
 */
public class MineBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final Player player;
    private final Block block;
    private boolean cancelled = false;

    public MineBlockBreakEvent(Mine mine, Player player, Block block) {
        this.mine = mine;
        this.player = player;
        this.block = block;
    }

    public Mine getMine() { return mine; }
    public Player getPlayer() { return player; }
    public Block getBlock() { return block; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}