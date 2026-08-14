package com.ecosystem.simulation.entities;

/**
 * Interface for any entity that is capable of autonomous movement.
 *
 * <h3>Why an interface instead of relying on the abstract class hierarchy?</h3>
 * <p>{@link Plant} does not move, so it must NOT inherit movement capability.
 * Using a separate {@code Movable} interface allows {@link Animal} (and its
 * subclasses {@link Predator} and {@link Herbivore}) to declare movement without
 * forcing {@link Organism} or {@link Entity} to carry the contract.</p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Interface / subtyping</b>: code that only needs movement can depend on
 *       {@code Movable} rather than the concrete {@code Animal} class, reducing
 *       coupling.</li>
 *   <li><b>Separation of concerns</b>: movement ability is its own, independently
 *       testable contract.</li>
 *   <li><b>Liskov Substitution</b>: any {@code Movable} must honour the contract —
 *       calling {@code move()} must result in a valid position change.</li>
 * </ul>
 *
 * <h3>Example usage (inclusion polymorphism)</h3>
 * <pre>
 *   List&lt;Movable&gt; movers = world.getEntitiesOfType(Movable.class);
 *   movers.forEach(Movable::move);
 * </pre>
 */
public interface Movable {

    /**
     * Moves this entity by one step according to its own movement strategy.
     * The step size and direction are determined by the implementing class.
     */
    void move();

    /**
     * Returns the current movement speed of this entity.
     * Speed is expressed in world-grid cells per simulation step.
     *
     * @return speed value (always &gt; 0)
     */
    double getSpeed();
}
