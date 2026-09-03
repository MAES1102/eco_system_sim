package com.ecosystem.simulation.simulation;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.environment.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the simulation world that contains all entities.
 * Manages spatial organization and entity storage.
 * 
 * This class demonstrates the OOP principle of COMPOSITION by containing
 * an Environment object to manage environmental conditions.
 * 
 * Key OOP Principles Demonstrated:
 * - Composition: World contains Environment (has-a relationship)
 * - Encapsulation: Private entity list with public methods
 * - Single Responsibility: Manages entity storage and spatial queries
 */
public class World {
    
    /**
     * List of all entities in the world.
     */
    private List<Entity> entities;
    
    /**
     * Environment object managing obstacles and food levels.
     * Composition relationship - World owns Environment.
     */
    private Environment environment;
    
    /**
     * Width of the world (number of columns).
     */
    private int width;
    
    /**
     * Height of the world (number of rows).
     */
    private int height;
    
    /**
     * Constructor for World.
     * Initializes world with given dimensions and creates environment.
     * 
     * @param width The width of the world
     * @param height The height of the world
     */
    public World(int width, int height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();
        this.environment = new Environment(width, height);
    }
    
    /**
     * Adds an entity to the world.
     * 
     * @param entity The entity to add
     */
    public void addEntity(Entity entity) {
        if (entity != null) {
            this.entities.add(entity);
        }
    }
    
    /**
     * Removes an entity from the world.
     * 
     * @param entity The entity to remove
     */
    public void removeEntity(Entity entity) {
        if (entity != null) {
            this.entities.remove(entity);
        }
    }
    
    /**
     * Gets all entities in the world.
     * 
     * @return List of all entities
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(this.entities);  // Return copy to preserve encapsulation
    }
    
    /**
     * Gets entities at specific coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return List of entities at the specified position
     */
    public List<Entity> getEntitiesAt(int x, int y) {
        List<Entity> entitiesAtPosition = new ArrayList<>();
        for (Entity entity : this.entities) {
            if (entity.getX() == x && entity.getY() == y) {
                entitiesAtPosition.add(entity);
            }
        }
        return entitiesAtPosition;
    }
    
    /**
     * Gets neighboring entities within a given radius.
     * Uses simple distance calculation (no diagonal distance).
     * 
     * @param entity The center entity
     * @param radius The search radius
     * @return List of neighboring entities
     */
    public List<Entity> getNeighbors(Entity entity, int radius) {
        List<Entity> neighbors = new ArrayList<>();
        if (entity == null) {
            return neighbors;
        }
        
        int centerX = entity.getX();
        int centerY = entity.getY();
        
        for (Entity other : this.entities) {
            if (other == entity) {
                continue;  // Skip the entity itself
            }
            
            int dx = Math.abs(other.getX() - centerX);
            int dy = Math.abs(other.getY() - centerY);
            
            // Simple Manhattan distance (no diagonal for simplicity)
            if (dx <= radius && dy <= radius) {
                neighbors.add(other);
            }
        }
        
        return neighbors;
    }
    
    /**
     * Gets the environment object.
     * 
     * @return The environment
     */
    public Environment getEnvironment() {
        return this.environment;
    }
    
    /**
     * Gets the width of the world.
     * 
     * @return The width
     */
    public int getWidth() {
        return this.width;
    }
    
    /**
     * Gets the height of the world.
     * 
     * @return The height
     */
    public int getHeight() {
        return this.height;
    }
    
    /**
     * Removes all dead entities from the world.
     * Should be called periodically to clean up the entity list.
     * 
     * @return Number of entities removed
     */
    public int removeDeadEntities() {
        int removedCount = 0;
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : this.entities) {
            if (!entity.isAlive()) {
                toRemove.add(entity);
            }
        }
        
        for (Entity entity : toRemove) {
            this.entities.remove(entity);
            removedCount++;
        }
        
        return removedCount;
    }
    
    /**
     * Gets the count of alive entities in the world.
     * 
     * @return Number of alive entities
     */
    public int getAliveEntityCount() {
        int count = 0;
        for (Entity entity : this.entities) {
            if (entity.isAlive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts alive entities of a specific type by simple class name.
     * Provides accurate live population count directly from world state,
     * independent of Statistics counters (which track events, not live counts).
     *
     * @param typeName Simple class name, e.g. "Predator", "Herbivore", "Plant"
     * @return Number of alive entities with that class name
     */
    public int countAliveByType(String typeName) {
        int count = 0;
        for (Entity entity : this.entities) {
            if (entity.isAlive() && entity.getClass().getSimpleName().equals(typeName)) {
                count++;
            }
        }
        return count;
    }
}