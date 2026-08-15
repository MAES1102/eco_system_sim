package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Predator;

/**
 * Discrete event fired when a predator's attack succeeds.
 *
 * <p>Sole owner of the predator-side effects of a kill: energy gain, satiation
 * cooldown, and the successful-hunt statistic. It does <b>not</b> flip the
 * prey's {@code alive} flag itself — it schedules a {@link DeathEvent} for the
 * prey (cause {@code "predation"}), keeping death unified through one owner
 * regardless of cause. {@link Predator#attack} only decides success/failure and
 * schedules this event; it performs no mutation itself.</p>
 */
public class PredationEvent extends SimulationEvent {

    private final Entity prey;
    private final int energyGained;

    public PredationEvent(int scheduledTime, Predator predator, Entity prey, int energyGained) {
        super(scheduledTime, predator);
        this.prey = prey;
        this.energyGained = energyGained;
    }

    @Override
    public void execute(SchedulingContext ctx) {
        Entity source = getSource();
        if (!(source instanceof Predator predator) || !predator.isAlive() || predator.isPendingRemoval()) {
            return;
        }
        if (prey == null || !prey.isAlive() || prey.isPendingRemoval()) {
            return;
        }
        predator.gainEnergy(energyGained);
        predator.setFedCooldown(10);
        ctx.getStatistics().recordSuccessfulHunt();
        if (ctx.getSimulationEventListener() != null) {
            ctx.getSimulationEventListener().onEntityEvent("hunted successfully", "Predator");
        }
        prey.scheduleRemoval("predation");
    }

    @Override
    public String getDescription() {
        Entity predator = getSource();
        String predId = (predator != null) ? predator.getClass().getSimpleName() + "#" + predator.getId() : "Unknown";
        String preyId = (prey != null) ? prey.getClass().getSimpleName() + "#" + prey.getId() : "Unknown";
        return predId + " killed " + preyId + " (+" + energyGained + " energy)";
    }

    public Entity getPrey() { return prey; }
    public int getEnergyGained() { return energyGained; }
}
