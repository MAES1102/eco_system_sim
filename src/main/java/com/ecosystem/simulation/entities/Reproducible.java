package com.ecosystem.simulation.entities;

/**
 * Interface for any organism capable of sexual or asexual reproduction.
 *
 * <h3>Why separate from {@link Movable}?</h3>
 * <p>Movement and reproduction are orthogonal capabilities.  {@link Plant}
 * reproduces but does not move; both {@link Animal} subtypes both reproduce
 * and move.  Modelling each capability as its own interface follows the
 * <em>Interface Segregation Principle</em>: no organism is forced to implement
 * methods irrelevant to it.</p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Interface / subtyping polymorphism</b>: a population-growth service
 *       can call {@code entity.reproduce()} on any {@code Reproducible} without
 *       knowing whether it is a plant, herbivore, or predator.</li>
 *   <li><b>Open/Closed Principle</b>: a new species (e.g. {@code Scavenger}) can
 *       be added by implementing this interface — no existing code changes.</li>
 * </ul>
 *
 * <h3>Example usage</h3>
 * <pre>
 *   // Rule engine action — reproduce any eligible organism
 *   if (entity instanceof Reproducible r) {
 *       r.reproduce();
 *   }
 * </pre>
 */
public interface Reproducible {

    /**
     * Attempts to create one offspring.
     * Each implementing class is responsible for:
     * <ul>
     *   <li>Checking whether the organism has sufficient energy.</li>
     *   <li>Enforcing a reproduction cooldown to prevent exponential growth.</li>
     *   <li>Enforcing a species-level population cap.</li>
     *   <li>Adding the offspring to the {@link com.ecosystem.simulation.simulation.World}.</li>
     * </ul>
     */
    void reproduce();
}
