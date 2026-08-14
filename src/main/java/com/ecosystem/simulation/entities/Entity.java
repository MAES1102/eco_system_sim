package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.EventQueue;
import com.ecosystem.simulation.events.SimulationEvent;
import com.ecosystem.simulation.simulation.SimulationEventListener;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * Abstract base class for all entities in the ecosystem simulation.
 * 
 * This class demonstrates the OOP principle of ABSTRACTION by defining
 * the common properties and behaviors that all simulation entities share,
 * while leaving specific implementations to subclasses.
 * 
 * Key OOP Principles Demonstrated:
 * - Abstraction: Abstract class with abstract method update()
 * - Encapsulation: Protected fields with public methods
 * - Inheritance: Base class for all entity types
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public abstract class Entity {
    
    // Protected fields allow subclasses to access directly while
    // hiding implementation details from external classes (Encapsulation)
    
    /**
     * Unique identifier for this entity.
     * Used to distinguish between different entities in the simulation.
     */
    protected int id;
    
    /**
     * X coordinate of the entity's position in the world.
     * Represents horizontal position (column).
     */
    protected int x;
    
    /**
     * Y coordinate of the entity's position in the world.
     * Represents vertical position (row).
     */
    protected int y;
    
    /**
     * Indicates whether this entity is currently alive.
     * When false, the entity should be removed from the simulation.
     */
    protected boolean alive;
    
    /**
     * Static counter to generate unique IDs for entities.
     * Each new entity gets an incrementing ID.
     */
    private static int nextId = 1;
    
    /**
     * Constructor for Entity.
     * Initializes a new entity with the given position and marks it as alive.
     * Automatically generates a unique ID.
     * 
     * @param x The initial x coordinate (horizontal position)
     * @param y The initial y coordinate (vertical position)
     * @throws IllegalArgumentException if coordinates are negative
     */
    public Entity(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative: x=" + x + ", y=" + y);
        }
        
        this.id = nextId++;  // Assign unique ID and increment counter
        this.x = x;
        this.y = y;
        this.alive = true;   // New entities start alive
    }
    
    /**
     * Abstract method that must be implemented by all subclasses.
     * Defines the behavior of an entity during each simulation step.
     * 
     * This demonstrates ABSTRACTION - the Entity class defines
     * that all entities must be updatable, but doesn't specify HOW.
     * Each subclass provides its own implementation.
     * 
     * Example implementations:
     * - Predator: Move toward prey, hunt, consume energy
     * - Herbivore: Move toward plants, eat, flee from predators
     * - Plant: Grow, possibly reproduce
     */
    public abstract void update();
    
    /**
     * Checks if this entity is currently alive.
     * 
     * @return true if the entity is alive, false otherwise
     */
    public boolean isAlive() {
        return this.alive;
    }
    
    /**
     * Marks this entity as dead.
     * The simulation will remove dead entities during the next cleanup step.
     * This method is typically called when energy reaches 0 or age exceeds maximum.
     */
    public void die() {
        this.alive = false;
    }
    
    /**
     * Gets the unique identifier of this entity.
     * 
     * @return The entity's ID
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * Gets the x coordinate (horizontal position) of this entity.
     * 
     * @return The x coordinate
     */
    public int getX() {
        return this.x;
    }
    
    /**
     * Gets the y coordinate (vertical position) of this entity.
     * 
     * @return The y coordinate
     */
    public int getY() {
        return this.y;
    }
    
    /**
     * Moves this entity to a new position.
     * Clamps coordinates to stay within [0, 49] range for a 50x50 world.
     * 
     * @param newX The new x coordinate
     * @param newY The new y coordinate
     */
    public void moveTo(int newX, int newY) {
        // Clamp coordinates to stay within world boundaries (0-49 for 50x50 world)
        this.x = Math.max(0, Math.min(49, newX));
        this.y = Math.max(0, Math.min(49, newY));
    }
    
    /**
     * Gets the position of this entity as a formatted string.
     * Useful for debugging and display purposes.
     * 
     * @return String representation of position in format "(x, y)"
     */
    public String getPosition() {
        return "(" + x + ", " + y + ")";
    }
    
    /**
     * Returns a string representation of this entity.
     * Useful for debugging and logging.
     * 
     * @return String containing entity type, ID, position, and alive status
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "#" + id + " at " + getPosition();
    }

    /**
     * Reference to the World for spatial queries and entity interactions.
     * Set by SimulationEngine after entity creation.
     */
    protected World world;

    /**
     * Reference to Statistics for recording births/deaths.
     * Set by SimulationEngine after entity creation.
     */
    protected Statistics statistics;

    /**
     * Sets the World reference for spatial queries and entity interactions.
     * Called by SimulationEngine after entity creation.
     * 
     * @param world The World reference
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Returns the World this entity belongs to.
     *
     * @return the world reference, or {@code null} if not yet set
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Sets the Statistics reference for recording births/deaths.
     * Called by SimulationEngine after entity creation.
     * 
     * @param statistics The Statistics reference
     */
    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    protected SimulationEventListener simulationEventListener;

    public void setSimulationEventListener(SimulationEventListener simulationEventListener) {
        this.simulationEventListener = simulationEventListener;
    }

    /**
     * Current simulation tick, kept in sync by SimulationEngine at the start of each step.
     * Entities read this when constructing SimulationEvent objects so that all events
     * produced during the same step carry the same timestamp.
     */
    protected int simulationTime = 0;

    /**
     * Updates the entity's local copy of the simulation clock.
     * Called by SimulationEngine at the top of each step.
     *
     * @param time current tick
     */
    public void setSimulationTime(int time) {
        this.simulationTime = time;
    }

    /**
     * Returns the last known simulation tick for this entity.
     *
     * @return simulation tick
     */
    public int getSimulationTime() {
        return this.simulationTime;
    }

    /**
     * Shared event queue for the discrete-event simulation layer.
     * Entities enqueue typed {@link SimulationEvent} objects here;
     * {@link com.ecosystem.simulation.simulation.SimulationEngine} processes them each tick.
     */
    protected EventQueue<SimulationEvent> eventQueue;

    /**
     * Sets the shared event queue.
     * Called by {@link com.ecosystem.simulation.simulation.SimulationEngine} after entity creation.
     *
     * @param eventQueue the simulation-wide event queue
     */
    public void setEventQueue(EventQueue<SimulationEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    /**
     * Returns the shared event queue.
     *
     * @return the event queue, or {@code null} if not yet injected
     */
    public EventQueue<SimulationEvent> getEventQueue() {
        return this.eventQueue;
    }
}