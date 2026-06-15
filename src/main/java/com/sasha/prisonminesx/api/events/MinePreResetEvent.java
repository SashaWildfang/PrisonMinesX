package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired immediately before a mine begins to reset.
 * Can be cancelled by other plugins to prevent the reset.
 */
public class MinePreResetEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final boolean isForced;
    private boolean cancelled = false;

    public MinePreResetEvent(Mine mine, boolean isForced) {
        this.mine = mine;
        this.isForced = isForced;
    }

    public Mine getMine() { return mine; }
    public boolean isForced() { return isForced; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}