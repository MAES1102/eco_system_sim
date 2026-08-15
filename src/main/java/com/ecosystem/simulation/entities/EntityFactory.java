package com.ecosystem.simulation.entities;

import com.ecosystem.simulation.events.SchedulingContext;
import com.ecosystem.simulation.simulation.SimulationConfig;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * The single, centralized place initial entities and offspring are created and
 * fully wired — eliminating the duplicated hardcoded constructor literals that
 * used to appear once in {@code MainWindow}/{@code HeadlessRunner} and again in
 * each species' own {@code reproduce()} method.
 *
 * <p>Every species-default value (energy, speed, attack/defense, vision range,
 * max age, reproduction cooldown, energy consumption, population cap) is
 * sourced from {@link SimulationConfig}, so both the initial population and
 * every reproduced offspring reflect the same configuration — offspring use
 * configured species defaults, not genetic inheritance from the parent, which
 * is the simpler policy chosen for this project.</p>
 */
public class EntityFactory {

    private final SimulationConfig config;
    private final World world;
    private final Statistics statistics;
    private final SchedulingContext schedulingContext;

    public EntityFactory(SimulationConfig config, World world, Statistics statistics,
                          SchedulingContext schedulingContext) {
        this.config = config;
        this.world = world;
        this.statistics = statistics;
        this.schedulingContext = schedulingContext;
    }

    public Predator createPredator(int x, int y) {
        Predator p = new Predator(x, y, config.predatorEnergy(), config.predatorSpeed(), config.predatorAttackPower());
        p.setVisionRange(config.predatorVisionRange());
        p.setMaxAge(config.predatorMaxAge());
        p.setReproductionCooldownPeriod(config.predatorReproductionCooldown());
        p.setEnergyConsumptionPerTick(config.predatorEnergyConsumption());
        p.setPopulationCap(config.predatorCap());
        wire(p);
        return p;
    }

    public Herbivore createHerbivore(int x, int y) {
        Herbivore h = new Herbivore(x, y, config.herbivoreEnergy(), config.herbivoreSpeed(), config.herbivoreDefensePower());
        h.setVisionRange(config.herbivoreVisionRange());
        h.setMaxAge(config.herbivoreMaxAge());
        h.setReproductionCooldownPeriod(config.herbivoreReproductionCooldown());
        h.setEnergyConsumptionPerTick(config.herbivoreEnergyConsumption());
        h.setPopulationCap(config.herbivoreCap());
        wire(h);
        return h;
    }

    public Plant createPlant(int x, int y) {
        Plant plant = new Plant(x, y, config.plantEnergy(), config.plantGrowthRate());
        plant.setMaxAge(config.plantMaxAge());
        plant.setReproductionCooldownPeriod(config.plantReproductionCooldown());
        plant.setPopulationCap(config.plantCap());
        wire(plant);
        return plant;
    }

    /**
     * Creates one offspring of the same species as {@code parent}, positioned
     * near the parent (small random offset, clamped to the world's configured
     * bounds), using configured species defaults.
     */
    public Entity createOffspringNear(Entity parent) {
        int maxX = world.getWidth() - 1;
        int maxY = world.getHeight() - 1;
        int newX = Math.max(0, Math.min(maxX, parent.getX() + offset()));
        int newY = Math.max(0, Math.min(maxY, parent.getY() + offset()));

        if (parent instanceof Predator) {
            return createPredator(newX, newY);
        }
        if (parent instanceof Herbivore) {
            return createHerbivore(newX, newY);
        }
        if (parent instanceof Plant) {
            return createPlant(newX, newY);
        }
        return null;
    }

    private void wire(Entity e) {
        e.setWorld(world);
        e.setStatistics(statistics);
        e.setSchedulingContext(schedulingContext);
        if (schedulingContext != null) {
            // Looked up dynamically (not captured at EntityFactory construction time) so
            // offspring created after MainWindow calls setSimulationEventListener still
            // pick up the listener correctly.
            e.setSimulationEventListener(schedulingContext.getSimulationEventListener());
            e.setSimulationTime(schedulingContext.getClock());
        }
    }

    private static int offset() {
        return (int) (Math.random() * 5) - 2;
    }
}
