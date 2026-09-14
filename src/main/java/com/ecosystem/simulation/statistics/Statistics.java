package com.ecosystem.simulation.statistics;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks and reports statistics about the simulation.
 * Records population counts, births, deaths, and interaction events.
 * 
 * This class demonstrates the OOP principle of ENCAPSULATION by
 * hiding the data structures used for tracking and providing
 * a simple interface for recording and reporting statistics.
 * 
 * Key OOP Principles Demonstrated:
 * - Encapsulation: Private data structures with public methods
 * - Single Responsibility: Only tracks and reports statistics
 */
public class Statistics {
    
    /**
     * Map tracking population count by entity type.
     * Key: entity type name (e.g., "Predator", "Herbivore", "Plant")
     * Value: count of entities of that type
     */
    private Map<String, Integer> population;
    
    /**
     * Total number of births recorded in the simulation.
     */
    private int birthCount;
    
    /**
     * Total number of deaths recorded in the simulation.
     */
    private int deathCount;

    // --- Per-species birth counters ---
    private int predatorBirths;
    private int herbivoreBirths;
    private int plantBirths;

    // --- Per-species death counters ---
    private int predatorDeaths;
    private int herbivoreDeaths;
    private int plantDeaths;

    // --- Interaction counters ---
    /** Number of predator attacks that resulted in a kill. */
    private int successfulHunts;

    /** Number of predator attacks that were attempted but failed (no kill). */
    private int failedHunts;

    /** Number of plants killed by herbivore grazing. */
    private int plantsConsumed;
    
    /**
     * Constructor for Statistics.
     * Initializes tracking structures.
     */
    public Statistics() {
        this.population = new HashMap<>();
        this.birthCount = 0;
        this.deathCount = 0;
        this.predatorBirths = 0;
        this.herbivoreBirths = 0;
        this.plantBirths = 0;
        this.predatorDeaths = 0;
        this.herbivoreDeaths = 0;
        this.plantDeaths = 0;
        this.successfulHunts = 0;
        this.failedHunts = 0;
        this.plantsConsumed = 0;
    }
    
    /**
     * Records a birth event for a specific entity type.
     *
     * <p><b>Overload 1 of 2</b> — records one birth for the named type.
     * Demonstrates <em>overloading polymorphism</em>: same method name,
     * different parameter signature.</p>
     *
     * @param entityType the simple class name of the entity that was born
     */
    public void recordBirth(String entityType) {
        recordBirth(entityType, 1);
    }

