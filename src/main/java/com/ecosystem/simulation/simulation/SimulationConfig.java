package com.ecosystem.simulation.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Typed access to externally configurable simulation parameters, loaded from
 * {@code config/simulation.properties} (bootstrapped from the bundled
 * classpath default on first run, exactly like {@link com.ecosystem.simulation.rules.RuleRepository}).
 *
 * <p>Every value returned here is genuinely consumed: species defaults flow
 * into {@link com.ecosystem.simulation.entities.EntityFactory}, which is used
 * for both the initial population <em>and</em> every reproduced offspring, and
 * world/environment values flow into {@code World}/{@code Environment}
 * construction. A parameter is only exposed here if the model actually uses it
 * end to end — see the project report's configuration-propagation section.</p>
 */
public class SimulationConfig {

    private final Properties props = new Properties();

    private SimulationConfig() {
    }

    /** No-I/O built-in defaults, matching the original hardcoded values exactly. Used by tests and as the ultimate fallback. */
    public static SimulationConfig defaults() {
        return new SimulationConfig();
    }

    /** Bootstraps the active file from the bundled default if absent, then loads it. Never throws — falls back to built-in defaults on any I/O problem. */
    public static SimulationConfig loadOrDefault(Path activeFile, String classpathSeedResource) {
        SimulationConfig config = new SimulationConfig();
        try {
            if (!Files.exists(activeFile)) {
                Path parent = activeFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (InputStream in = SimulationConfig.class.getResourceAsStream(classpathSeedResource)) {
                    if (in != null) {
                        Files.copy(in, activeFile);
                    }
                }
            }
            if (Files.exists(activeFile)) {
                try (InputStream in = Files.newInputStream(activeFile)) {
                    config.props.load(in);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: could not load simulation.properties, using built-in defaults: " + e.getMessage());
        }
        return config;
    }

    // ── world / environment ──────────────────────────────────────────────────
    public int worldWidth() { return getInt("world.width", 50); }
    public int worldHeight() { return getInt("world.height", 50); }
    public int environmentFoodLevel() { return getInt("environment.foodLevel", 50); }
    public int environmentMaxFoodLevel() { return getInt("environment.maxFoodLevel", 100); }
    public int environmentFoodRegenRate() { return getInt("environment.foodRegenRate", 5); }
    public double environmentTemperature() { return getDouble("environment.temperature", 20.0); }

    // ── initial population ───────────────────────────────────────────────────
    public int initialPredators() { return getInt("population.predator", 3); }
    public int initialHerbivores() { return getInt("population.herbivore", 15); }
    public int initialPlants() { return getInt("population.plant", 40); }

    // ── predator ──────────────────────────────────────────────────────────────
    public int predatorEnergy() { return getInt("predator.energy", 80); }
    public double predatorSpeed() { return getDouble("predator.speed", 2.0); }
    public int predatorAttackPower() { return getInt("predator.attackPower", 7); }
    public int predatorVisionRange() { return getInt("predator.visionRange", 10); }
    public int predatorMaxAge() { return getInt("predator.maxAge", 140); }
    public int predatorReproductionCooldown() { return getInt("predator.reproductionCooldown", 40); }
    public int predatorEnergyConsumption() { return getInt("predator.energyConsumption", 2); }
    public int predatorCap() { return getInt("predator.cap", 5); }

    // ── herbivore ─────────────────────────────────────────────────────────────
    public int herbivoreEnergy() { return getInt("herbivore.energy", 50); }
    public double herbivoreSpeed() { return getDouble("herbivore.speed", 1.5); }
    public int herbivoreDefensePower() { return getInt("herbivore.defensePower", 12); }
    public int herbivoreVisionRange() { return getInt("herbivore.visionRange", 18); }
    public int herbivoreMaxAge() { return getInt("herbivore.maxAge", 80); }
    public int herbivoreReproductionCooldown() { return getInt("herbivore.reproductionCooldown", 20); }
    public int herbivoreEnergyConsumption() { return getInt("herbivore.energyConsumption", 1); }
    public int herbivoreCap() { return getInt("herbivore.cap", 22); }

    // ── plant ─────────────────────────────────────────────────────────────────
    public int plantEnergy() { return getInt("plant.energy", 50); }
    public double plantGrowthRate() { return getDouble("plant.growthRate", 2.0); }
    public int plantMaxAge() { return getInt("plant.maxAge", 100); }
    public int plantReproductionCooldown() { return getInt("plant.reproductionCooldown", 12); }
    public int plantCap() { return getInt("plant.cap", 120); }

    // ── simulation control ────────────────────────────────────────────────────
    /** Used by {@code HeadlessRunner} as the stopping condition; the interactive GUI runs indefinitely by design. */
    public int maxSteps() { return getInt("simulation.maxSteps", 200); }

    private int getInt(String key, int def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private double getDouble(String key, double def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return def; }
    }
}
