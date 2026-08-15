package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event recording that an entity moved to a new position.
 *
 * <p><b>Observational only — this event does not cause the movement.</b> The
 * entity's position is already changed synchronously inside its own
 * {@link EntityActivityEvent#execute} (via {@code Entity.update()}), <em>before</em>
 * this event is even constructed. {@code MovementEvent} exists purely to give the
 * DES event log / GUI a structured, timestamped record of what changed, for
 * logging, statistics, or future heat-map/territory analysis — it is
 * deliberately a no-op on {@link #execute}, and is documented as such rather
 * than presented as causal.</p>
 */
public class MovementEvent extends SimulationEvent {

    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;

    public MovementEvent(int scheduledTime, Entity source, int fromX, int fromY, int toX, int toY) {
        super(scheduledTime, source);
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    /** No-op by design — see class Javadoc. */
    @Override
    public void execute(SchedulingContext ctx) {
        // Intentionally empty: this event only records movement that has already
        // happened; it does not, and must not, mutate position itself.
    }

    @Override
    public String getDescription() {
        Entity e = getSource();
        String id = (e != null) ? e.getClass().getSimpleName() + "#" + e.getId() : "Unknown";
        return id + " moved (" + fromX + "," + fromY + ") -> (" + toX + "," + toY + ")";
    }

    public int getFromX() { return fromX; }
    public int getFromY() { return fromY; }
    public int getToX() { return toX; }
    public int getToY() { return toY; }
}
