package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Entity;

/**
 * Discrete event fired when any organism reproduces.
 */
public class ReproductionEvent extends SimulationEvent {

    /** The offspring entity created by this reproduction. */
    private final Entity offspring;

    /**
     * Constructs a reproduction event.
     *
     * @param scheduledTime the tick at which reproduction occurred
     * @param parent        the organism that reproduced
     * @param offspring     the newly created organism
     */
    public ReproductionEvent(int scheduledTime, Entity parent, Entity offspring) {
        super(scheduledTime, parent);
        this.offspring = offspring;
    }

    /**
     * Executes the reproduction event.
     * Hook point for population-limit enforcement, lineage recording, etc.
     */
    @Override
    public void execute() {
        // Hook: update a genealogy tree, trigger a population-limit check, etc.
    }

    @Override
    public String getDescription() {
        Entity parent = getSource();
        String parentId    = (parent   != null) ? parent.getClass().getSimpleName()   + "#" + parent.getId()   : "Unknown";
        String offspringId = (offspring != null) ? offspring.getClass().getSimpleName() + "#" + offspring.getId() : "Unknown";
        return parentId + " reproduced → " + offspringId;
    }

    /** Returns the offspring entity. */
    public Entity getOffspring() { return offspring; }
}