    /**
     * Records multiple births at once for a specific entity type.
     *
     * <p><b>Overload 2 of 2</b> — useful for batch initialisation (e.g. placing
     * an initial population at simulation start).  Delegates to the per-entity
     * counter logic, keeping the tracking consistent.</p>
     *
     * @param entityType the simple class name of the entity that was born
     * @param count      how many individuals were born (must be &ge; 1)
     * @throws IllegalArgumentException if {@code count} is less than 1
     */
    public void recordBirth(String entityType, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Birth count must be >= 1, got: " + count);
        }
        for (int i = 0; i < count; i++) {
            this.birthCount++;
            incrementPopulation(entityType);
            adjustSpeciesCounter(entityType, true);
        }
    }

    /**
     * Records a death event for a specific entity type.
     * Called exclusively by SimulationEngine.removeDeadEntities() to avoid double-counting.
     *
     * @param entityType The type of entity that died
     */
    public void recordDeath(String entityType) {
        this.deathCount++;
        decrementPopulation(entityType);
        adjustSpeciesCounter(entityType, false);
    }

    /**
     * The one place this class enumerates the known species, used by both
     * {@link #recordBirth} and {@link #recordDeath} instead of each repeating
     * its own {@code Predator}/{@code Herbivore}/{@code Plant} switch.
     *
     * @param birth {@code true} to bump that species' birth counter, {@code false} for its death counter
     */
    private void adjustSpeciesCounter(String entityType, boolean birth) {
        switch (entityType) {
            case "Predator":  if (birth) this.predatorBirths++;  else this.predatorDeaths++;  break;
            case "Herbivore": if (birth) this.herbivoreBirths++; else this.herbivoreDeaths++; break;
            case "Plant":     if (birth) this.plantBirths++;     else this.plantDeaths++;     break;
        }
    }

    /**
     * Records a successful predator hunt (attack that killed a herbivore).
     */
    public void recordSuccessfulHunt() {
        this.successfulHunts++;
    }

    /**
     * Records a failed predator hunt attempt (attack in range but no kill).
     */
    public void recordFailedHunt() {
        this.failedHunts++;
    }

    /**
     * Records a plant being consumed by a herbivore during grazing.
     * This is separate from plantDeaths (plants also die of old age).
     */
    public void recordPlantConsumed() {
        this.plantsConsumed++;
    }
    
    /**
     * Records the initial population when an entity is added to the world.
     * 
     * @param entityType The type of entity added
     */
    public void recordInitialEntity(String entityType) {
        incrementPopulation(entityType);
    }
    
    /**
     * Increments the population count for a specific entity type.
     * 
     * @param entityType The entity type
     */
    private void incrementPopulation(String entityType) {
        int currentCount = population.getOrDefault(entityType, 0);
        population.put(entityType, currentCount + 1);
    }
    
    /**
     * Decrements the population count for a specific entity type.
     * 
     * @param entityType The entity type
     */
    private void decrementPopulation(String entityType) {
        int currentCount = population.getOrDefault(entityType, 0);
        if (currentCount > 0) {
            population.put(entityType, currentCount - 1);
        }
    }
    
    /**
     * Gets the current population count for a specific entity type.
     * 
     * @param entityType The entity type
     * @return Population count for that type
     */
    public int getPopulation(String entityType) {
        return population.getOrDefault(entityType, 0);
    }
    
    /**
     * Gets the total population count across all entity types.
     * 
     * @return Total population
     */
    public int getTotalPopulation() {
        int total = 0;
        for (int count : population.values()) {
            total += count;
        }
        return total;
    }
    
    /**
     * Gets the total number of births recorded.
     * 
     * @return Birth count
     */
    public int getBirthCount() {
        return this.birthCount;
    }
    
    /**
     * Gets the total number of deaths recorded.
     * 
     * @return Death count
     */
    public int getDeathCount() {
        return this.deathCount;
    }

    // --- Getters for specific counters ---

    public int getPredatorBirths()  { return predatorBirths; }
    public int getHerbivoreBirths() { return herbivoreBirths; }
    public int getPlantBirths()     { return plantBirths; }

    public int getPredatorDeaths()  { return predatorDeaths; }
    public int getHerbivoreDeaths() { return herbivoreDeaths; }
    public int getPlantDeaths()     { return plantDeaths; }

    public int getSuccessfulHunts() { return successfulHunts; }
    public int getFailedHunts()     { return failedHunts; }
    public int getPlantsConsumed()  { return plantsConsumed; }
    
    /**
     * Generates a comprehensive report of simulation statistics.
     * 
     * @return Formatted string containing all statistics
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Simulation Statistics ===\n");
        report.append("Total Population: ").append(getTotalPopulation()).append("\n");
        report.append("Total Births: ").append(this.birthCount).append("\n");
        report.append("Total Deaths: ").append(this.deathCount).append("\n");
        report.append("\nPopulation by Type:\n");
        
        for (Map.Entry<String, Integer> entry : population.entrySet()) {
            report.append("  - ").append(entry.getKey())
                  .append(": ").append(entry.getValue()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Resets all statistics to initial state.
     * Useful for starting a new simulation.
     */
    public void reset() {
        this.population.clear();
        this.birthCount = 0;
        this.deathCount = 0;
        this.predatorBirths = 0;
        this.herbivoreBirths = 0;
        this.plantBirths = 0;
        this.predatorDeaths = 0;
        this.herbivoreDeaths = 0;
        this.plantDeaths = 0;
        this.successfulHunts = 0;
        this.failedHunts = 0;
        this.plantsConsumed = 0;
    }
}
