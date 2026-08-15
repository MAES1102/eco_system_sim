package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.EntityFactory;
import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.rules.RuleVocabulary;
import com.ecosystem.simulation.simulation.SimulationEventListener;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * The seam through which a {@link SimulationEvent} (or an entity's own domain
 * logic, invoked from inside {@code EntityActivityEvent.execute()}) can read
 * the current simulation clock, schedule follow-up events, and reach the small
 * set of shared collaborators (world, statistics, rule engine, entity factory)
 * — without holding a reference to the whole {@code SimulationEngine}.
 *
 * <p>Implemented by {@code SimulationEngine}. This is a deliberately narrow
 * interface (Interface Segregation): event classes and entity domain methods
 * depend only on the handful of operations they actually need.</p>
 */
public interface SchedulingContext {

    /** Schedules {@code event} to be executed once the clock reaches its scheduled time. */
    void schedule(SimulationEvent event);

    /** Returns the simulation clock's current value (the scheduled time of the event currently executing). */
    int getClock();

    World getWorld();

    Statistics getStatistics();

    RuleEngine getRuleEngine();

    RuleVocabulary getVocabulary();

    EntityFactory getEntityFactory();

    /** May be {@code null} if no GUI listener is attached (e.g. headless runs). */
    SimulationEventListener getSimulationEventListener();
}
