package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a mine is paused or unpaused.
 * Pausing a mine freezes its active reset timers and ignores percentage thresholds.
 */
public class MineStateChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final boolean isPausing;
    private boolean cancelled = false;

    public MineStateChangeEvent(Mine mine, boolean isPausing) {
        this.mine = mine;
        this.isPausing = isPausing;
    }

    /** @return The mine changing operational states. */
    public Mine getMine() { return mine; }

    /** @return True if the mine is being paused, false if it is being resumed. */
    public boolean isPausing() { return isPausing; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}