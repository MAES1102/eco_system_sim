package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * The recurring, self-rescheduling event through which every entity acts.
 *
 * <p>This is the causal event for routine, own-entity behavior: aging, energy
 * consumption, growth, movement, and decision-making all legitimately happen
 * inside {@link #execute}, because this activity event <em>is</em> the discrete
 * event that causes them (see {@code Entity.update()}). Predation, reproduction,
 * and death are triggered from here but owned by their own dedicated event
 * classes — this event only schedules them.</p>
 *
 * <p>No entity is ever polled by an external loop: each living entity has
 * exactly one pending {@code EntityActivityEvent} at a time, which — if the
 * entity is still alive and not {@link Entity#isPendingRemoval()} at the end of
 * its own execution — reschedules its own successor one tick later. A dead or
 * pending-removal entity's chain simply stops rescheduling, which is how the
 * DES model naturally excludes such entities from all further movement, rule
 * evaluation, reproduction, and targeting.</p>
 */
public class EntityActivityEvent extends SimulationEvent {

    private final Entity entity;

    public EntityActivityEvent(int scheduledTime, Entity entity) {
        super(scheduledTime, entity);
        this.entity = entity;
    }

    @Override
    public void execute(SchedulingContext ctx) {
        if (!entity.isAlive() || entity.isPendingRemoval()) {
            return;
        }

        int fromX = entity.getX();
        int fromY = entity.getY();

        entity.update();

        if (entity.isAlive() && !entity.isPendingRemoval() && (entity.getX() != fromX || entity.getY() != fromY)) {
            ctx.schedule(new MovementEvent(ctx.getClock(), entity, fromX, fromY, entity.getX(), entity.getY()));
        }

        if (entity.isAlive() && !entity.isPendingRemoval()) {
            ctx.getRuleEngine().evaluate(entity, ctx.getWorld(), ctx.getStatistics(), ctx);
        }

        if (entity.isAlive() && !entity.isPendingRemoval()) {
            ctx.schedule(new EntityActivityEvent(ctx.getClock() + 1, entity));
        }
    }

    @Override
    public String getDescription() {
        return entity.getClass().getSimpleName() + "#" + entity.getId() + " activity";
    }

    public Entity getEntity() {
        return entity;
    }
}
