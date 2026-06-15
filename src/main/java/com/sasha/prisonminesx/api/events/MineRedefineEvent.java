package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MineRedefineEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private boolean cancelled = false;

    public MineRedefineEvent(Mine mine) { this.mine = mine; }
    public Mine getMine() { return mine; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}