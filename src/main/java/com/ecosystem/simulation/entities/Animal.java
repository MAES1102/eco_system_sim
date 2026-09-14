package com.ecosystem.simulation.entities;

/**
 * Abstract base class for all mobile animals in the ecosystem.
 * Extends Organism with movement capabilities and speed.
 * 
 * This class demonstrates the OOP principle of INHERITANCE by extending
 * Organism and adding ONLY behavior common to ALL animals.
 * 
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Organism, reuses energy and age management
 * - Abstraction: Abstract class, defines common animal behaviors
 * - Information hiding: Private speed/visionRange fields, reachable only through accessors
 * - Liskov Substitution: Only contains methods applicable to all animals
 */
public abstract class Animal extends Organism implements Movable {
    
    /**
     * Movement speed of this animal.
     * Higher values allow the animal to move further in one time step.
     */
    private double speed;

    /**
     * Search radius (in grid cells) used when looking for prey/food. Was
     * previously a hardcoded literal inside {@code hunt()}/{@code graze()};
     * promoted to a configurable field so it can be sourced from
     * {@code simulation.properties} via {@code EntityFactory}. Defaults match
     * the original hardcoded values (10 for predators, 18 for herbivores) — see
     * each subclass's constructor.
     */
    private int visionRange = 10;

    /**
     * Constructor for Animal.
     * Initializes animal with position, energy, and speed.
     *
     * @param x The initial x coordinate
     * @param y The initial y coordinate
     * @param energy The starting energy level
     * @param speed The movement speed
     * @throws IllegalArgumentException if speed is not positive
     */
    public Animal(int x, int y, int energy, double speed) {
        super(x, y, energy);

        if (speed <= 0) {
            throw new IllegalArgumentException("Speed must be positive: " + speed);
        }

        this.speed = speed;
    }

    public int getVisionRange() { return this.visionRange; }
    public void setVisionRange(int visionRange) { this.visionRange = visionRange; }
    
    /**
     * Moves this animal in a random direction.
     * The distance moved is based on the animal's speed.
     * Subclasses can override this for specific movement behaviors.
     * 
     * This method is concrete because ALL animals can move, even if
     * they move differently (predators chase prey, herbivores wander).
     */
    public void move() {
        // Simple random movement - subclasses can override for smarter behavior
        int moveDistance = (int) speed;
        if (moveDistance < 1) {
            moveDistance = 1;  // Minimum movement
        }
        
        // Random direction: -1, 0, or 1 for both x and y
        int deltaX = (int)(Math.random() * 3) - 1;  // -1, 0, or 1
        int deltaY = (int)(Math.random() * 3) - 1;  // -1, 0, or 1
        
        int newX = getX() + deltaX * moveDistance;
        int newY = getY() + deltaY * moveDistance;
        
        moveTo(newX, newY);
    }
    
    /**
     * Gets the movement speed of this animal.
     * 
     * @return The speed value
     */
    public double getSpeed() {
        return this.speed;
    }
    
    /**
     * Sets the movement speed of this animal.
     * 
     * @param speed The new speed value
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
}