package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests lazy invalidation of stale scheduled events (an entity dies between
 * scheduling and execution) and the {@code pendingRemoval} duplicate-death
 * guard.
 */
class LazyInvalidationTest {

    @Test
    void deathEvent_onAlreadyDeadEntity_isHarmlessNoOp() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        predator.setWorld(world);
        predator.setStatistics(stats);
        world.addEntity(predator);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        predator.setSchedulingContext(ctx);

        predator.die(); // simulate it already died via some other path
        DeathEvent stale = new DeathEvent(0, predator, "starvation");

        stale.execute(ctx);

        assertEquals(0, stats.getPredatorDeaths(), "a stale DeathEvent on an already-dead entity must not double-record the death");
    }

    @Test
    void predationEvent_preyAlreadyDead_isLazilyInvalidated() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        Herbivore prey = new Herbivore(1, 1, 30, 1.5, 5);
        predator.setWorld(world);
        predator.setStatistics(stats);
        prey.setWorld(world);
        prey.setStatistics(stats);
        world.addEntity(predator);
        world.addEntity(prey);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        predator.setSchedulingContext(ctx);
        prey.setSchedulingContext(ctx);

        prey.die(); // prey died from something else before the predation event fires
        PredationEvent stale = new PredationEvent(0, predator, prey, 45);
        int energyBefore = predator.getEnergy();

        stale.execute(ctx);

        assertEquals(energyBefore, predator.getEnergy(), "a stale PredationEvent must not grant energy for a kill that no longer happened");
        assertEquals(0, stats.getSuccessfulHunts());
    }

    @Test
    void reproductionEvent_parentDiedBeforeExecution_isLazilyInvalidated() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Herbivore parent = new Herbivore(5, 5, 80, 1.5, 5);
        parent.setWorld(world);
        parent.setStatistics(stats);
        world.addEntity(parent);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        parent.setSchedulingContext(ctx);

        parent.die();
        ReproductionEvent stale = new ReproductionEvent(0, parent);
        int before = world.countAliveByType("Herbivore");

        stale.execute(ctx);

        assertEquals(before, world.countAliveByType("Herbivore"), "a stale ReproductionEvent must not create offspring for a dead parent");
        assertEquals(0, stats.getHerbivoreBirths());
    }

    // ── pendingRemoval duplicate-death prevention ────────────────────────────

    @Test
    void scheduleRemoval_isIdempotent_onlySchedulesOneDeathEvent() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        predator.setWorld(world);
        predator.setStatistics(stats);
        world.addEntity(predator);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        predator.setSchedulingContext(ctx);

        predator.scheduleRemoval("starvation");
        predator.scheduleRemoval("old age"); // a second, different cause -- must be ignored
        predator.scheduleRemoval("predation");

        assertEquals(1, ctx.scheduled.size(), "at most one DeathEvent may ever be scheduled for the same entity");
        assertTrue(predator.isPendingRemoval());
        assertTrue(predator.isAlive(), "the entity remains alive until the DeathEvent actually executes");
    }

    @Test
    void pendingRemoval_entity_excludedFromBeingTargeted() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        Herbivore prey = new Herbivore(1, 1, 30, 1.5, 5);
        predator.setWorld(world);
        prey.setWorld(world);
        prey.setStatistics(stats);
        world.addEntity(predator);
        world.addEntity(prey);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        predator.setSchedulingContext(ctx);
        prey.setSchedulingContext(ctx);

        prey.scheduleRemoval("rule");
        assertTrue(prey.isAlive(), "pendingRemoval entity is still technically alive");

        boolean attacked = predator.attack(prey);

        assertFalse(attacked, "a pendingRemoval entity must not be attackable even though isAlive() is still true");
    }
}
