package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event fired when a parent successfully triggers reproduction.
 *
 * <p>Sole owner of "a new entity now exists": creates the offspring (via
 * {@link com.ecosystem.simulation.entities.EntityFactory}, using configured
 * species defaults — not genetic inheritance), adds it to the world, records
 * the birth exactly once, and schedules the offspring's first
 * {@link EntityActivityEvent}. The parent's own energy cost is deducted inside
 * the parent's own activity event (its own state, not this event's concern).</p>
 */
public class ReproductionEvent extends SimulationEvent {

    public ReproductionEvent(int scheduledTime, Entity parent) {
        super(scheduledTime, parent);
    }

    @Override
    public void execute(SchedulingContext ctx) {
        Entity parent = getSource();
        if (parent == null || !parent.isAlive() || parent.isPendingRemoval()) {
            return;
        }
        Entity offspring = ctx.getEntityFactory().createOffspringNear(parent);
        if (offspring == null) {
            return;
        }
        ctx.getWorld().addEntity(offspring);
        ctx.getStatistics().recordBirth(offspring.getClass().getSimpleName());
        ctx.schedule(new EntityActivityEvent(ctx.getClock() + 1, offspring));
        if (ctx.getSimulationEventListener() != null) {
            ctx.getSimulationEventListener().onEntityEvent("reproduced", parent.getClass().getSimpleName());
        }
    }

    @Override
    public String getDescription() {
        Entity parent = getSource();
        String id = (parent != null) ? parent.getClass().getSimpleName() + "#" + parent.getId() : "Unknown";
        return id + " reproduced";
    }
}
