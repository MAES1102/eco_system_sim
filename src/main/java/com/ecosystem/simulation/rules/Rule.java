package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Movable;
import com.ecosystem.simulation.entities.Organism;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.entities.Reproducible;
import com.ecosystem.simulation.simulation.World;

/**
 * Represents a single user-defined rule loaded from the configuration file.
 *
 * <p>Rules follow the external DSL format:</p>
 * <pre>
 *   RuleName | TargetType | Condition | Action
 * </pre>
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Encapsulation</b>: raw strings (name, targetType, conditionStr, actionStr) are
 *       private; the parsed {@link Condition} and {@link Action} objects are also private.
 *       Callers interact only through {@link #matches}, {@link #evaluateCondition},
 *       and {@link #executeAction}.</li>
 *   <li><b>Information hiding</b>: how conditions and actions are parsed is entirely
 *       internal.  A caller adding a new rule never sees string-splitting logic.</li>
 *   <li><b>Composition</b>: each {@code Rule} <em>has</em> a {@link Condition} and an
 *       {@link Action} — rather than being a condition or an action itself.</li>
 *   <li><b>Polymorphism (subtyping)</b>: {@code condition} and {@code action} are
 *       interface references; the concrete lambdas assigned at parse time are chosen at
 *       runtime, not compile time.</li>
 * </ul>
 *
 * <h3>Supported condition fields</h3>
 * {@code energy}, {@code age}, {@code x}, {@code y}
 *
 * <h3>Supported operators</h3>
 * {@code <}, {@code >}, {@code <=}, {@code >=}, {@code ==}, {@code !=}
 *
 * <h3>Supported action strings</h3>
 * {@code die}, {@code move}, {@code flee}, {@code grow}, {@code reproduce},
 * {@code energy -N} (lose N energy), {@code energy +N} (gain N energy)
 */
public class Rule {

    // -------------------------------------------------------------------------
    // Raw strings (stored for display / serialisation)
    // -------------------------------------------------------------------------

    /** Rule identifier displayed in the Active Rules panel. */
    private final String name;

    /**
     * Target entity type this rule applies to.
     * Matched against the entity's full class hierarchy so {@code "Organism"} fires
     * on all living things, {@code "Animal"} fires on both predators and herbivores, etc.
     */
    private final String targetType;

    /** Original condition string, e.g. {@code "energy < 10"}. */
    private final String conditionStr;

    /** Original action string, e.g. {@code "die"} or {@code "energy -20"}. */
    private final String actionStr;

    // -------------------------------------------------------------------------
    // Parsed abstractions (Condition / Action interfaces)
    // -------------------------------------------------------------------------

    /**
     * Parsed condition — evaluated as a {@link Condition} lambda for performance
     * and to demonstrate the <em>Condition abstraction</em> OOP requirement.
     */
    private final Condition condition;

