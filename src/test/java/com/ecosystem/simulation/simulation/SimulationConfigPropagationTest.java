package com.ecosystem.simulation.simulation;

import com.ecosystem.simulation.entities.EntityFactory;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.events.ReproductionEvent;
import com.ecosystem.simulation.events.TestSchedulingContext;
import com.ecosystem.simulation.statistics.Statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that {@code config/simulation.properties} values genuinely propagate
 * end to end: {@code config file -> SimulationConfig -> EntityFactory ->
 * entity/environment state}, and that the <em>same</em> configured defaults
 * apply to reproduced offspring, not just the initial population.
 */
class SimulationConfigPropagationTest {

    @TempDir
    Path tempDir;

    private SimulationConfig loadCustom(String propertiesText) throws IOException {
        Path file = tempDir.resolve("simulation.properties");
        Files.writeString(file, propertiesText);
        return SimulationConfig.loadOrDefault(file, "/simulation.properties");
    }

    @Test
    void nonDefaultPredatorEnergy_propagatesToCreatedEntity() throws IOException {
        SimulationConfig config = loadCustom("predator.energy=999\n");
        assertEquals(999, config.predatorEnergy());

        World world = new World(50, 50);
        Statistics stats = new Statistics();
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats, new com.ecosystem.simulation.rules.RuleVocabulary(), config);
        EntityFactory factory = ctx.entityFactory;

        Predator p = factory.createPredator(5, 5);
        assertEquals(999, p.getEnergy(), "EntityFactory must use the configured, non-default predator energy");
    }

    @Test
    void nonDefaultVisionRangeAndMaxAge_propagate() throws IOException {
        SimulationConfig config = loadCustom("herbivore.visionRange=42\nherbivore.maxAge=7\n");
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats, new com.ecosystem.simulation.rules.RuleVocabulary(), config);

        Herbivore h = ctx.entityFactory.createHerbivore(5, 5);

        assertEquals(42, h.getVisionRange());
        assertEquals(7, h.getMaxAge());
    }

    @Test
    void nonDefaultWorldSize_propagatesToWorldAndMovementClamping() throws IOException {
        SimulationConfig config = loadCustom("world.width=10\nworld.height=8\n");
        SimulationEngine engine = new SimulationEngine(config);

        assertEquals(10, engine.getWorld().getWidth());
        assertEquals(8, engine.getWorld().getHeight());

        Predator p = engine.getEntityFactory().createPredator(0, 0);
        p.moveTo(9999, 9999);
        assertEquals(9, p.getX(), "movement must clamp to the configured (non-default) world width, not the old hardcoded 49");
        assertEquals(7, p.getY(), "movement must clamp to the configured (non-default) world height");
    }

    @Test
    void nonDefaultTemperature_propagatesToEnvironment() throws IOException {
        SimulationConfig config = loadCustom("environment.temperature=-3.5\n");
        SimulationEngine engine = new SimulationEngine(config);

        assertEquals(-3.5, engine.getWorld().getEnvironment().getTemperature(), 1e-9);
    }

    @Test
    void nonDefaultFoodRegenRate_propagatesToRegeneration() throws IOException {
        SimulationConfig config = loadCustom("environment.foodLevel=10\nenvironment.maxFoodLevel=200\nenvironment.foodRegenRate=37\n");
        SimulationEngine engine = new SimulationEngine(config);

        int before = engine.getWorld().getEnvironment().getFoodLevel();
        engine.getWorld().getEnvironment().regenerateFood();
        int after = engine.getWorld().getEnvironment().getFoodLevel();

        assertEquals(37, after - before, "food regeneration must use the configured (non-default) rate");
    }

    @Test
    void offspring_useConfiguredSpeciesDefaults_notParentInheritance() throws IOException {
        SimulationConfig config = loadCustom("herbivore.energy=17\nherbivore.defensePower=3\n");
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats, new com.ecosystem.simulation.rules.RuleVocabulary(), config);

        // Parent has very different stats from the configured defaults.
        Herbivore parent = new Herbivore(5, 5, 80, 3.0, 40);
        parent.setWorld(world);
        parent.setStatistics(stats);
        parent.setSchedulingContext(ctx);
        world.addEntity(parent);

        Herbivore offspring = (Herbivore) ctx.entityFactory.createOffspringNear(parent);

        assertEquals(17, offspring.getEnergy(), "offspring must use the configured default, not the parent's own energy");
        assertEquals(3, offspring.getDefensePower(), "offspring must use the configured default, not the parent's own defensePower");
    }

    @Test
    void reproductionEvent_usesFactoryConfiguredDefaults_endToEnd() throws IOException {
        SimulationConfig config = loadCustom("plant.energy=61\n");
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats, new com.ecosystem.simulation.rules.RuleVocabulary(), config);

        Plant parent = ctx.entityFactory.createPlant(5, 5);
        world.addEntity(parent);

        new ReproductionEvent(0, parent).execute(ctx);

        long newPlantsWithConfiguredEnergy = world.getEntities().stream()
                .filter(e -> e instanceof Plant && e != parent)
                .map(e -> (Plant) e)
                .filter(p -> p.getEnergy() == 61)
                .count();

        assertTrue(newPlantsWithConfiguredEnergy >= 1, "the offspring created by ReproductionEvent must carry the configured plant energy default");
    }
}
