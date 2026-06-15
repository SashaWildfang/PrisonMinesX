package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MineCreateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;
    private final Player creator;
    private boolean cancelled = false;

    public MineCreateEvent(Mine mine, Player creator) {
        this.mine = mine;
        this.creator = creator;
    }

    public Mine getMine() { return mine; }
    public Player getCreator() { return creator; }

    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}