package com.ecosystem.simulation.entities;

/**
 * Abstract base class for all living organisms in the ecosystem.
 * Extends Entity with biological properties like energy and age.
 * 
 * This class demonstrates the OOP principle of INHERITANCE by extending
 * Entity and adding organism-specific properties and behaviors.
 * 
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Entity, reuses id, position, and alive status
 * - Abstraction: Abstract class, defines biological behaviors
 * - Encapsulation: Protected fields with public methods
 */
public abstract class Organism extends Entity {
    
    /**
     * Current energy level of this organism.
     * Energy is consumed by actions and gained by eating.
     * When energy reaches 0, the organism dies.
     */
    protected int energy;
    
    /**
     * Age of this organism in simulation time steps.
     * Increases each time the organism is updated.
     */
    protected int age;
    
    /**
     * Maximum energy this organism can have.
     * Energy gained beyond this limit is lost.
     */
    protected int maxEnergy;
    
    /**
     * Constructor for Organism.
     * Initializes organism with position and starting energy.
     * 
     * @param x The initial x coordinate
     * @param y The initial y coordinate
     * @param energy The starting energy level
     * @throws IllegalArgumentException if energy is negative
     */
    public Organism(int x, int y, int energy) {
        super(x, y);
        
        if (energy < 0) {
            throw new IllegalArgumentException("Energy cannot be negative: " + energy);
        }
        
        this.energy = energy;
        this.age = 0;
        this.maxEnergy = 100;  // Default maximum energy
    }
    
    /**
     * Increases the organism's energy by the specified amount.
     * Energy cannot exceed maxEnergy.
     * 
     * @param amount The amount of energy to gain
     */
    public void gainEnergy(int amount) {
        this.energy += amount;
        if (this.energy > this.maxEnergy) {
            this.energy = this.maxEnergy;
        }
    }
    
    /**
     * Decreases the organism's energy by the specified amount.
     * If energy reaches 0 or below, this organism's removal is scheduled
     * (cause: "starvation") rather than dying immediately — {@link Entity#die()}
     * is only ever called from {@code DeathEvent.execute()}, keeping death
     * unified through one owner regardless of cause. Calling this from inside
     * an {@code EntityActivityEvent}'s own execution (the normal case) is
     * legitimate: routine energy consumption is owned by that activity event.
     *
     * @param amount The amount of energy to consume
     */
    public void consumeEnergy(int amount) {
        this.energy -= amount;
        if (this.energy <= 0) {
            this.energy = 0;
            scheduleRemoval("starvation");
        }
    }

    // die() inherited from Entity — only ever called from DeathEvent.execute()
    
    /**
     * Gets the current energy level.
     * 
     * @return Current energy
     */
    public int getEnergy() {
        return this.energy;
    }
    
    /**
     * Gets the current age.
     * 
     * @return Current age in time steps
     */
    public int getAge() {
        return this.age;
    }
    
    /**
     * Increases the organism's age by one time step.
     * Called during each update cycle.
     */
    protected void increaseAge() {
        this.age++;
    }
    
    /**
     * Checks if this organism is starving (low energy).
     * 
     * @return true if energy is below starvation threshold (20)
     */
    public boolean isStarving() {
        return this.energy < 20;
    }
    
    /**
     * Sets the maximum energy for this organism.
     * 
     * @param maxEnergy The new maximum energy
     */
    public void setMaxEnergy(int maxEnergy) {
        this.maxEnergy = maxEnergy;
    }
    
    /**
     * Gets the maximum energy for this organism.
     * 
     * @return Maximum energy
     */
    public int getMaxEnergy() {
        return this.maxEnergy;
    }
}