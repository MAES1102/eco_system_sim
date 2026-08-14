package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.PredationEvent;
import com.ecosystem.simulation.events.ReproductionEvent;

/**
 * Represents a predator in the ecosystem.
 * Predators hunt herbivores for food and have attack capabilities.
 * 
 * This class demonstrates the OOP principle of INHERITANCE by extending
 * Animal and adding predator-specific behaviors like hunting and attacking.
 * 
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Animal, reuses movement and speed
 * - Specialization: Adds predator-specific properties (attackPower)
 * - Method Overriding: Overrides update() for predator behavior
 * - Encapsulation: Protected attackPower with public methods
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class Predator extends Animal implements Reproducible {
    
    /**
     * Attack power of this predator.
     * Higher values make the predator more successful in hunts.
     */
    protected int attackPower;

/**
 * Target entity being hunted (for multi-step pursuit).
 * Allows predator to remember target across multiple update cycles.
 */
private Entity target;

/**
 * Steps remaining to remember current target.
 */
private int targetMemory;

/**
 * Steps remaining before this predator can reproduce again.
 * Prevents exponential predator population growth.
 */
private int reproductionCooldown;

/**
 * Steps remaining before this predator may make another kill.
 *
 * <p>Set to 10 after every successful kill.  The predator can still search,
 * chase, and attempt attacks each step — only the lethal outcome is
 * suppressed.  This mirrors a biological satiation model: a predator that
 * has just eaten does not need to (and biologically cannot) consume another
 * herbivore immediately.  The cooldown gives the herbivore population a
 * guaranteed 10-step recovery window between consecutive kills by the same
 * individual.</p>
 */
private int fedCooldown;

