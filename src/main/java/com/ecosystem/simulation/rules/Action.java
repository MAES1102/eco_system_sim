package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.simulation.World;

/**
 * Functional interface representing an operation applied to a simulation entity
 * when a rule's condition is satisfied.
 *
 * <h3>Why a separate Action interface?</h3>
 * <p>In the original design, actions were an internal {@code switch} inside
 * {@link Rule#executeAction}.  Making {@code Action} a first-class type allows:</p>
 * <ul>
 *   <li>Programmatic rule construction without touching the rule file.</li>
 *   <li>Composing multiple actions into a single rule (e.g. "die AND log").</li>
 *   <li>Unit-testing actions in isolation.</li>
 *   <li>Adding new action types (e.g. {@code teleport}, {@code mutate}) by implementing
 *       this interface — satisfying the <em>Open/Closed Principle</em>.</li>
 * </ul>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Interface / subtyping polymorphism</b>: {@link RuleEngine} calls
 *       {@code action.execute(entity, world)} through this interface regardless
 *       of which concrete implementation is behind it.</li>
 *   <li><b>Parametric polymorphism</b>: Lambda syntax is supported because this
 *       is a {@code @FunctionalInterface}.</li>
 * </ul>
 *
 * <h3>Supported action strings (parsed by Rule)</h3>
 * <pre>
 *   die                   — entity dies immediately
 *   move                  — animal performs one random move
 *   flee                  — herbivore moves away (falls back to move)
 *   grow                  — plant gains energy
 *   reproduce             — entity reproduces if able
 *   energy -20            — organism loses 20 energy units
 *   energy +30            — organism gains 30 energy units
 * </pre>
 */
@FunctionalInterface
public interface Action {

    /**
     * Applies this action to the given entity.
     *
     * @param entity the entity to act upon; never {@code null} when called by {@link RuleEngine}
     * @param world  the simulation world (needed for spatial actions like {@code move});
     *               may be {@code null} for entity-only actions
     */
    void execute(Entity entity, World world);
}
