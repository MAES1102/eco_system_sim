package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event fired when an entity dies — starvation, old age, predation,
 * grazing, or a user rule's {@code die} command.
 *
 * <p>This is the <b>single, unified owner</b> of the "this entity is dead"
 * mutation: {@link Entity#die()}, {@code Statistics.recordDeath}, and
 * {@code World.removeEntity} are called from exactly one place in the entire
 * codebase — here — regardless of what caused the death. Every other piece of
 * code that wants an entity dead calls {@link Entity#scheduleRemoval(String)}
 * instead of touching these directly.</p>
 */
public class DeathEvent extends SimulationEvent {

    private final String cause;

    public DeathEvent(int scheduledTime, Entity source, String cause) {
        super(scheduledTime, source);
        this.cause = (cause != null && !cause.isBlank()) ? cause : "unknown";
    }

    @Override
    public void execute(SchedulingContext ctx) {
        Entity e = getSource();
        if (e == null || !e.isAlive()) {
            return;
        }
        e.die();
        ctx.getStatistics().recordDeath(e.getClass().getSimpleName());
        if ("grazed".equals(cause)) {
            ctx.getStatistics().recordPlantConsumed();
        }
        ctx.getWorld().removeEntity(e);
        if (ctx.getSimulationEventListener() != null
                && ("predation".equals(cause) || "rule".equals(cause))) {
            ctx.getSimulationEventListener().onEntityEvent("died (" + cause + ")", e.getClass().getSimpleName());
        }
    }

    @Override
    public String getDescription() {
        Entity e = getSource();
        String id = (e != null) ? e.getClass().getSimpleName() + "#" + e.getId() : "Unknown";
        return id + " died -- cause: " + cause;
    }

    public String getCause() {
        return cause;
    }
}
