package com.ecosystem.simulation.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for core entity invariants.
 *
 * <p>Each test targets a single, deterministic behaviour in the entity layer
 * that does not require the full simulation stack (no World, no GUI, no
 * Statistics needed).</p>
 *
 * <p>Tests covered:</p>
 * <ol>
 *   <li><b>Organism energy → death</b> — {@link Organism#consumeEnergy(int)}
 *       must mark the organism as dead and clamp energy to 0 when the amount
 *       consumed equals or exceeds current energy.</li>
 *   <li><b>Predator energy consumption per step</b> — one call to
 *       {@link Predator#update()} must reduce energy by exactly 3 (the
 *       documented metabolic cost for predators).</li>
 *   <li><b>Plant growth</b> — {@link Plant#grow()} must increase energy by
 *       {@code (int) growthRate} each call, capped at {@code maxEnergy}.</li>
 * </ol>
 */
class SimulationEntitiesTest {

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 1 — Organism energy death invariant
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When exactly all remaining energy is consumed the organism must die and
     * energy must be clamped to 0 (not negative).
     *
     * <p>This test verifies the invariant documented in
     * {@link Organism#consumeEnergy}: "If energy reaches 0 or below, the
     * organism dies."  A Herbivore is used as a concrete Organism.</p>
     */
    @Test
    void organism_diesWhenEnergyDepletedExactly() {
        Herbivore herbivore = new Herbivore(0, 0, 5, 1.5, 5);

        assertTrue(herbivore.isAlive(),  "Herbivore must start alive");
        assertEquals(5, herbivore.getEnergy(), "Starting energy must be 5");

        herbivore.consumeEnergy(5);   // energy: 5 → 0

        assertFalse(herbivore.isAlive(), "Herbivore must be dead after energy reaches 0");
        assertEquals(0, herbivore.getEnergy(), "Energy must be clamped at 0, not negative");
    }

    /**
     * When more energy is consumed than is available the result is the same:
     * organism dies and energy is clamped to 0 (not a negative value).
     */
    @Test
    void organism_diesWhenEnergyOverdepleted() {
        Herbivore herbivore = new Herbivore(0, 0, 3, 1.5, 5);

        herbivore.consumeEnergy(100); // far exceeds current energy of 3

        assertFalse(herbivore.isAlive(), "Herbivore must be dead when overdepleted");
        assertEquals(0, herbivore.getEnergy(), "Energy must be 0 (clamped), not negative");
    }

    /**
     * Consuming less energy than available must not kill the organism.
     * Boundary check: one unit below fatal threshold.
     */
    @Test
    void organism_survivesWhenEnergyAboveZero() {
        Herbivore herbivore = new Herbivore(0, 0, 10, 1.5, 5);

        herbivore.consumeEnergy(9);   // energy: 10 → 1

        assertTrue(herbivore.isAlive(), "Herbivore with 1 energy remaining must still be alive");
        assertEquals(1, herbivore.getEnergy());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 2 — Predator energy consumption per update step
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Each call to {@link Predator#update()} must consume exactly 2 energy
     * (the metabolic cost: {@code consumeEnergy(2)}).
     *
     * <p>The predator is constructed without a World reference, so
     * {@link Predator#hunt()} is called but returns immediately at the
     * {@code world == null} guard — no energy change from hunting.
     * {@link Animal#move()} changes position but not energy. Only
     * {@code consumeEnergy(2)} affects the energy field, making the result
     * fully deterministic regardless of the random move.</p>
     *
     * <p>Precondition: starting energy (50) must be below the reproduction
     * threshold {@code maxEnergy * 0.6 = 60} so that {@link Predator#reproduce}
     * is not triggered.</p>
     */
    @Test
    void predator_consumesExactlyTwoEnergyPerUpdate() {
        Predator predator = new Predator(0, 0, 50, 2.0, 7);
        // hunt() is called but returns early (world == null) -- no energy change from hunting.
        // move() changes position only.
        // consumeEnergy(2) is the only energy-modifying call.

        predator.update();

        assertEquals(48, predator.getEnergy(),
                "Predator must consume exactly 2 energy per update step (50 − 2 = 48)");
        assertTrue(predator.isAlive(),
                "Predator with 48 energy must still be alive after one update");
    }

    /**
     * Verifies that the 2-energy cost accumulates correctly across multiple steps.
     * After N steps the predator's energy should be startEnergy − (2 × N),
     * provided no reproduction is triggered.
     *
     * <p>The hunt cooldown does not affect energy: whether hunt() is called or
     * skipped each step, only {@code consumeEnergy(2)} moves the energy field.</p>
     */
    @Test
    void predator_energyDecreasesCumulativelyOverMultipleSteps() {
        Predator predator = new Predator(0, 0, 50, 2.0, 7);

        for (int step = 0; step < 5; step++) {
            predator.update();
        }

        // 50 − (2 × 5) = 40
        assertEquals(40, predator.getEnergy(),
                "After 5 steps predator energy must be 50 − 10 = 40");
        assertTrue(predator.isAlive());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST 3 — Plant growth
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@link Plant#grow()} must increase energy by {@code (int) growthRate}
     * per call.  With growthRate = 3.0 and starting energy = 10, one call
     * must yield energy = 13.
     */
    @Test
    void plant_gainsEnergyFromGrow() {
        Plant plant = new Plant(0, 0, 10, 3.0);

        plant.grow();

        assertEquals(13, plant.getEnergy(),
                "Plant energy must increase by (int)growthRate = 3 after one grow() call");
    }

    /**
     * {@link Plant#grow()} must call {@link Organism#gainEnergy(int)} which
     * caps the result at {@code maxEnergy} (default 100).  Energy must never
     * exceed 100 regardless of how many grow() calls are made.
     */
    @Test
    void plant_energyIsCappedAtMaxEnergy() {
        Plant plant = new Plant(0, 0, 98, 5.0);  // 2 below cap, growthRate = 5

        plant.grow();   // would produce 103 without the cap

        assertEquals(100, plant.getEnergy(),
                "Plant energy must be capped at maxEnergy (100), not exceed it");
    }

    /**
     * Multiple grow() calls must accumulate energy correctly up to the cap.
     */
    @Test
    void plant_energyAccumulatesAcrossMultipleGrowCalls() {
        Plant plant = new Plant(0, 0, 0, 2.0);

        for (int i = 0; i < 10; i++) {
            plant.grow();   // +2 per call
        }

        assertEquals(20, plant.getEnergy(),
                "After 10 grow() calls with rate 2.0, energy must be 0 + (10 × 2) = 20");
        assertTrue(plant.isAlive(), "Plant must remain alive during growth");
    }
}
