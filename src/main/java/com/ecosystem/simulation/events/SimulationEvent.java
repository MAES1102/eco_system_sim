package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for all discrete simulation events.
 *
 * <p>This is the cornerstone of the future-event-list (FEL) discrete-event
 * simulation: {@link com.ecosystem.simulation.simulation.SimulationEngine}
 * never advances its clock by a fixed increment — it always pops the earliest
 * event from the FEL and sets its clock to that event's {@link #scheduledTime},
 * so state only ever changes inside {@link #execute}.</p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Abstraction</b>: common data ({@code scheduledTime}, {@code source},
 *       {@code sequenceNumber}) and the abstract contract ({@link #execute},
 *       {@link #getDescription}) are defined here.</li>
 *   <li><b>Inheritance / inclusion polymorphism</b>: {@link EntityActivityEvent},
 *       {@link EnvironmentRegenerationEvent}, {@link DeathEvent},
 *       {@link MovementEvent}, {@link PredationEvent}, and
 *       {@link ReproductionEvent} all extend this class; the FEL processes them
 *       exclusively through this base type — {@code event.execute(ctx)} dispatches
 *       to the correct override at runtime.</li>
 * </ul>
 *
 * <h3>Deterministic ordering</h3>
 * <p>{@code java.util.PriorityQueue} does not guarantee FIFO order for elements
 * that compare equal, so {@link #compareTo} breaks ties on a strictly
 * monotonically increasing {@code sequenceNumber} assigned at construction —
 * two events scheduled for the same tick are always processed in the order they
 * were created.</p>
 */
public abstract class SimulationEvent implements Comparable<SimulationEvent> {

    private static final AtomicLong NEXT_SEQUENCE = new AtomicLong(0);

    /** Simulation tick at which this event should be processed. */
    private final int scheduledTime;

    /** The entity that generated this event (may be {@code null} for world-level events). */
    private final Entity source;

    /** Monotonic insertion order, used only to break {@code scheduledTime} ties deterministically. */
    private final long sequenceNumber;

    protected SimulationEvent(int scheduledTime, Entity source) {
        if (scheduledTime < 0) {
            throw new IllegalArgumentException("scheduledTime must be >= 0, got: " + scheduledTime);
        }
        this.scheduledTime = scheduledTime;
        this.source = source;
        this.sequenceNumber = NEXT_SEQUENCE.getAndIncrement();
    }

    /**
     * Executes the effect of this event.
     *
     * <p>Called by {@link com.ecosystem.simulation.simulation.SimulationEngine}
     * when this event is popped from the future-event list. Implementations that
     * mutate other entities/world state must re-validate their preconditions
     * first (lazy invalidation) — the entity that scheduled this event may have
     * died or been marked {@code pendingRemoval} in the interim.</p>
     *
     * @param ctx scheduling seam for reading the clock and enqueuing follow-up events
     */
    public abstract void execute(SchedulingContext ctx);

    /** Short, human-readable description used by the GUI event log and headless report. */
    public abstract String getDescription();

    public int getScheduledTime() {
        return scheduledTime;
    }

    public Entity getSource() {
        return source;
    }

    /**
     * Natural ordering: earliest {@code scheduledTime} first; ties broken by
     * {@code sequenceNumber} (insertion order) for deterministic FEL processing.
     */
    @Override
    public int compareTo(SimulationEvent other) {
        int byTime = Integer.compare(this.scheduledTime, other.scheduledTime);
        if (byTime != 0) {
            return byTime;
        }
        return Long.compare(this.sequenceNumber, other.sequenceNumber);
    }

    @Override
    public String toString() {
        return "[t=" + scheduledTime + "] " + getDescription();
    }
}
