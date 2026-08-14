package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event fired when an entity moves to a new position.
 *
 * <p>Movement events are created by {@link com.ecosystem.simulation.simulation.SimulationEngine}
 * after each entity update by comparing the entity's position before and after its
 * {@code update()} call.  This keeps entity classes clean — they do not need to know
 * about the event system.</p>
 *
 * <p>Storing the <em>origin</em> coordinates alongside the <em>destination</em>
 * enables path-replay, heat-map generation, and territory analysis without
 * modifying any simulation logic.</p>
 */
public class MovementEvent extends SimulationEvent {

    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;

    /**
     * Constructs a movement event.
     *
     * @param scheduledTime the tick at which the movement happened
     * @param source        the entity that moved
     * @param fromX         origin x coordinate
     * @param fromY         origin y coordinate
     * @param toX           destination x coordinate
     * @param toY           destination y coordinate
     */
    public MovementEvent(int scheduledTime, Entity source,
                         int fromX, int fromY, int toX, int toY) {
        super(scheduledTime, source);
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    /** Hook point for movement-based analytics (heat maps, territory tracking). */
    @Override
    public void execute() {
        // Extend here to populate a heat-map array, update territory ownership, etc.
    }

    @Override
    public String getDescription() {
        Entity e = getSource();
        String id = (e != null) ? e.getClass().getSimpleName() + "#" + e.getId() : "Unknown";
        return id + " moved (" + fromX + "," + fromY + ") → (" + toX + "," + toY + ")";
    }

    public int getFromX() { return fromX; }
    public int getFromY() { return fromY; }
    public int getToX()   { return toX; }
    public int getToY()   { return toY; }
}
