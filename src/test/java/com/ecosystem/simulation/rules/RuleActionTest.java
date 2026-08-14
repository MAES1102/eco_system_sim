package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.simulation.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@link Rule#executeAction} and {@link RuleEngine#evaluate},
 * proving that the actions exposed by the Rule Builder ({@code die}, {@code move},
 * {@code reproduce}) are actually executed by the rule engine.
 */
class RuleActionTest {

    // ─────────────────────────────────────────────────────────────────────────
    // die
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dieRule_killsEntity_viaExecuteAction() {
        Predator predator = new Predator(5, 5, 50, 2.0, 7);
        Rule rule = new Rule("Kill", "Predator", "energy < 100", "die");

        assertTrue(predator.isAlive(), "precondition: predator must start alive");
        rule.executeAction(predator);

        assertFalse(predator.isAlive(), "die action must mark the entity dead");
    }

    @Test
    void dieRule_killsEntity_endToEndThroughEngine() {
        Predator predator = new Predator(5, 5, 50, 2.0, 7);
        RuleEngine engine = new RuleEngine();
        engine.addRule(new Rule("StarvingPredator", "Predator", "energy < 100", "die"));

        // matches(Predator) → evaluateCondition(50 < 100 = true) → executeAction("die")
        engine.evaluate(predator);

        assertFalse(predator.isAlive(),
                "die rule must kill a matching entity whose condition is met");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // move
    // ─────────────────────────────────────────────────────────────────────────

    /** Spy that records whether {@link Animal#move()} was invoked. */
    private static class MoveSpyPredator extends Predator {
        boolean moveCalled = false;

        MoveSpyPredator() {
            super(10, 10, 50, 2.0, 7);
        }

        @Override
        public void move() {
            moveCalled = true;
        }
    }

    @Test
    void moveRule_invokesAnimalMove() {
        MoveSpyPredator spy = new MoveSpyPredator();
        Rule rule = new Rule("Wander", "Predator", "energy > 0", "move");

        rule.executeAction(spy);

        assertTrue(spy.moveCalled, "move action must call Animal.move() on an animal entity");
    }

    @Test
    void moveRule_onPlant_doesNothing() {
        // A Plant is not an Animal, so a move action must be a safe no-op.
        Plant plant = new Plant(7, 7, 50, 2.0);
        int originalX = plant.getX();
        int originalY = plant.getY();
        Rule rule = new Rule("Wander", "Plant", "energy > 0", "move");

        rule.executeAction(plant);

        assertTrue(plant.isAlive(), "move on a plant must not kill it");
        assertEquals(originalX, plant.getX(), "plant must not move");
        assertEquals(originalY, plant.getY(), "plant must not move");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reproduce
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void reproduceRule_addsPlantOffspring_viaExecuteAction() {
        World world = new World(50, 50);
        Plant plant = new Plant(10, 10, 90, 2.0);
        plant.setWorld(world);
        world.addEntity(plant);

        int before = world.countAliveByType("Plant");
        Rule rule = new Rule("PlantGrowth", "Plant", "energy > 80", "reproduce");
        rule.executeAction(plant);
        int after = world.countAliveByType("Plant");

        assertEquals(before + 1, after, "reproduce action must add one Plant offspring to the world");
    }

    @Test
    void reproduceRule_addsPlantOffspring_endToEndThroughEngine() {
        World world = new World(50, 50);
        Plant plant = new Plant(10, 10, 90, 2.0);
        plant.setWorld(world);
        world.addEntity(plant);

        RuleEngine engine = new RuleEngine();
        engine.addRule(new Rule("PlantGrowth", "Plant", "energy > 80", "reproduce"));

        // matches(Plant) → evaluateCondition(90 > 80 = true) → executeAction("reproduce")
        engine.evaluate(plant);

        assertEquals(2, world.countAliveByType("Plant"),
                "PlantGrowth reproduce rule must produce a second living plant end-to-end");
    }

    @Test
    void reproduceRule_addsHerbivoreOffspring() {
        World world = new World(50, 50);
        Herbivore herbivore = new Herbivore(20, 20, 80, 1.5, 5);
        herbivore.setWorld(world);
        world.addEntity(herbivore);

        Rule rule = new Rule("HerbBreed", "Herbivore", "energy > 50", "reproduce");
        rule.executeAction(herbivore);

        assertEquals(2, world.countAliveByType("Herbivore"),
                "reproduce action must add one Herbivore offspring");
    }

    @Test
    void reproduceRule_addsPredatorOffspring() {
        World world = new World(50, 50);
        Predator predator = new Predator(30, 30, 80, 2.0, 7);
        predator.setWorld(world);
        world.addEntity(predator);

        Rule rule = new Rule("PredBreed", "Predator", "energy > 50", "reproduce");
        rule.executeAction(predator);

        assertEquals(2, world.countAliveByType("Predator"),
                "reproduce action must add one Predator offspring");
    }
}
