package com.ecosystem.simulation.entities;

/**
 * Represents a plant in the ecosystem.
 * Plants are stationary organisms that grow and can reproduce.
 * 
 * This class demonstrates the OOP principle of INHERITANCE by extending
 * Organism and implementing plant-specific behaviors like growth.
 * 
 * Unlike Animal, Plant is a concrete class (not abstract) because plants
 * have complete, self-contained behavior without needing further specialization.
 * 
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Organism, reuses energy and age management
 * - Concrete Implementation: Provides complete plant behavior
 * - Encapsulation: Private growth rate with public methods
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class Plant extends Organism implements Reproducible {
    
    /**
     * Rate at which this plant grows per time step.
     * Higher values mean faster energy gain from sunlight.
     */
    private double growthRate;
    
    /**
     * Steps remaining before this plant can reproduce again.
     * Prevents exponential plant population growth.
     */
    private int reproductionCooldown;
    
    /**
     * Maximum age a plant can reach before dying of old age.
     * Prevents plants from living indefinitely.
     */
    private static final int MAX_AGE = 100;
    
    /**
     * Constructor for Plant.
     * Initializes plant with position, energy, and growth rate.
     * 
     * @param x The initial x coordinate
     * @param y The initial y coordinate
     * @param energy The starting energy level
     * @param growthRate The growth rate (energy gained per time step)
     * @throws IllegalArgumentException if growthRate is not positive
     */
    public Plant(int x, int y, int energy, double growthRate) {
        super(x, y, energy);
        
        if (growthRate <= 0) {
            throw new IllegalArgumentException("Growth rate must be positive: " + growthRate);
        }
        
        this.growthRate = growthRate;
        this.reproductionCooldown = 0;
    }
    
    /**
     * Implements the update behavior for plants.
     * Plants grow each time step, gaining energy based on growth rate.
     * Plants age each time step.
     * If energy is sufficient and cooldown is over, plants may reproduce.
     * Plants die of old age when they reach MAX_AGE.
     */
    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }
        
        // Plant gains energy from sunlight (growth)
        grow();
        
        // Plant ages
        increaseAge();
        
        // Check if plant dies of old age
        if (getAge() >= MAX_AGE) {
            die();
            return;
        }
        
        // Decrease reproduction cooldown
        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }
        
        // Check if plant should reproduce (requires sufficient energy and no cooldown)
        if (getEnergy() > getMaxEnergy() * 0.5 && reproductionCooldown == 0) {
            reproduce();
        }
    }
    
    /**
     * Makes the plant grow by gaining energy.
     * The amount gained is based on the growth rate.
     */
    public void grow() {
        int energyGain = (int) growthRate;
        gainEnergy(energyGain);
    }
    
    /**
     * Attempts to reproduce this plant.
     * If successful, creates a new plant nearby.
     * Reproduction consumes significant energy.
     * Sets reproduction cooldown to prevent exponential growth.
     */
    public void reproduce() {
        if (world == null) {
            return;  // World reference not set yet
        }
        
        // Hard cap: no new plants when world is at carrying capacity
        if (world.countAliveByType("Plant") >= 120) {
            reproductionCooldown = 10;  // Try again in 10 steps
            return;
        }
        
        int reproductionCost = getEnergy() / 2;
        consumeEnergy(reproductionCost);
        
        // Create a new plant at a nearby valid position
        int newX = getX() + (int)(Math.random() * 5) - 2;  // -2 to +2 offset
        int newY = getY() + (int)(Math.random() * 5) - 2;
        
        // Clamp to world boundaries
        newX = Math.max(0, Math.min(49, newX));
        newY = Math.max(0, Math.min(49, newY));
        
        Plant newPlant = new Plant(newX, newY, 50, 2.0);  // Child plant starts with high energy for fast first reproduction
        newPlant.setWorld(world);
        newPlant.setStatistics(statistics);
        newPlant.setSimulationEventListener(simulationEventListener);
        
        world.addEntity(newPlant);
        
        if (statistics != null) {
            statistics.recordBirth("Plant");
        }
        
        if (simulationEventListener != null) {
            simulationEventListener.onEntityEvent("reproduced", "Plant");
        }
        
        // Set reproduction cooldown before can reproduce again
        this.reproductionCooldown = 12;
    }
    
    /**
     * Gets the growth rate of this plant.
     * 
     * @return The growth rate
     */
    public double getGrowthRate() {
        return this.growthRate;
    }
    
    /**
     * Sets the growth rate of this plant.
     * 
     * @param growthRate The new growth rate
     */
    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }
}