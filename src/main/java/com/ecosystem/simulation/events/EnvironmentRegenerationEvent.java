package com.ecosystem.simulation.events;

/**
 * The recurring, self-rescheduling event that drives periodic environmental
 * regeneration (food regrowth). Demonstrates that periodic <em>state-changing</em>
 * ecosystem processes are modeled the same way entity activity is: a
 * self-scheduling recurring event, not a special-cased call inside the engine's
 * loop. (Purely observational periodic reporting, like statistics snapshots in
 * {@code HeadlessRunner}, deliberately is <em>not</em> modeled as an event,
 * since it never mutates simulation state.)
 */
public class EnvironmentRegenerationEvent extends SimulationEvent {

    public EnvironmentRegenerationEvent(int scheduledTime) {
        super(scheduledTime, null);
    }

    @Override
    public void execute(SchedulingContext ctx) {
        ctx.getWorld().getEnvironment().regenerateFood();
        ctx.schedule(new EnvironmentRegenerationEvent(ctx.getClock() + 1));
    }

    @Override
    public String getDescription() {
        return "Environment regenerated food";
    }
}
