package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event fired when an entity dies (starvation, old age, or predation).
 *
 * <p>Extends {@link SimulationEvent}, demonstrating <b>inheritance</b> and
 * <b>inclusion polymorphism</b>: an {@link EventQueue}{@code <SimulationEvent>}
 * processes this object through the base-type reference, dispatching the correct
 * {@link #execute()} override at runtime.</p>
 */
public class DeathEvent extends SimulationEvent {

    /** Textual cause of death for logging and statistics. */
    private final String cause;

    /**
     * Constructs a death event.
     *
     * @param scheduledTime the tick at which death occurred
     * @param source        the entity that died
     * @param cause         human-readable cause, e.g. {@code "starvation"}, {@code "old age"},
     *                      {@code "predation"}
     */
    public DeathEvent(int scheduledTime, Entity source, String cause) {
        super(scheduledTime, source);
        this.cause = (cause != null && !cause.isBlank()) ? cause : "unknown";
    }

    /**
     * Executes the death event.
     * Currently a logging hook; downstream listeners (GUI log, extended statistics)
     * can be added here without modifying entity or engine code.
     */
    @Override
    public void execute() {
        // Hook point: extend here to update detailed mortality statistics,
        // trigger follow-on events (e.g. a ScavengerEvent), etc.
    }

    @Override
    public String getDescription() {
        Entity e = getSource();
        String id = (e != null) ? e.getClass().getSimpleName() + "#" + e.getId() : "Unknown";
        return id + " died — cause: " + cause;
    }

    /**
     * Returns the cause of death.
     *
     * @return cause string
     */
    public String getCause() {
        return cause;
    }
}