/**
 * Maximum age a predator can reach before dying of old age.
 */
    private static final int MAX_AGE = 140;
    
    /**
     * Constructor for Predator.
     * Initializes predator with position, energy, speed, and attack power.
     * 
     * @param x The initial x coordinate
     * @param y The initial y coordinate
     * @param energy The starting energy level
     * @param speed The movement speed
     * @param attackPower The attack power
     * @throws IllegalArgumentException if attackPower is not positive
     */
    public Predator(int x, int y, int energy, double speed, int attackPower) {
        super(x, y, energy, speed);
        
        if (attackPower <= 0) {
            throw new IllegalArgumentException("Attack power must be positive: " + attackPower);
        }
        
        this.attackPower = attackPower;
        this.target = null;
        this.targetMemory = 0;
        this.reproductionCooldown = 0;
        this.fedCooldown = 0;
    }
    
    /**
     * Implements the update behavior for predators.
     * Predators hunt, move, consume energy, and age each time step.
     * Predators can reproduce when energy is high and cooldown is over.
     * Predators die of old age when they reach MAX_AGE.
     */
    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }
        
        // Hunt and move: if a directed chase occurred, skip random movement so
        // the approach is not partially undone by the random walk in the same step.
        boolean chased = hunt();
        if (!chased) {
            move();
        }
        
        // Predator consumes energy for living and moving
        consumeEnergy(2);
        
        // Predator ages
        increaseAge();
        
        // Check if predator dies of old age
        if (getAge() >= MAX_AGE) {
            die();
            return;
        }
        
        // Tick cooldowns
        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }
        if (fedCooldown > 0) {
            fedCooldown--;
        }
        
        // Check if predator should reproduce (requires sufficient energy and no cooldown)
        if (getEnergy() > getMaxEnergy() * 0.6 && reproductionCooldown == 0) {
            reproduce();
        }
        
        // Check if predator is starving
        if (isStarving()) {
            // Starving predators might move more desperately
            // This could be expanded in a more complex simulation
        }
    }
    
    /**
     * Hunt behavior specific to predators.
     * Predators search for and attack herbivores to gain energy.
     *
     * <p>Returns {@code true} when a directed chase movement was performed so
     * that {@link #update()} can skip the subsequent random {@link #move()} call.
     * Mixing directed and random movement in the same step partially cancelled
     * the approach, making hunting far less efficient.</p>
     *
     * @return true if a chase movement was performed, false if no target was found
     */
    public boolean hunt() {
        if (world == null) {
            return false;
        }
        
        // Decrease target memory counter
        if (targetMemory > 0) {
            targetMemory--;
        }
        
        // Check if current target is still valid
        if (target != null && targetMemory > 0 && target.isAlive()) {
            // Chase existing target — directed movement performed
            chase(target);
            int distance = Math.abs(target.getX() - getX()) + Math.abs(target.getY() - getY());
            if (distance <= 2) {
                boolean success = attack(target);
                if (statistics != null) {
                    if (success) {
                        statistics.recordSuccessfulHunt();
                        if (simulationEventListener != null) {
                            simulationEventListener.onEntityEvent("hunted successfully", "Predator");
                        }
                    } else {
                        statistics.recordFailedHunt();
                    }
                }
                // Reset target after attack attempt
                target = null;
                targetMemory = 0;
            }
            return true;  // directed chase movement was performed
        }
        
        // Search for nearby herbivores within radius 10
        java.util.List<Entity> neighbors = world.getNeighbors(this, 10);
        Herbivore nearestHerbivore = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Herbivore && neighbor.isAlive()) {
                Herbivore herbivore = (Herbivore) neighbor;
                int distance = Math.abs(herbivore.getX() - getX()) + Math.abs(herbivore.getY() - getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestHerbivore = herbivore;
                }
            }
        }
        
        if (nearestHerbivore != null) {
            // Set as target and remember for 5 steps
            target = nearestHerbivore;
            targetMemory = 5;
            
            // Move toward the herbivore — directed movement performed
            chase(nearestHerbivore);
            
            // If close enough (within 2 cells), attack
            if (minDistance <= 2) {
                boolean success = attack(nearestHerbivore);
                if (statistics != null) {
                    if (success) {
                        statistics.recordSuccessfulHunt();
                        if (simulationEventListener != null) {
                            simulationEventListener.onEntityEvent("hunted successfully", "Predator");
                        }
                    } else {
                        statistics.recordFailedHunt();
                    }
                }
                // Reset target after attack attempt
                target = null;
                targetMemory = 0;
            }
            return true;  // directed chase movement was performed
        }
        
        return false;  // no prey in range — caller should perform random movement
    }
    
    /**
     * Attacks a target entity.
     * Success depends on attack power versus target's defense (if applicable).
     *
     * <p>If {@code fedCooldown > 0} the predator has eaten recently and the
     * lethal outcome is suppressed — the predator still executes the attack
     * motion but cannot make a kill.  This models biological satiation without
     * removing the hunting behaviour entirely.  On a successful kill,
     * {@code fedCooldown} is set to 10 so the predator must wait 10 steps
     * before killing again.</p>
     *
     * @param target The entity to attack
     * @return true if the attack was successful (prey killed)
     */
    public boolean attack(Entity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        // Check if target is a herbivore (predators typically hunt herbivores)
        if (target instanceof Herbivore) {
            Herbivore herbivore = (Herbivore) target;

            // Satiation gate: recently-fed predator cannot make another kill yet.
            // The predator still approaches and attempts — only the kill is blocked.
            if (fedCooldown > 0) {
                return false;
            }

            // Probabilistic attack: success chance = attackPower / (attackPower + defensePower)
            // e.g. 7 / (7+5) = 58% success rate — produces realistic failed hunts
            double successChance = (double) this.attackPower / (this.attackPower + herbivore.getDefensePower());
            if (Math.random() < successChance) {
                herbivore.die();
                gainEnergy(45);
                fedCooldown = 10;
                // Fire a PredationEvent so the DES layer records this kill
                if (eventQueue != null) {
                    eventQueue.enqueue(new PredationEvent(simulationTime, this, herbivore, 45));
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Chases a target entity.
     * Moves the predator toward the target's position.
     * This method is more generic to reduce coupling with specific entity types.
     * 
     * @param target The entity to chase
     */
    public void chase(Entity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        
        // Calculate direction to target
        int deltaX = target.getX() - getX();
        int deltaY = target.getY() - getY();
        
        // Normalize and move toward target
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
     * Sets the initial satiation cooldown, used to stagger predator hunt
     * attempts at simulation start so kills are distributed across multiple
     * steps rather than clustered in the first update cycle.
     *
     * @param cooldown initial fedCooldown value (≥ 0)
     */
    public void setFedCooldown(int cooldown) {
        this.fedCooldown = Math.max(0, cooldown);
    }

    /**
     * Gets the attack power of this predator.
     * 
     * @return The attack power
     */
    public int getAttackPower() {
        return this.attackPower;
    }
    
    /**
     * Sets the attack power of this predator.
     * 
     * @param attackPower The new attack power
     */
    public void setAttackPower(int attackPower) {
        this.attackPower = attackPower;
    }
    
    /**
     * Attempts to reproduce this predator.
     * If successful, creates a new predator nearby.
     * Reproduction consumes significant energy.
     * Sets reproduction cooldown to prevent exponential growth.
     */
    public void reproduce() {
        if (world == null) {
            return;  // World reference not set yet
        }
        
        // Hard cap: no new predators beyond carrying capacity
        if (world.countAliveByType("Predator") >= 5) {
            reproductionCooldown = 10;
            return;
        }
        
        int reproductionCost = getEnergy() / 2;
        consumeEnergy(reproductionCost);
        
        // Create a new predator at a nearby valid position
        int newX = getX() + (int)(Math.random() * 5) - 2;  // -2 to +2 offset
        int newY = getY() + (int)(Math.random() * 5) - 2;
        
        // Clamp to world boundaries
        newX = Math.max(0, Math.min(49, newX));
        newY = Math.max(0, Math.min(49, newY));
        
        Predator newPredator = new Predator(newX, newY, 75, 2.0, 7);
        newPredator.setWorld(world);
        newPredator.setStatistics(statistics);
        newPredator.setSimulationEventListener(simulationEventListener);
        newPredator.setEventQueue(eventQueue);
        newPredator.setSimulationTime(simulationTime);

        world.addEntity(newPredator);

        if (statistics != null) {
            statistics.recordBirth("Predator");
        }

        if (simulationEventListener != null) {
            simulationEventListener.onEntityEvent("reproduced", "Predator");
        }

        // Fire discrete events for both the reproduction action and the new birth
        if (eventQueue != null) {
            eventQueue.enqueue(new ReproductionEvent(simulationTime, this, newPredator));
        }
        
        // Set reproduction cooldown (40 steps before can reproduce again)
        this.reproductionCooldown = 40;
    }
}