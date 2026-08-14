package com.ecosystem.simulation.simulation;

/**
 * Thrown when the simulation engine encounters an unrecoverable error.
 *
 * <h3>OOP concepts demonstrated</h3>
 * <ul>
 *   <li><b>Inheritance</b>: extends {@link RuntimeException}, inheriting its
 *       message/cause chain and stack-trace facilities.</li>
 *   <li><b>Constructor overloading</b>: three constructors with different signatures
 *       demonstrate <em>overloading polymorphism</em> — same class name, different
 *       parameter lists chosen at compile time.</li>
 *   <li><b>Custom exception hierarchy</b>: domain-specific exceptions make it
 *       possible to catch simulation errors separately from general Java errors,
 *       improving error-handling granularity.</li>
 * </ul>
 *
 * <h3>Usage examples</h3>
 * <pre>
 *   // Message only
 *   throw new SimulationException("World dimensions must be positive");
 *
 *   // Message + cause
 *   throw new SimulationException("Failed to initialise entities", ioException);
 *
 *   // Pre-defined error code
 *   throw new SimulationException(SimulationException.Code.WORLD_EMPTY);
 * </pre>
 *
 * @see com.ecosystem.simulation.rules.RuleParseException
 */
public class SimulationException extends RuntimeException {

    /**
     * Enumerated error codes for well-known failure modes.
     * Using an enum instead of integer codes makes code self-documenting
     * and prevents magic-number bugs.
     */
    public enum Code {
        /** The world was initialised with no entities. */
        WORLD_EMPTY,
        /** World dimensions are zero or negative. */
        INVALID_DIMENSIONS,
        /** An entity action was attempted on a dead entity. */
        ENTITY_ALREADY_DEAD,
        /** The rule file could not be found or parsed. */
        RULE_LOAD_FAILED,
        /** A simulation step was requested while the engine was stopped. */
        ENGINE_NOT_RUNNING
    }

    // -------------------------------------------------------------------------
    // Overloaded constructors — demonstrates overloading polymorphism
    // -------------------------------------------------------------------------

    /**
     * Constructs a {@code SimulationException} with a human-readable message.
     *
     * <p><b>Overload 1 of 3.</b></p>
     *
     * @param message description of the problem
     */
    public SimulationException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code SimulationException} wrapping an underlying cause.
     *
     * <p><b>Overload 2 of 3.</b></p>
     *
     * @param message description of the problem
     * @param cause   the exception that triggered this simulation error
     */
    public SimulationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code SimulationException} from a pre-defined error code.
     * Generates a standard message automatically.
     *
     * <p><b>Overload 3 of 3.</b></p>
     *
     * @param code the enumerated error code
     */
    public SimulationException(Code code) {
        super(buildMessage(code));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String buildMessage(Code code) {
        switch (code) {
            case WORLD_EMPTY:          return "Simulation world contains no entities";
            case INVALID_DIMENSIONS:   return "World dimensions must be greater than zero";
            case ENTITY_ALREADY_DEAD:  return "Cannot perform action on a dead entity";
            case RULE_LOAD_FAILED:     return "Failed to load rules from configuration file";
            case ENGINE_NOT_RUNNING:   return "Cannot advance simulation: engine is not running";
            default:                   return "Simulation error (code=" + code + ")";
        }
    }
}
