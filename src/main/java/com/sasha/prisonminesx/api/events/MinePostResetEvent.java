package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired immediately after the FAWE reset layer task has completely finished.
 */
public class MinePostResetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;

    public MinePostResetEvent(Mine mine) {
        this.mine = mine;
    }

    public Mine getMine() { return mine; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}