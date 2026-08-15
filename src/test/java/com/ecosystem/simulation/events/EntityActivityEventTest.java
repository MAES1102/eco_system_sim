package com.ecosystem.simulation.events;

import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.rules.Rule;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the recurring {@link EntityActivityEvent}: self-rescheduling while
 * alive, rule evaluation happening inside its execution, and the chain
 * terminating (no further scheduling) once an entity is dead or pending removal.
 */
class EntityActivityEventTest {

    @Test
    void livingEntity_reschedulesItsOwnSuccessor() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Plant plant = new Plant(5, 5, 10, 2.0);
        plant.setWorld(world);
        plant.setStatistics(stats);
        world.addEntity(plant);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        ctx.setClock(1);
        plant.setSchedulingContext(ctx);

        new EntityActivityEvent(1, plant).execute(ctx);

        assertEquals(1, ctx.scheduled.size(), "exactly one follow-up EntityActivityEvent must be scheduled");
        EntityActivityEvent next = (EntityActivityEvent) ctx.scheduled.get(0);
        assertEquals(2, next.getScheduledTime(), "the successor must be scheduled exactly one tick later than the clock at execution time");
    }

    @Test
    void pendingRemovalEntity_doesNotReschedule() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Plant plant = new Plant(5, 5, 10, 2.0);
        plant.setWorld(world);
        plant.setStatistics(stats);
        world.addEntity(plant);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        plant.setSchedulingContext(ctx);
        plant.scheduleRemoval("rule");
        ctx.scheduled.clear(); // discard the DeathEvent scheduling from above, isolate this event's behavior

        new EntityActivityEvent(1, plant).execute(ctx);

        assertTrue(ctx.scheduled.isEmpty(), "a pendingRemoval entity's activity chain must terminate -- no further scheduling of any kind");
    }

    @Test
    void deadEntity_activityEvent_isNoOp() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        Plant plant = new Plant(5, 5, 10, 2.0);
        plant.setWorld(world);
        plant.setStatistics(stats);
        world.addEntity(plant);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        plant.setSchedulingContext(ctx);
        plant.die();

        new EntityActivityEvent(1, plant).execute(ctx);

        assertTrue(ctx.scheduled.isEmpty());
    }

    @Test
    void ruleEvaluation_happensInsideActivityEvent() throws com.ecosystem.simulation.rules.RuleParseException {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        // Low energy so update()'s own reproduction side effect (triggered above 50% of
        // maxEnergy) cannot fire and confound the energy value the rule below observes;
        // the condition itself is age-based, which update() always sets to exactly 1 here.
        Plant plant = new Plant(5, 5, 10, 2.0);
        plant.setWorld(world);
        plant.setStatistics(stats);
        world.addEntity(plant);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);
        ctx.setClock(1);
        plant.setSchedulingContext(ctx);
        ctx.ruleEngine.addRule(new Rule("KillAnyActivePlant", "Plant", "age >= 1", "die", ctx.vocabulary));

        new EntityActivityEvent(1, plant).execute(ctx);

        assertTrue(plant.isPendingRemoval(), "a rule evaluated inside the activity event must be able to take effect (schedule removal) the same tick");
    }

    @Test
    void movementEvent_isEmitted_whenPositionChanges_butIsObservationalOnly() {
        World world = new World(50, 50);
        Statistics stats = new Statistics();
        // A plant never moves, so use a deterministic position-changing stand-in:
        // directly verify MovementEvent's own no-op execute() contract instead.
        MovementEvent event = new MovementEvent(1, new Plant(5, 5, 50, 2.0), 5, 5, 6, 6);
        TestSchedulingContext ctx = new TestSchedulingContext(world, stats);

        event.execute(ctx); // must not throw, must not schedule anything, must not mutate anything

        assertTrue(ctx.scheduled.isEmpty(), "MovementEvent must not cause any further mutation or scheduling -- it is a record only");
        assertFalse(event.getDescription().isBlank());
    }
}
