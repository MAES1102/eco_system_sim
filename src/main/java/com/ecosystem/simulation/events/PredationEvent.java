package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Predator;

/**
 * Discrete event fired when a predator successfully kills a herbivore.
 *
 * <p>Enqueued directly by {@link Predator#attack(Entity)} on a successful kill,
 * making the predation interaction a first-class event.  This is the most
 * important cross-entity interaction in the simulation and benefits most
 * from the event model: future systems (disease spread from carcasses,
 * scavenger mechanics, ecosystem-balance alarms) can intercept this event
 * from the queue without touching predator or herbivore code.</p>
 */
public class PredationEvent extends SimulationEvent {

    /** The herbivore that was killed. */
    private final Entity prey;

    /** Energy gained by the predator from this kill. */
    private final int energyGained;

    /**
     * Constructs a predation event.
     *
     * @param scheduledTime the tick at which the kill occurred
     * @param predator      the predator that made the kill
     * @param prey          the herbivore that was killed
     * @param energyGained  energy transferred to the predator
     */
    public PredationEvent(int scheduledTime, Predator predator, Entity prey, int energyGained) {
        super(scheduledTime, predator);
        this.prey = prey;
        this.energyGained = energyGained;
    }

    /**
     * Executes the predation event.
     * Hook point for chain-reaction events (e.g. disease spread, scavenging).
     */
    @Override
    public void execute() {
        // Hook: schedule a ScavengerEvent, update a food-web graph, trigger disease, etc.
    }

    @Override
    public String getDescription() {
        Entity predator = getSource();
        String predId  = (predator != null) ? predator.getClass().getSimpleName() + "#" + predator.getId() : "Unknown";
        String preyId  = (prey     != null) ? prey.getClass().getSimpleName()     + "#" + prey.getId()     : "Unknown";
        return predId + " killed " + preyId + " (+" + energyGained + " energy)";
    }

    /** Returns the prey entity. */
    public Entity getPrey() { return prey; }

    /** Returns the energy gained by the predator. */
    public int getEnergyGained() { return energyGained; }
}
