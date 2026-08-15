package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.PredationEvent;
import com.ecosystem.simulation.events.ReproductionEvent;

/**
 * Represents a predator in the ecosystem. Predators hunt herbivores for food
 * and have attack capabilities.
 *
 * Key OOP Principles Demonstrated:
 * - Inheritance: Extends Animal, reuses movement and speed
 * - Method Overriding: Overrides update() for predator behavior
 * - Encapsulation: Protected attackPower with public methods
 */
public class Predator extends Animal implements Reproducible {

    protected int attackPower;

    private Entity target;
    private int targetMemory;
    private int reproductionCooldown;

    /**
     * Steps remaining before this predator may make another kill (satiation).
     * Set by {@link PredationEvent#execute}, the sole owner of predator-side
     * predation effects — {@link #attack} only decides success/failure.
     */
    private int fedCooldown;

    /** Maximum age before dying of old age. Configurable via EntityFactory/SimulationConfig; default matches the original hardcoded constant. */
    private int maxAge = 140;

    /** Metabolic energy cost applied once per activity event. Configurable; default matches the original hardcoded literal. */
    private int energyConsumptionPerTick = 2;

    /** Cooldown length assigned after a successful reproduction. Configurable; default matches the original hardcoded literal. */
    private int reproductionCooldownPeriod = 40;

    /** Population cap enforced before scheduling a ReproductionEvent. Configurable; default matches the original hardcoded literal. */
    private int populationCap = 5;

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
        this.visionRange = 10;
    }

    @Override
    public void update() {
        if (!isAlive()) {
            return;
        }

        boolean chased = hunt();
        if (!chased) {
            move();
        }

        consumeEnergy(energyConsumptionPerTick);
        if (!isAlive() || isPendingRemoval()) {
            return; // starvation may have just scheduled removal
        }

        increaseAge();

        if (getAge() >= maxAge) {
            scheduleRemoval("old age");
            return;
        }

        if (reproductionCooldown > 0) {
            reproductionCooldown--;
        }
        if (fedCooldown > 0) {
            fedCooldown--;
        }

        if (getEnergy() > getMaxEnergy() * 0.6 && reproductionCooldown == 0) {
            reproduce();
        }
    }

    /**
     * Hunt behavior: searches for and attacks nearby herbivores. Only decides
     * whether an attack succeeds and, on success, schedules a
     * {@link PredationEvent} — it never mutates energy/cooldown/statistics
     * itself; that belongs to {@link PredationEvent#execute}.
     *
     * @return true if a directed chase movement was performed
     */
    public boolean hunt() {
        if (world == null) {
            return false;
        }

        if (targetMemory > 0) {
            targetMemory--;
        }

        if (target != null && targetMemory > 0 && target.isAlive() && !target.isPendingRemoval()) {
            chase(target);
            int distance = Math.abs(target.getX() - getX()) + Math.abs(target.getY() - getY());
            if (distance <= 2) {
                attack(target);
                target = null;
                targetMemory = 0;
            }
            return true;
        }

        java.util.List<Entity> neighbors = world.getNeighbors(this, visionRange);
        Herbivore nearestHerbivore = null;
        int minDistance = Integer.MAX_VALUE;

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Herbivore && neighbor.isAlive() && !neighbor.isPendingRemoval()) {
                Herbivore herbivore = (Herbivore) neighbor;
                int distance = Math.abs(herbivore.getX() - getX()) + Math.abs(herbivore.getY() - getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestHerbivore = herbivore;
                }
            }
        }

        if (nearestHerbivore != null) {
            target = nearestHerbivore;
            targetMemory = 5;
            chase(nearestHerbivore);

            if (minDistance <= 2) {
                attack(nearestHerbivore);
                target = null;
                targetMemory = 0;
            }
            return true;
        }

        return false;
    }

    /**
     * Attacks a target entity: decides success/failure only. On success,
     * schedules a {@link PredationEvent} which applies all predator-side
     * effects (energy gain, satiation cooldown, hunt statistic) and the
     * prey's death. On failure, records the failed-hunt statistic directly —
     * a failed attack has no cross-entity mutation, so no event is needed.
     *
     * @return true if the attack succeeded (a PredationEvent was scheduled)
     */
    public boolean attack(Entity target) {
        if (target == null || !target.isAlive() || target.isPendingRemoval()) {
            return false;
        }

        if (target instanceof Herbivore herbivore) {
            if (fedCooldown > 0) {
                return false;
            }

            double successChance = (double) this.attackPower / (this.attackPower + herbivore.getDefensePower());
            if (Math.random() < successChance) {
                if (schedulingContext != null) {
                    schedulingContext.schedule(new PredationEvent(schedulingContext.getClock(), this, herbivore, 45));
                }
                return true;
            } else if (statistics != null) {
                statistics.recordFailedHunt();
            }
        }

        return false;
    }

    public void chase(Entity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        int deltaX = target.getX() - getX();
        int deltaY = target.getY() - getY();
        int moveDistance = (int) getSpeed();

        int newX = getX();
        int newY = getY();

        if (deltaX > 0) newX += moveDistance;
        else if (deltaX < 0) newX -= moveDistance;

        if (deltaY > 0) newY += moveDistance;
        else if (deltaY < 0) newY -= moveDistance;

        moveTo(newX, newY);
    }

    public void setFedCooldown(int cooldown) {
        this.fedCooldown = Math.max(0, cooldown);
    }

    public int getAttackPower() { return this.attackPower; }
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }

    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public int getMaxAge() { return this.maxAge; }

    public void setEnergyConsumptionPerTick(int amount) { this.energyConsumptionPerTick = amount; }

    public void setReproductionCooldownPeriod(int period) { this.reproductionCooldownPeriod = period; }

    public void setPopulationCap(int cap) { this.populationCap = cap; }
    public int getPopulationCap() { return this.populationCap; }

    /**
     * Decides whether to reproduce (cooldown/cap/energy-cost checks, all this
     * predator's own state) and, if so, schedules a {@link ReproductionEvent} —
     * which is the sole owner of the new offspring's creation.
     */
    @Override
    public void reproduce() {
        if (world == null || schedulingContext == null) {
            return;
        }

        if (world.countAliveByType("Predator") >= populationCap) {
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
