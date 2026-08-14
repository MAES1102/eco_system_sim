package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Abstract base class for all discrete simulation events.
 *
 * <p>This class is the cornerstone of the <strong>Discrete-Event Simulation</strong>
 * architecture.  Instead of evaluating every entity every tick with no memory of
 * <em>what caused</em> a state change, the system records each significant occurrence
 * as a typed, self-describing {@code SimulationEvent} object.  Events are ordered by
 * their {@link #scheduledTime} inside an {@link EventQueue} and processed in
 * chronological order, just like a real-world DES engine (e.g. SimPy, JSIM).</p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Abstraction</b>: common data ({@code scheduledTime}, {@code source}) and the
 *       abstract contract ({@code execute()}, {@code getDescription()}) are defined
 *       here; concrete behaviour lives in subclasses.</li>
 *   <li><b>Inheritance</b>: {@link DeathEvent}, {@link MovementEvent},
 *       {@link PredationEvent} and {@link ReproductionEvent} all extend this class.</li>
 *   <li><b>Polymorphism (inclusion)</b>: the {@link EventQueue} and
 *       {@link com.ecosystem.simulation.simulation.SimulationEngine} work exclusively
 *       through this base type — {@code event.execute()} dispatches to the correct
 *       subclass at runtime.</li>
 * </ul>
 *
 * <h3>Extending the system</h3>
 * To add a new event type (e.g. {@code WeatherEvent}) simply:
 * <ol>
 *   <li>Create a subclass of {@code SimulationEvent}.</li>
 *   <li>Implement {@link #execute()} and {@link #getDescription()}.</li>
 *   <li>Enqueue an instance via {@code entity.getEventQueue().enqueue(new WeatherEvent(...))}.</li>
 * </ol>
 * No existing code needs to change — satisfying the <em>Open/Closed Principle</em>.
 */
public abstract class SimulationEvent implements Comparable<SimulationEvent> {

    /** Simulation tick at which this event should be processed. */
    private final int scheduledTime;

    /** The entity that generated this event (may be {@code null} for world-level events). */
    private final Entity source;

    /**
     * Constructs a new simulation event.
     *
     * @param scheduledTime the tick at which the event occurs (must be &ge; 0)
     * @param source        the entity that caused this event; {@code null} is allowed
     *                      for environment-level events
     * @throws IllegalArgumentException if {@code scheduledTime} is negative
     */
    protected SimulationEvent(int scheduledTime, Entity source) {
        if (scheduledTime < 0) {
            throw new IllegalArgumentException("scheduledTime must be >= 0, got: " + scheduledTime);
        }
        this.scheduledTime = scheduledTime;
        this.source = source;
    }

    /**
     * Executes the effect of this event.
     *
     * <p>Called by {@link EventQueue#processAll()} for every dequeued event.
     * Subclasses perform their specific side-effects here (logging, statistics
     * updates, chained event scheduling, etc.).</p>
     */
    public abstract void execute();

    /**
     * Returns a human-readable description of this event.
     * Used by the GUI event log and the headless report.
     *
     * @return short description string, e.g. {@code "Predator#3 killed Herbivore#7"}
     */
    public abstract String getDescription();

    /**
     * Returns the simulation tick at which this event is scheduled.
     *
     * @return scheduled tick (&ge; 0)
     */
    public int getScheduledTime() {
        return scheduledTime;
    }

    /**
     * Returns the entity that generated this event.
     *
     * @return source entity, or {@code null} for environment-level events
     */
    public Entity getSource() {
        return source;
    }

    /**
     * Natural ordering by scheduled time, enabling priority-queue insertion.
     * Events with the same tick are ordered in FIFO insertion order (stable).
     */
    @Override
    public int compareTo(SimulationEvent other) {
        return Integer.compare(this.scheduledTime, other.scheduledTime);
    }

    @Override
    public String toString() {
        return "[t=" + scheduledTime + "] " + getDescription();
    }
}
