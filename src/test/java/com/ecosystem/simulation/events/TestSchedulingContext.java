package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.EntityFactory;
import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.rules.RuleVocabulary;
import com.ecosystem.simulation.simulation.SimulationConfig;
import com.ecosystem.simulation.simulation.SimulationEventListener;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, deterministic {@link SchedulingContext} test double.
 *
 * <p>Unlike the real {@code SimulationEngine}, this captures every scheduled
 * event into {@link #scheduled} instead of maintaining its own future-event
 * list, and lets the test manually set the clock and invoke
 * {@code event.execute(this)} — giving full, deterministic control over event
 * ordering and timing without timers, threads, or a running engine.</p>
 */
public class TestSchedulingContext implements SchedulingContext {

    public final World world;
    public final Statistics statistics;
    public final RuleEngine ruleEngine;
    public final RuleVocabulary vocabulary;
    public final EntityFactory entityFactory;
    public final List<SimulationEvent> scheduled = new ArrayList<>();

    private int clock;

    public TestSchedulingContext(World world, Statistics statistics) {
        this(world, statistics, new RuleVocabulary(), SimulationConfig.defaults());
    }

    public TestSchedulingContext(World world, Statistics statistics, RuleVocabulary vocabulary, SimulationConfig config) {
        this.world = world;
        this.statistics = statistics;
        this.vocabulary = vocabulary;
        this.ruleEngine = new RuleEngine(vocabulary);
        this.entityFactory = new EntityFactory(config, world, statistics, this);
        this.clock = 0;
    }

    @Override
    public void schedule(SimulationEvent event) {
        scheduled.add(event);
    }

    @Override
    public int getClock() {
        return clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    @Override
    public RuleVocabulary getVocabulary() {
        return vocabulary;
    }

    @Override
    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    @Override
    public SimulationEventListener getSimulationEventListener() {
        return null;
    }
}
