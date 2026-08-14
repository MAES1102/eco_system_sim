package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.ReproductionEvent;

/**
 * Represents a herbivore in the ecosystem.
 * Herbivores eat plants and have defense capabilities against predators.
 * 
 * This class demonstrates the OOP principle of INHERITANCE by extending
 * Animal and adding herbivore-specific behaviors like grazing and defending.
 * 
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Animal, reuses movement and speed
 * - Specialization: Adds herbivore-specific properties (defensePower)
 * - Method Overriding: Overrides update() for herbivore behavior
 * - Encapsulation: Protected defensePower with public methods
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class Herbivore extends Animal implements Reproducible {
    
    /**
     * Defense power of this herbivore.
     * Higher values make the herbivore more resistant to predator attacks.
     */
    protected int defensePower;
    
    /**
     * Steps remaining before this herbivore can reproduce again.
     * Prevents exponential herbivore population growth.
     */
    private int reproductionCooldown;

    /**
     * Steps remaining before this herbivore can eat again.
     * Prevents overconsumption that would crash the plant population.
     */
    private int grazeCooldown;
    
    /**
     * Maximum age a herbivore can reach before dying of old age.
     */
    private static final int MAX_AGE = 80;
    
    /**
     * Constructor for Herbivore.
     * Initializes herbivore with position, energy, speed, and defense power.
     * 
     * @param x The initial x coordinate
     * @param y The initial y coordinate
     * @param energy The starting energy level
     * @param speed The movement speed
     * @param defensePower The defense power
     * @throws IllegalArgumentException if defensePower is not positive
     */
    public Herbivore(int x, int y, int energy, double speed, int defensePower) {
        super(x, y, energy, speed);
        
        if (defensePower <= 0) {
            throw new IllegalArgumentException("Defense power must be positive: " + defensePower);
        }
        
        this.defensePower = defensePower;
        this.reproductionCooldown = 0;
        this.grazeCooldown = 0;
    }
    
    /**
     * Implements the update behavior for herbivores.
     * Herbivores graze on plants, move, consume energy, and age each time step.
     * Herbivores can reproduce when energy is high and cooldown is over.
     * Herbivores die of old age when they reach MAX_AGE.
     */
    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }
        
        // Decrease grazing cooldown before attempting to graze
        if (grazeCooldown > 0) {
            grazeCooldown--;
        }
        
        // Graze and move: if directed movement toward a plant occurred, skip random
        // movement so the approach is not partially undone in the same step.
        boolean directed = graze();
        if (!directed) {
            move();
        }
        
        // Herbivore consumes energy for living and moving
        consumeEnergy(1);  // Herbivores consume less energy than predators
        
        // Herbivore ages
        increaseAge();
        
        // Check if herbivore dies of old age
        if (getAge() >= MAX_AGE) {
            die();
            return;
        }
        
        // Decrease reproduction cooldown
        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }
        
        // Check if herbivore should reproduce (requires sufficient energy and no cooldown)
        if (getEnergy() > getMaxEnergy() * 0.55 && reproductionCooldown == 0) {
            reproduce();
        }
        
        // Check if herbivore is starving
        if (isStarving()) {
            // Starving herbivores might move more to find food
            // This could be expanded in a more complex simulation
        }
    }
    
    /**
     * Graze behavior specific to herbivores.
     * Herbivores eat plants to gain energy.
     *
     * <p>Returns {@code true} when a directed move toward a plant was performed
     * so that {@link #update()} can skip the subsequent random {@link #move()} call.
     * Moving directed then immediately random in the same step partially undoes the
     * approach, reducing the rate at which herbivores reach plants.</p>
     *
     * @return true if directed movement toward a plant was performed, false if no
     *         plant was found in search range
     */
    public boolean graze() {
        if (world == null) {
            return false;
        }
        
        // Search for nearby plants within radius 18 (larger radius improves plant detection)
        java.util.List<Entity> neighbors = world.getNeighbors(this, 18);
        Plant nearestPlant = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Plant && neighbor.isAlive()) {
                Plant plant = (Plant) neighbor;
                int distance = Math.abs(plant.getX() - getX()) + Math.abs(plant.getY() - getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPlant = plant;
                }
            }
        }
        
        if (nearestPlant != null) {
            // Move toward the plant — directed movement performed
            int deltaX = nearestPlant.getX() - getX();
            int deltaY = nearestPlant.getY() - getY();
            int moveDistance = (int) getSpeed();
            
            int newX = getX();
            int newY = getY();
            
            if (deltaX > 0) newX += Math.min(moveDistance, deltaX);
            else if (deltaX < 0) newX -= Math.min(moveDistance, -deltaX);
            
            if (deltaY > 0) newY += Math.min(moveDistance, deltaY);
            else if (deltaY < 0) newY -= Math.min(moveDistance, -deltaY);
            
            moveTo(newX, newY);
            
            // If close enough (within 2 cells) and not in grazing cooldown, consume the plant
            if (minDistance <= 2 && grazeCooldown == 0) {
                gainEnergy(20);
                nearestPlant.die();
                world.removeEntity(nearestPlant);
                grazeCooldown = 5;  // Must wait 5 steps before eating again
                if (statistics != null) {
                    statistics.recordPlantConsumed();
                }
                if (simulationEventListener != null) {
                    simulationEventListener.onEntityEvent("consumed plant", "Herbivore");
                }
            }
            return true;  // directed movement toward plant was performed
        }
        
        return false;  // no plant in range — caller should perform random movement
    }
    
    /**
     * Defends against a predator attack.
     * Success depends on defense power versus predator's attack power.
     * 
     * @param attacker The predator attacking this herbivore
     * @return true if the defense was successful
     */
    public boolean defend(Predator attacker) {
        if (attacker == null) {
            return false;
        }
        
        // Simple defense logic: higher defense power increases survival chance
        int survivalChance = this.defensePower * 10;
        if (Math.random() * 100 < survivalChance) {
            // Successfully defended - lose some energy but survive
            consumeEnergy(5);
            return true;
        }
        
        // Defense failed - herbivore dies
        return false;
    }
    
    /**
     * Flees from a nearby predator.
     * Moves the herbivore away from the predator's position.
     * 
     * @param predator The predator to flee from
     */
    public void flee(Predator predator) {
        if (predator == null || !predator.isAlive()) {
            return;
        }
        
        // Calculate direction away from predator
        int deltaX = getX() - predator.getX();
        int deltaY = getY() - predator.getY();
        
        // Normalize and move away from predator
        int moveDistance = (int) getSpeed();
        
        int newX = getX();
        int newY = getY();
        
        if (deltaX > 0) newX += moveDistance;
        else if (deltaX < 0) newX -= moveDistance;
        
        if (deltaY > 0) newY += moveDistance;
        else if (deltaY < 0) newY -= moveDistance;
        
        moveTo(newX, newY);
    }
    
    /**
     * Gets the defense power of this herbivore.
     * 
     * @return The defense power
     */
    public int getDefensePower() {
        return this.defensePower;
    }
    
    /**
     * Sets the defense power of this herbivore.
     * 
     * @param defensePower The new defense power
     */
    public void setDefensePower(int defensePower) {
        this.defensePower = defensePower;
    }
    
    /**
     * Attempts to reproduce this herbivore.
     * If successful, creates a new herbivore nearby.
     * Reproduction consumes significant energy.
     * Sets reproduction cooldown to prevent exponential growth.
     */
    public void reproduce() {
        if (world == null) {
            return;  // World reference not set yet
        }
        
        // Hard cap: no new herbivores beyond carrying capacity
        if (world.countAliveByType("Herbivore") >= 22) {
            reproductionCooldown = 10;
            return;
        }
        
        int reproductionCost = getEnergy() / 2;
        consumeEnergy(reproductionCost);
        
        // Create a new herbivore at a nearby valid position
        int newX = getX() + (int)(Math.random() * 5) - 2;  // -2 to +2 offset
        int newY = getY() + (int)(Math.random() * 5) - 2;
        
        // Clamp to world boundaries
        newX = Math.max(0, Math.min(49, newX));
        newY = Math.max(0, Math.min(49, newY));
        
        Herbivore newHerbivore = new Herbivore(newX, newY, 50, 1.5, 12);
        newHerbivore.setWorld(world);
        newHerbivore.setStatistics(statistics);
        newHerbivore.setSimulationEventListener(simulationEventListener);
        newHerbivore.setEventQueue(eventQueue);
        newHerbivore.setSimulationTime(simulationTime);

        world.addEntity(newHerbivore);

        if (statistics != null) {
            statistics.recordBirth("Herbivore");
        }

        if (simulationEventListener != null) {
            simulationEventListener.onEntityEvent("reproduced", "Herbivore");
        }

        // Fire discrete events for the reproduction and the new birth
        if (eventQueue != null) {
            eventQueue.enqueue(new ReproductionEvent(simulationTime, this, newHerbivore));
        }
        
        // Set reproduction cooldown before can reproduce again
        this.reproductionCooldown = 20;
    }
}