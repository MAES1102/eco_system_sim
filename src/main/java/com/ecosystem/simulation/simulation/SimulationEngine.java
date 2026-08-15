package com.ecosystem.simulation.simulation;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.EntityFactory;
import com.ecosystem.simulation.events.EntityActivityEvent;
import com.ecosystem.simulation.events.EnvironmentRegenerationEvent;
import com.ecosystem.simulation.events.EventQueue;
import com.ecosystem.simulation.events.SchedulingContext;
import com.ecosystem.simulation.events.SimulationEvent;
import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.rules.RuleVocabulary;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * The simulation engine: a genuine future-event-list (FEL) discrete-event
 * engine. Composition: owns {@link World}, {@link RuleEngine},
 * {@link Statistics}, {@link EntityFactory}, and the FEL itself
 * ({@link EventQueue}{@code <SimulationEvent>}).
 *
 * <p>Implements {@link SchedulingContext} directly, so every event's
 * {@code execute(SchedulingContext)} receives exactly the narrow seam it needs
 * (schedule, clock, world, statistics, rule engine, vocabulary, entity factory,
 * GUI listener) without depending on this whole class.</p>
 *
 * <h3>Why this is genuinely discrete-event, not fixed-Δt polling</h3>
 * <p>{@link #advanceTo(int)} pops the single earliest-scheduled event from the
 * FEL, sets {@link #clock} to <em>that event's own</em> {@code scheduledTime},
 * and executes it — repeating until the FEL is empty or the next event's time
 * exceeds the requested boundary. No entity is ever polled directly: every
 * entity acts only because it has a pending {@link EntityActivityEvent}, which
 * reschedules its own successor. The engine never increments the clock on its
 * own initiative; it only ever adopts a popped event's timestamp.</p>
 */
public class SimulationEngine implements SchedulingContext {

    private final SimulationConfig config;
    private final World world;
    private final RuleVocabulary vocabulary;
    private final RuleEngine ruleEngine;
    private final Statistics statistics;
    private final EntityFactory entityFactory;
    private final EventQueue<SimulationEvent> eventQueue;

    private int clock;
    private boolean silent;
    private SimulationEventListener simulationEventListener;

    public SimulationEngine(SimulationConfig config) {
        if (config == null) {
            throw new SimulationException(SimulationException.Code.INVALID_DIMENSIONS);
        }
        int width = config.worldWidth();
        int height = config.worldHeight();
        if (width <= 0 || height <= 0) {
            throw new SimulationException(SimulationException.Code.INVALID_DIMENSIONS);
        }

        this.config = config;
        this.world = new World(width, height);
        this.world.getEnvironment().setFoodLevel(config.environmentFoodLevel());
        this.world.getEnvironment().setMaxFoodLevel(config.environmentMaxFoodLevel());
        this.world.getEnvironment().setFoodRegenRate(config.environmentFoodRegenRate());
        this.world.getEnvironment().setTemperature(config.environmentTemperature());

        this.vocabulary = new RuleVocabulary();
        this.ruleEngine = new RuleEngine(vocabulary);
        this.statistics = new Statistics();
        this.eventQueue = new EventQueue<>();
        this.entityFactory = new EntityFactory(config, world, statistics, this);
        this.clock = 0;
        this.silent = false;
    }

    /**
     * Bootstraps the initial population (via {@link EntityFactory}, from
     * {@link SimulationConfig}) and schedules each entity's first
     * {@link EntityActivityEvent} plus the recurring
     * {@link EnvironmentRegenerationEvent}. Must be called once before
     * advancing the clock.
     */
    public void initialize() {
        for (int i = 0; i < config.initialPredators(); i++) {
            spawnInitial(entityFactory.createPredator(randomPos(world.getWidth()), randomPos(world.getHeight())));
        }
        for (int i = 0; i < config.initialHerbivores(); i++) {
            spawnInitial(entityFactory.createHerbivore(randomPos(world.getWidth()), randomPos(world.getHeight())));
        }
        for (int i = 0; i < config.initialPlants(); i++) {
            spawnInitial(entityFactory.createPlant(randomPos(world.getWidth()), randomPos(world.getHeight())));
        }
        eventQueue.enqueue(new EnvironmentRegenerationEvent(1));
        this.clock = 0;
    }

    private void spawnInitial(Entity entity) {
        world.addEntity(entity);
        statistics.recordInitialEntity(entity.getClass().getSimpleName());
        eventQueue.enqueue(new EntityActivityEvent(1, entity));
    }

    private static int randomPos(int max) {
        return (int) (Math.random() * max);
    }

    // ── DES driving loop ─────────────────────────────────────────────────────

    /**
     * Advances the simulation by one external "tick" — processes every event
     * scheduled at or before {@code clock + 1}. Called once per GUI Timer tick
     * or once per {@code HeadlessRunner} loop iteration; internally, time still
     * advances strictly by adopting popped events' own scheduled times, never
     * by a bare increment.
     */
    public void step() {
        advanceTo(clock + 1);
        printStatus();
    }

    /**
     * Pops and executes events in scheduled-time order (ties broken
     * deterministically by {@code SimulationEvent}'s sequence number) until the
     * FEL is empty or the next pending event's time exceeds {@code targetTime}.
     * The engine's clock is set from each popped event's own
     * {@code getScheduledTime()} — this is the core DES property: the engine
     * does not drain the whole queue regardless of time, and it never advances
     * the clock on its own initiative.
     */
    public void advanceTo(int targetTime) {
        while (eventQueue.peek() != null && eventQueue.peek().getScheduledTime() <= targetTime) {
            SimulationEvent event = eventQueue.dequeue();
            clock = event.getScheduledTime();
            event.execute(this);
        }
        if (clock < targetTime) {
            clock = targetTime;
        }
    }

    private void printStatus() {
        if (silent) {
            return;
        }
        System.out.println("--- Time Step " + clock + " ---");
        System.out.println("Alive Entities: " + world.getAliveEntityCount());
        System.out.println("Food Level: " + world.getEnvironment().getFoodLevel());
    }

    // ── SchedulingContext ─────────────────────────────────────────────────────

    @Override
    public void schedule(SimulationEvent event) {
        eventQueue.enqueue(event);
    }

    @Override
    public int getClock() {
        return clock;
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
        return simulationEventListener;
    }

    // ── misc accessors ───────────────────────────────────────────────────────

    public void setSimulationEventListener(SimulationEventListener listener) {
        this.simulationEventListener = listener;
    }

    public int getTimeStep() {
        return clock;
    }

    public EventQueue<SimulationEvent> getEventQueue() {
        return eventQueue;
    }

    public String generateReport() {
        return statistics.generateReport();
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public SimulationConfig getConfig() {
        return config;
    }
}
