package com.ecosystem.simulation.simulation;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Organism;
import com.ecosystem.simulation.events.DeathEvent;
import com.ecosystem.simulation.events.EventQueue;
import com.ecosystem.simulation.events.MovementEvent;
import com.ecosystem.simulation.events.SimulationEvent;
import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.statistics.Statistics;

import java.util.List;

/**
 * Main simulation engine that controls the ecosystem simulation.
 * Coordinates World, RuleEngine, and Statistics.
 * 
 * This class demonstrates the OOP principle of COMPOSITION by owning
 * World, RuleEngine, and Statistics objects. These objects cannot exist
 * independently of the simulation engine.
 * 
 * Key OOP Principles Demonstrated:
 * - Composition: Owns World, RuleEngine, Statistics (has-a relationships)
 * - Encapsulation: Private components with controlled access
 * - Single Responsibility: Orchestrates simulation execution
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class SimulationEngine {
    
    /**
     * World object managing entities and spatial organization.
     * Composition relationship - SimulationEngine owns World.
     */
    private World world;
    
    /**
     * RuleEngine object managing and applying user-defined rules.
     * Composition relationship - SimulationEngine owns RuleEngine.
     */
    private RuleEngine ruleEngine;
    
    /**
     * Statistics object tracking population data.
     * Composition relationship - SimulationEngine owns Statistics.
     */
    private Statistics statistics;

    /**
     * Shared event queue for the discrete-event simulation layer.
     *
     * <p>This is the central data structure of the DES architecture.
     * Entities enqueue typed {@link SimulationEvent} objects during their
     * {@code update()} and {@code attack()}/{@code reproduce()} methods;
     * {@link #processEvents()} drains the queue at the end of each tick.</p>
     *
     * <p>The generic bound {@code <SimulationEvent>} demonstrates
     * <em>parametric polymorphism</em>: the queue is typed at compile time
     * but works uniformly for any {@code SimulationEvent} subclass.</p>
     */
    private final EventQueue<SimulationEvent> eventQueue;
    
    /**
     * Indicates whether the simulation is currently running.
     */
    private boolean running;
    
    /**
     * Current simulation time step.
     */
    private int timeStep;
    
    /**
     * Constructor for SimulationEngine.
     * Initializes world with given dimensions and creates components.
     * 
     * @param worldWidth Width of the simulation world
     * @param worldHeight Height of the simulation world
     */
    public SimulationEngine(int worldWidth, int worldHeight) {
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new SimulationException(SimulationException.Code.INVALID_DIMENSIONS);
        }
        this.world = new World(worldWidth, worldHeight);
        this.ruleEngine = new RuleEngine();
        this.statistics = new Statistics();
        this.eventQueue = new EventQueue<>();
        this.running = false;
        this.timeStep = 0;
    }
    
    /**
     * Initializes the simulation with initial entities.
     * Should be called before starting the simulation.
     * 
     * @param initialEntities List of entities to add to the world
     */
    public void initialize(List<Entity> initialEntities) {
        // Add all initial entities to the world
        for (Entity entity : initialEntities) {
            entity.setWorld(world);
            entity.setStatistics(statistics);
            entity.setSimulationEventListener(simulationEventListener);
            entity.setEventQueue(eventQueue);       // inject shared DES event queue
            entity.setSimulationTime(timeStep);     // synchronise clock
            world.addEntity(entity);
            statistics.recordInitialEntity(entity.getClass().getSimpleName());
        }
        
        this.timeStep = 0;
    }
    
    /**
     * Starts the simulation.
     * The simulation will run continuously until stop() is called.
     */
    public void run() {
        this.running = true;
        System.out.println("Simulation started at time step " + timeStep);
        
        while (running) {
            step();
            
            // Small delay to make simulation observable
            try {
                Thread.sleep(1000);  // 1 second per time step
            } catch (InterruptedException e) {
                System.err.println("Simulation interrupted");
                running = false;
            }
        }
        
        System.out.println("Simulation stopped at time step " + timeStep);
    }
    
    /**
     * Executes a single simulation step.
     * This is the core simulation logic.
     */
    /**
     * Executes one discrete-event simulation step.
     *
     * <p>Step sequence:</p>
     * <ol>
     *   <li>Synchronise the simulation clock on all entities.</li>
     *   <li>Update all entities (entity logic fires, events are enqueued).</li>
     *   <li>Apply user-defined rules (may enqueue further events).</li>
     *   <li>Remove dead entities (enqueues {@link DeathEvent} for each).</li>
     *   <li>Process the event queue — all enqueued events are executed
     *       in scheduled-time order.</li>
     *   <li>Regenerate environment food.</li>
     *   <li>Advance the clock.</li>
     * </ol>
     */
    public void step() {
        // Step 1: Synchronise simulation clock on all live entities
        syncClockToEntities();

        // Step 2: Update all entities (may enqueue MovementEvent, PredationEvent, ReproductionEvent)
        updateAllEntities();

        // Step 3: Apply rules to all entities
        applyRulesToAllEntities();

        // Step 4: Enqueue DeathEvents and remove dead entities
        removeDeadEntities();

        // Step 5: Process the discrete-event queue (polymorphic dispatch via execute())
        processEvents();

        // Step 6: Update environment
        updateEnvironment();

        // Step 7: Advance time
        timeStep++;

        // Step 8: Print status
        printStatus();
    }
    
    /**
     * Propagates the current simulation tick to every live entity.
     * Entities use this value to timestamp the events they enqueue.
     */
    private void syncClockToEntities() {
        for (Entity entity : world.getEntities()) {
            entity.setSimulationTime(timeStep);
        }
    }

    /**
     * Updates all entities by calling their update() method.
     * Each entity implements its own update behavior.
     */
    private void updateAllEntities() {
        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }
            int fromX = entity.getX();
            int fromY = entity.getY();
            entity.update();
            if (entity.isAlive() && (entity.getX() != fromX || entity.getY() != fromY)) {
                eventQueue.enqueue(new MovementEvent(
                        timeStep, entity, fromX, fromY, entity.getX(), entity.getY()));
            }
        }
    }
    
    /**
     * Applies all loaded rules to all entities.
     * Rules are evaluated in the order they were loaded.
     */
    private void applyRulesToAllEntities() {
        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (entity.isAlive()) {
                ruleEngine.evaluate(entity);
            }
        }
    }
    
    /**
     * Enqueues a {@link DeathEvent} for each dying entity, records the death in
     * statistics, and removes the entity from the world.
     *
     * <p>This is the single authoritative source for recording deaths —
     * entity classes must NOT call {@code statistics.recordDeath()} themselves
     * to avoid double-counting.</p>
     */
    private void removeDeadEntities() {
        List<Entity> entities = world.getEntities();
        for (Entity entity : entities) {
            if (!entity.isAlive()) {
                // Determine cause for the event description
                String cause = determineCause(entity);
                eventQueue.enqueue(new DeathEvent(timeStep, entity, cause));

                statistics.recordDeath(entity.getClass().getSimpleName());
                world.removeEntity(entity);
            }
        }
    }

    /**
     * Infers the most likely cause of death from the entity's current state.
     * Used to populate {@link DeathEvent} descriptions.
     */
    private String determineCause(Entity entity) {
        if (entity instanceof Organism) {
            Organism o = (Organism) entity;
            if (o.getEnergy() <= 0) return "starvation";
        }
        return "natural causes";
    }

    /**
     * Drains and executes all events currently in the queue.
     *
     * <p>This is the DES "process events" phase.  Each event's
     * {@link SimulationEvent#execute()} is dispatched polymorphically —
     * {@code DeathEvent}, {@code PredationEvent}, {@code ReproductionEvent}, etc.
     * all run through the same loop without any type checking.</p>
     */
    private void processEvents() {
        eventQueue.processAll();
    }
    
    /**
     * Updates environmental conditions.
     * Regenerates food in the environment.
     */
    private void updateEnvironment() {
        world.getEnvironment().regenerateFood();
    }
    
    /**
     * Prints current simulation status.
     * Shows time step and population information.
     * Suppressed when silent mode is enabled.
     */
    private void printStatus() {
        if (silent) return;
        System.out.println("--- Time Step " + timeStep + " ---");
        System.out.println("Alive Entities: " + world.getAliveEntityCount());
        System.out.println("Food Level: " + world.getEnvironment().getFoodLevel());
    }
    
    /**
     * Stops the simulation.
     * The simulation will complete the current step and then exit.
     */
    public void stop() {
        this.running = false;
    }
    
    /**
     * Checks if the simulation is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return this.running;
    }
    
    /**
     * Gets the current time step.
     * 
     * @return Current time step
     */
    public int getTimeStep() {
        return this.timeStep;
    }
    
    /**
     * Gets the world object.
     * 
     * @return The world
     */
    public World getWorld() {
        return this.world;
    }
    
    /**
     * Gets the rule engine object.
     * 
     * @return The rule engine
     */
    public RuleEngine getRuleEngine() {
        return this.ruleEngine;
    }
    
    /**
     * Gets the statistics object.
     * 
     * @return The statistics
     */
    public Statistics getStatistics() {
        return this.statistics;
    }
    
    /**
     * Returns the shared discrete-event queue.
     * External components (GUI, headless runner) can inspect total event counts.
     *
     * @return the event queue
     */
    public EventQueue<SimulationEvent> getEventQueue() {
        return this.eventQueue;
    }

    /**
     * Generates and returns a full simulation report.
     * 
     * @return Formatted statistics report
     */
    public String generateReport() {
        return statistics.generateReport();
    }

    private SimulationEventListener simulationEventListener;

    public void setSimulationEventListener(SimulationEventListener simulationEventListener) {
        this.simulationEventListener = simulationEventListener;
    }

    /**
     * When true, suppresses per-step console output.
     * Useful for headless/batch simulation runs.
     */
    private boolean silent = false;

    public void setSilent(boolean silent) {
        this.silent = silent;
    }
}