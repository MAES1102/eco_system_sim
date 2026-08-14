package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;

/**
 * Functional interface representing a testable predicate on a simulation entity.
 *
 * <h3>Why an interface instead of an abstract class?</h3>
 * <p>A {@code Condition} has <em>no shared state</em> — each implementation only
 * needs to provide the {@link #evaluate} logic.  Using an interface (rather than
 * an abstract class) allows a single class to implement multiple conditions via
 * composition and enables lambda expressions in tests:
 * <pre>
 *     Condition lowEnergy = entity -&gt; entity instanceof Organism o &amp;&amp; o.getEnergy() &lt; 10;
 * </pre>
 * </p>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Interface / subtyping polymorphism</b>: any class that satisfies
 *       the {@code evaluate(Entity)} contract can be used wherever a
 *       {@code Condition} is expected.</li>
 *   <li><b>Open/Closed Principle</b>: new condition types (e.g. proximity checks,
 *       environmental checks) can be added by implementing this interface without
 *       modifying {@link Rule} or {@link RuleEngine}.</li>
 * </ul>
 *
 * <h3>Provided implementations</h3>
 * <p>{@link Rule} parses condition strings from the rules file and instantiates
 * concrete anonymous implementations internally.  External code may also create
 * programmatic rules using lambda expressions or named classes.</p>
 *
 * <h3>Example condition strings (parsed by Rule)</h3>
 * <pre>
 *   energy &lt; 10
 *   age &gt; 50
 *   energy &gt;= 80
 *   x == 25
 * </pre>
 */
@FunctionalInterface
public interface Condition {

    /**
     * Tests whether this condition is satisfied for the given entity.
     *
     * @param entity the entity to test; never {@code null} when called by {@link RuleEngine}
     * @return {@code true} if the condition holds
     */
    boolean evaluate(Entity entity);
}
