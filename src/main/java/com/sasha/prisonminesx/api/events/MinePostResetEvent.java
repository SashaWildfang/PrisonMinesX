package com.sasha.prisonminesx.api.events;

import com.sasha.prisonminesx.models.Mine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired immediately after the FAWE (FastAsyncWorldEdit) reset task has completely finished
 * placing all blocks, schematics, and surfaces.
 * * Crucial for safely teleporting players back into the mine, updating external scoreboards,
 * or spawning custom entities/bosses inside the newly refreshed mine.
 */
public class MinePostResetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Mine mine;

    public MinePostResetEvent(Mine mine) {
        this.mine = mine;
    }

    /** @return The mine that has just finished resetting. */
    public Mine getMine() { return mine; }

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }
}