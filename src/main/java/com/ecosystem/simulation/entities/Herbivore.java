package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.ReproductionEvent;

/**
 * Represents a herbivore in the ecosystem. Herbivores eat plants and have
 * defense capabilities against predators.
 */
public class Herbivore extends Animal implements Reproducible {

    protected int defensePower;
    private int reproductionCooldown;
    private int grazeCooldown;

    private int maxAge = 80;
    private int energyConsumptionPerTick = 1;
    private int reproductionCooldownPeriod = 20;
    private int populationCap = 22;

    public Herbivore(int x, int y, int energy, double speed, int defensePower) {
        super(x, y, energy, speed);

        if (defensePower <= 0) {
            throw new IllegalArgumentException("Defense power must be positive: " + defensePower);
        }

        this.defensePower = defensePower;
        this.reproductionCooldown = 0;
        this.grazeCooldown = 0;
        this.visionRange = 18;
    }

    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }

        if (grazeCooldown > 0) {
            grazeCooldown--;
        }

        boolean directed = graze();
        if (!directed) {
            move();
        }

        consumeEnergy(energyConsumptionPerTick);
        if (!isAlive() || isPendingRemoval()) {
            return;
        }

        increaseAge();

        if (getAge() >= maxAge) {
            scheduleRemoval("old age");
            return;
        }

        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }

        if (getEnergy() > getMaxEnergy() * 0.55 && reproductionCooldown == 0) {
            reproduce();
        }
    }

    /**
     * Grazes on the nearest plant. The herbivore's own energy gain is this
     * activity event's own state (legitimate to mutate directly); the plant's
     * death is requested via {@link Entity#scheduleRemoval(String)} (cause
     * {@code "grazed"}) rather than calling {@code die()}/{@code removeEntity()}
     * directly — this is the fix for the pre-existing "phantom plant" statistics
     * bug, where grazing bypassed the single death-recording path.
     */
    public boolean graze() {
        if (world == null) {
            return false;
        }

        java.util.List<Entity> neighbors = world.getNeighbors(this, visionRange);
        Plant nearestPlant = null;
        int minDistance = Integer.MAX_VALUE;

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Plant && neighbor.isAlive() && !neighbor.isPendingRemoval()) {
                Plant plant = (Plant) neighbor;
                int distance = Math.abs(plant.getX() - getX()) + Math.abs(plant.getY() - getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPlant = plant;
                }
            }
        }

        if (nearestPlant != null) {
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

            if (minDistance <= 2 && grazeCooldown == 0) {
                gainEnergy(20);
                grazeCooldown = 5;
                if (simulationEventListener != null) {
                    simulationEventListener.onEntityEvent("consumed plant", "Herbivore");
                }
                nearestPlant.scheduleRemoval("grazed");
            }
            return true;
        }

        return false;
    }

    public boolean defend(Predator attacker) {
        if (attacker == null) {
            return false;
        }
        int survivalChance = this.defensePower * 10;
        if (Math.random() * 100 < survivalChance) {
            consumeEnergy(5);
            return true;
        }
        return false;
    }

    public void flee(Predator predator) {
        if (predator == null || !predator.isAlive()) {
            return;
        }

        int deltaX = getX() - predator.getX();
        int deltaY = getY() - predator.getY();
        int moveDistance = (int) getSpeed();

        int newX = getX();
        int newY = getY();

        if (deltaX > 0) newX += moveDistance;
        else if (deltaX < 0) newX -= moveDistance;

        if (deltaY > 0) newY += moveDistance;
        else if (deltaY < 0) newY -= moveDistance;

        moveTo(newX, newY);
    }

    public int getDefensePower() { return this.defensePower; }
    public void setDefensePower(int defensePower) { this.defensePower = defensePower; }

    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public int getMaxAge() { return this.maxAge; }

    public void setEnergyConsumptionPerTick(int amount) { this.energyConsumptionPerTick = amount; }

    public void setReproductionCooldownPeriod(int period) { this.reproductionCooldownPeriod = period; }

    public void setPopulationCap(int cap) { this.populationCap = cap; }
    public int getPopulationCap() { return this.populationCap; }

    @Override
    public void reproduce() {
        if (world == null || schedulingContext == null) {
            return;
        }

        if (world.countAliveByType("Herbivore") >= populationCap) {
            reproductionCooldown = 10;
            return;
        }

        int reproductionCost = getEnergy() / 2;
        consumeEnergy(reproductionCost);
        if (!isAlive() || isPendingRemoval()) {
            return;
        }

        schedulingContext.schedule(new ReproductionEvent(schedulingContext.getClock(), this));
        this.reproductionCooldown = reproductionCooldownPeriod;
    }
}