    /**
     * Parsed action — executed as an {@link Action} lambda.
     * Demonstrates the <em>Action abstraction</em> OOP requirement.
     */
    private final Action action;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a rule from raw strings (as produced by {@link RuleEngine#parseRule}).
     * The condition and action strings are immediately parsed into typed lambda objects.
     *
     * @param name         rule identifier
     * @param targetType   entity type name, e.g. {@code "Predator"}
     * @param conditionStr condition expression, e.g. {@code "energy < 10"}
     * @param actionStr    action expression, e.g. {@code "die"} or {@code "energy -20"}
     */
    public Rule(String name, String targetType, String conditionStr, String actionStr) {
        this.name         = name;
        this.targetType   = targetType;
        this.conditionStr = conditionStr;
        this.actionStr    = actionStr;
        this.condition    = parseCondition(conditionStr);
        this.action       = parseAction(actionStr);
    }

    // -------------------------------------------------------------------------
    // Public API (backward-compatible with existing callers)
    // -------------------------------------------------------------------------

    /**
     * Checks whether this rule applies to the given entity by walking the full
     * class hierarchy upward (stopping before {@link Object}).
     *
     * <p>Example: a rule with {@code targetType = "Organism"} will fire on a
     * {@code Predator} because the hierarchy is
     * {@code Predator → Animal → Organism → Entity}.</p>
     *
     * @param entity the entity to test; {@code null} returns {@code false}
     * @return {@code true} if any class in the hierarchy matches {@code targetType}
     */
    public boolean matches(Entity entity) {
        if (entity == null) {
            return false;
        }
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            if (clazz.getSimpleName().equals(this.targetType)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * Evaluates the parsed {@link Condition} against the given entity.
     *
     * @param entity the entity to test
     * @return {@code true} if the condition is satisfied
     */
    public boolean evaluateCondition(Entity entity) {
        if (entity == null || condition == null) {
            return false;
        }
        return condition.evaluate(entity);
    }

    /**
     * Executes the parsed {@link Action} on the given entity.
     * The entity's {@code world} reference is forwarded so spatial actions work.
     *
     * @param entity the entity to act upon
     */
    public void executeAction(Entity entity) {
        executeAction(entity, getWorldFrom(entity));
    }

    /**
     * Executes the parsed {@link Action} on the given entity with an explicit world.
     * Demonstrates <em>method overloading</em>: same name, different parameter lists.
     *
     * @param entity the entity to act upon
     * @param world  world reference (may be {@code null})
     */
    public void executeAction(Entity entity, World world) {
        if (entity == null || action == null) {
            return;
        }
        action.execute(entity, world);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** @return rule name */
    public String getName()        { return name; }

    /** @return raw target type string */
    public String getTargetType()  { return targetType; }

    /** @return raw condition string (for display only) */
    public String getCondition()   { return conditionStr; }

    /** @return raw action string (for display only) */
    public String getAction()      { return actionStr; }

    /**
     * Returns the parsed {@link Condition} object.
     * Allows advanced callers to inspect or compose conditions.
     *
     * @return the condition lambda
     */
    public Condition getParsedCondition() { return condition; }

    /**
     * Returns the parsed {@link Action} object.
     *
     * @return the action lambda
     */
    public Action getParsedAction() { return action; }

    @Override
    public String toString() {
        return name + " | " + targetType + " | " + conditionStr + " | " + actionStr;
    }

    /**
     * Validates condition syntax at load time.
     * Rejects unknown fields/operators instead of silently evaluating to false at runtime.
     *
     * @throws RuleParseException if the condition is not supported
     */
    public static void validateConditionSyntax(String expr, int lineNumber) throws RuleParseException {
        if (expr == null || expr.isBlank()) {
            throw new RuleParseException("Condition cannot be blank", lineNumber);
        }
        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 3) {
            throw new RuleParseException(
                    "Condition must be '<field> <operator> <value>', got: " + expr, lineNumber);
        }
        String field = parts[0];
        String operator = parts[1];
        if (!isSupportedField(field)) {
            throw new RuleParseException(
                    "Unknown condition field '" + field + "'. Supported: energy, age, x, y",
                    lineNumber);
        }
        if (!isSupportedOperator(operator)) {
            throw new RuleParseException(
                    "Unknown operator '" + operator + "'. Supported: <, >, <=, >=, ==, !=",
                    lineNumber);
        }
        try {
            Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new RuleParseException(
                    "Condition value must be an integer, got: " + parts[2], lineNumber);
        }
    }

    /**
     * Validates action syntax at load time.
     * Rejects unknown actions instead of silently no-op at runtime.
     *
     * @throws RuleParseException if the action is not supported
     */
    public static void validateActionSyntax(String expr, int lineNumber) throws RuleParseException {
        if (expr == null || expr.isBlank()) {
            throw new RuleParseException("Action cannot be blank", lineNumber);
        }
        String trimmed = expr.trim();
        if (trimmed.startsWith("energy ")) {
            String deltaStr = trimmed.substring("energy ".length()).trim();
            try {
                Integer.parseInt(deltaStr);
            } catch (NumberFormatException e) {
                throw new RuleParseException(
                        "Energy action must be 'energy +N' or 'energy -N', got: " + expr,
                        lineNumber);
            }
            return;
        }
        switch (trimmed) {
            case "die":
            case "move":
            case "flee":
            case "grow":
            case "reproduce":
                return;
            default:
                throw new RuleParseException(
                        "Unknown action '" + trimmed
                                + "'. Supported: die, move, flee, grow, reproduce, energy +N, energy -N",
                        lineNumber);
        }
    }

    private static boolean isSupportedField(String field) {
        return "energy".equals(field) || "age".equals(field)
                || "x".equals(field) || "y".equals(field);
    }

    private static boolean isSupportedOperator(String operator) {
        return "<".equals(operator) || ">".equals(operator)
                || "<=".equals(operator) || ">=".equals(operator)
                || "==".equals(operator) || "!=".equals(operator);
    }

    // -------------------------------------------------------------------------
    // Private parsing helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a condition string into a {@link Condition} lambda.
     * Format: {@code "<field> <operator> <value>"}
     *
     * @param expr condition expression from the rules file
     * @return a {@code Condition} that evaluates the expression, or one that
     *         always returns {@code false} if the expression is malformed
     */
    private static Condition parseCondition(String expr) {
        if (expr == null || expr.isBlank()) {
            return entity -> false;
        }

        String[] parts = expr.trim().split("\\s+");
        if (parts.length != 3) {
            return entity -> false;
        }

        final String field    = parts[0];
        final String operator = parts[1];
        final int    threshold;

        try {
            threshold = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return entity -> false;
        }

        return entity -> {
            int fieldValue = resolveField(entity, field);
            switch (operator) {
                case "<":  return fieldValue <  threshold;
                case ">":  return fieldValue >  threshold;
                case "<=": return fieldValue <= threshold;
                case ">=": return fieldValue >= threshold;
                case "==": return fieldValue == threshold;
                case "!=": return fieldValue != threshold;
                default:   return false;
            }
        };
    }

    /**
     * Resolves a named field to its current integer value on {@code entity}.
     * Supported fields: {@code energy}, {@code age}, {@code x}, {@code y}.
     */
    private static int resolveField(Entity entity, String field) {
        switch (field) {
            case "energy":
                return (entity instanceof Organism) ? ((Organism) entity).getEnergy() : 0;
            case "age":
                return (entity instanceof Organism) ? ((Organism) entity).getAge() : 0;
            case "x":
                return entity.getX();
            case "y":
                return entity.getY();
            default:
                return 0;
        }
    }

    /**
     * Parses an action string into an {@link Action} lambda.
     *
     * <p>Supported formats:</p>
     * <pre>
     *   die
     *   move
     *   flee
     *   grow
     *   reproduce
     *   energy -N     (lose N energy; coercion example: String → int)
     *   energy +N     (gain N energy)
     * </pre>
     *
     * @param expr action expression from the rules file
     * @return the corresponding {@code Action} lambda
     */
    private static Action parseAction(String expr) {
        if (expr == null || expr.isBlank()) {
            return (entity, world) -> { };
        }

        String trimmed = expr.trim();

        // ---- energy modifier: "energy -20" or "energy +30" ----
        if (trimmed.startsWith("energy ")) {
            String deltaStr = trimmed.substring("energy ".length()).trim();
            try {
                final int delta = Integer.parseInt(deltaStr);   // coercion: String → int
                return (entity, world) -> {
                    if (entity instanceof Organism) {
                        Organism o = (Organism) entity;
                        if (delta < 0) {
                            o.consumeEnergy(-delta);   // negative delta → lose energy
                        } else {
                            o.gainEnergy(delta);       // positive delta → gain energy
                        }
                    }
                };
            } catch (NumberFormatException e) {
                return (entity, world) -> { };  // malformed, skip
            }
        }

        // ---- named actions ----
        switch (trimmed) {
            case "die":
                return (entity, world) -> entity.die();

            case "move":
                return (entity, world) -> {
                    if (entity instanceof Movable) {
                        ((Movable) entity).move();
                    }
                };

            case "flee":
                return (entity, world) -> {
                    if (entity instanceof Herbivore) {
                        Herbivore herbivore = (Herbivore) entity;
                        if (world != null) {
                            for (Entity neighbor : world.getNeighbors(herbivore, 5)) {
                                if (neighbor instanceof Predator predator && predator.isAlive()) {
                                    herbivore.flee(predator);
                                    return;
                                }
                            }
                        }
                    }
                    if (entity instanceof Movable) {
                        ((Movable) entity).move();
                    }
                };

            case "grow":
                return (entity, world) -> {
                    if (entity instanceof Plant) {
                        ((Plant) entity).grow();
                    }
                };

            case "reproduce":
                // Uses the Reproducible interface — demonstrates subtyping polymorphism.
                // Any future organism that implements Reproducible works here automatically.
                return (entity, world) -> {
                    if (entity instanceof Reproducible) {
                        ((Reproducible) entity).reproduce();
                    }
                };

            default:
                return (entity, world) -> { };  // unknown action — no-op
        }
    }

    /**
     * Extracts the {@link World} reference from an entity using the public getter.
     * Returns {@code null} if the entity has no world reference set.
     */
    private static World getWorldFrom(Entity entity) {
        return entity.getWorld();
    }
}
