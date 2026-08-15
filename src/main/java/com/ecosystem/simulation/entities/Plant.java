package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.ReproductionEvent;

/**
 * Represents a plant in the ecosystem. Plants are stationary organisms that
 * grow and can reproduce. Concrete (not abstract): plants have complete,
 * self-contained behavior without needing further specialization.
 */
public class Plant extends Organism implements Reproducible {

    private double growthRate;
    private int reproductionCooldown;

    private int maxAge = 100;
    private int reproductionCooldownPeriod = 12;
    private int populationCap = 120;

    public Plant(int x, int y, int energy, double growthRate) {
        super(x, y, energy);

        if (growthRate <= 0) {
            throw new IllegalArgumentException("Growth rate must be positive: " + growthRate);
        }

        this.growthRate = growthRate;
        this.reproductionCooldown = 0;
    }

    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }

        grow();

        increaseAge();

        if (getAge() >= maxAge) {
            scheduleRemoval("old age");
            return;
        }

        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }

        if (getEnergy() > getMaxEnergy() * 0.5 && reproductionCooldown == 0) {
            reproduce();
        }
    }

    public void grow() {
        int energyGain = (int) growthRate;
        gainEnergy(energyGain);
    }

    @Override
    public void reproduce() {
        if (world == null || schedulingContext == null) {
            return;
        }

        if (world.countAliveByType("Plant") >= populationCap) {
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

    public double getGrowthRate() { return this.growthRate; }
    public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }

    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public int getMaxAge() { return this.maxAge; }

    public void setReproductionCooldownPeriod(int period) { this.reproductionCooldownPeriod = period; }

    public void setPopulationCap(int cap) { this.populationCap = cap; }
    public int getPopulationCap() { return this.populationCap; }
}
