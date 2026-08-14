package com.ecosystem.simulation.simulation;

/**
 * Observer interface for domain-level simulation events.
 *
 * <p>Entities fire events (hunt success, reproduction, plant consumed) by calling
 * {@link #onEntityEvent} on this listener.  The listener implementation lives in
 * the presentation layer ({@code gui.EventLogPanel}), but because entities only
 * depend on this interface — defined here in the {@code simulation} package —
 * the domain model has <em>no compile-time dependency on any Swing class</em>.</p>
 *
 * <p>This is an application of the <strong>Observer pattern</strong>: the domain
 * (subject) raises events; the GUI (observer) reacts without the domain knowing
 * which concrete class handles them.</p>
 *
 * <p>Existing rule-execution events are handled by the separate
 * {@link com.ecosystem.simulation.rules.RuleEngine.RuleExecutionListener} interface
 * already present in {@code RuleEngine}.</p>
 */
public interface SimulationEventListener {

    /**
     * Called when a significant entity-level event occurs during the simulation.
     *
     * @param event      short description of what happened, e.g. {@code "hunted successfully"},
     *                   {@code "reproduced"}, {@code "consumed plant"}
     * @param entityType simple class name of the entity that fired the event,
     *                   e.g. {@code "Predator"}, {@code "Herbivore"}, {@code "Plant"}
     */
    void onEntityEvent(String event, String entityType);
}
