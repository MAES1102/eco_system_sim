package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Animal;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.events.EntityActivityEvent;
import com.ecosystem.simulation.events.ReproductionEvent;
import com.ecosystem.simulation.events.TestSchedulingContext;
import com.ecosystem.simulation.simulation.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for {@link Rule#executeActions} and {@link RuleEngine#evaluate},
 * proving that rule actions actually run through the new parser/vocabulary/evaluator
 * pipeline and, for domain commands that cause discrete events (death, reproduction),
 * through the DES event-scheduling path.
 */
class RuleActionTest {

    private static final RuleVocabulary VOCAB = new RuleVocabulary();

    // ─────────────────────────────────────────────────────────────────────────
    // die
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dieRule_killsEntity_viaExecuteActions() throws RuleParseException {
        // No SchedulingContext wired -- scheduleRemoval() degrades to die() directly,
        // the documented testability fallback for bare entities outside a running engine.
        Predator predator = new Predator(5, 5, 50, 2.0, 7);
        Rule rule = new Rule("Kill", "Predator", "energy < 100", "die", VOCAB);

        assertTrue(predator.isAlive(), "precondition: predator must start alive");
        rule.executeActions(predator);

        assertFalse(predator.isAlive(), "die action must mark the entity dead");
    }

    @Test
    void dieRule_killsEntity_endToEndThroughEngine() throws RuleParseException {
        Predator predator = new Predator(5, 5, 50, 2.0, 7);
        RuleEngine engine = new RuleEngine(VOCAB);
        engine.addRule(new Rule("StarvingPredator", "Predator", "energy < 100", "die", VOCAB));

        engine.evaluate(predator);

        assertFalse(predator.isAlive(), "die rule must kill a matching entity whose condition is met");
    }

    /**
     * Death via a real {@code DeathEvent}, not the bare-entity fallback: proves the
     * unified death pathway actually fires through the DES queue when a
     * SchedulingContext is wired, exactly as it is in the running simulation.
     */
    @Test
    void dieRule_schedulesDeathEvent_whenSchedulingContextWired() throws RuleParseException {
        World world = new World(50, 50);
        Predator predator = new Predator(5, 5, 50, 2.0, 7);
        predator.setWorld(world);
        world.addEntity(predator);
        var stats = new com.ecosystem.simulation.statistics.Statistics();
        predator.setStatistics(stats);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        predator.setSchedulingContext(ctx);

        Rule rule = new Rule("Kill", "Predator", "energy < 100", "die", VOCAB);
        rule.executeActions(predator);

        assertTrue(predator.isAlive(), "predator must still be alive -- only pendingRemoval, until DeathEvent executes");
        assertTrue(predator.isPendingRemoval(), "die command must mark pendingRemoval immediately");
        assertEquals(1, ctx.scheduled.size(), "exactly one DeathEvent must be scheduled");

        ctx.scheduled.get(0).execute(ctx);

        assertFalse(predator.isAlive(), "predator must be dead after DeathEvent executes");
        assertEquals(1, stats.getPredatorDeaths(), "death must be recorded exactly once");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // move
    // ─────────────────────────────────────────────────────────────────────────

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
    void moveRule_invokesAnimalMove() throws RuleParseException {
        MoveSpyPredator spy = new MoveSpyPredator();
        Rule rule = new Rule("Wander", "Predator", "energy > 0", "move", VOCAB);

        rule.executeActions(spy);

        assertTrue(spy.moveCalled, "move action must call Animal.move() on an animal entity");
    }

    /**
     * Semantic change from the original design: {@code move} on a {@code Plant}
     * (which is not {@code Movable}) is now rejected at <b>construction time</b>
     * by {@link RuleVocabulary#requireCommand}, rather than silently doing
     * nothing at runtime. This is a statically detectable target/command
     * mismatch, exactly the case the validation policy is meant to catch.
     */
    @Test
    void moveRule_onPlant_rejectedAtValidation() {
        RuleParseException ex = assertThrows(RuleParseException.class,
                () -> new Rule("Wander", "Plant", "energy > 0", "move", VOCAB));
        assertTrue(ex.getMessage().contains("move"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reproduce -- now flows through ReproductionEvent + EntityFactory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void reproduceRule_schedulesReproductionEvent_forPlant() throws RuleParseException {
        World world = new World(50, 50);
        var stats = new com.ecosystem.simulation.statistics.Statistics();
        Plant plant = new Plant(10, 10, 90, 2.0);
        plant.setWorld(world);
        plant.setStatistics(stats);
        world.addEntity(plant);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        plant.setSchedulingContext(ctx);

        Rule rule = new Rule("PlantGrowth", "Plant", "energy > 80", "reproduce", VOCAB);
        rule.executeActions(plant);

        assertEquals(1, ctx.scheduled.size());
        assertTrue(ctx.scheduled.get(0) instanceof ReproductionEvent);

        int before = world.countAliveByType("Plant");
        ctx.scheduled.get(0).execute(ctx);
        int after = world.countAliveByType("Plant");

        assertEquals(before + 1, after, "reproduce action must add one Plant offspring once the ReproductionEvent executes");
        assertEquals(1, stats.getPlantBirths(), "birth must be recorded exactly once");
    }

    @Test
    void reproduceRule_endToEndThroughEngine_forHerbivore() throws RuleParseException {
        World world = new World(50, 50);
        var stats = new com.ecosystem.simulation.statistics.Statistics();
        Herbivore herbivore = new Herbivore(20, 20, 80, 1.5, 5);
        herbivore.setWorld(world);
        herbivore.setStatistics(stats);
        world.addEntity(herbivore);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        herbivore.setSchedulingContext(ctx);

        RuleEngine engine = new RuleEngine(VOCAB);
        engine.addRule(new Rule("HerbBreed", "Herbivore", "energy > 50", "reproduce", VOCAB));
        engine.evaluate(herbivore, world, stats, ctx);

        assertEquals(1, ctx.scheduled.size());
        ctx.scheduled.get(0).execute(ctx);

        assertEquals(2, world.countAliveByType("Herbivore"),
                "reproduce action must add one Herbivore offspring end-to-end");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compound conditions and generic mutation (new grammar capability)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void compoundCondition_andOperator_bothMustHold() throws RuleParseException {
        Predator predator = new Predator(0, 0, 15, 2.0, 7);
        Rule rule = new Rule("ColdAndWeak", "Predator", "energy < 20 AND age >= 0", "energy -= 5", VOCAB);

        assertTrue(rule.evaluateCondition(predator));
        rule.executeActions(predator);
        assertEquals(10, predator.getEnergy());
    }

    @Test
    void genericMutation_attackPowerMultiply() throws RuleParseException {
        Predator predator = new Predator(0, 0, 50, 2.0, 10);
        Rule rule = new Rule("Buff", "Predator", "energy > 0", "attackPower *= 2", VOCAB);

        rule.executeActions(predator);

        assertEquals(20, predator.getAttackPower());
    }
}
