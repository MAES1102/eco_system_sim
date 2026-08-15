package com.ecosystem.simulation;

import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.RuleRepository;
import com.ecosystem.simulation.simulation.SimulationConfig;
import com.ecosystem.simulation.simulation.SimulationEngine;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Headless (no GUI) ecosystem validator and smoke-test entry point.
 *
 * <p>Two modes:</p>
 * <ul>
 *   <li><b>single</b> (default): one run with a per-{@code REPORT_EVERY}-step
 *       diagnostic report, for {@code simulation.maxSteps} steps (from
 *       {@code config/simulation.properties}, default 200).</li>
 *   <li><b>batch</b>: many independent runs with a single aggregate summary.
 *       Usage: {@code HeadlessRunner batch <trials>}</li>
 * </ul>
 */
public class HeadlessRunner {

    private static final Path RULES_FILE = Path.of("config", "rules.txt");
    private static final Path CONFIG_FILE = Path.of("config", "simulation.properties");
    private static final int REPORT_EVERY = 25;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("batch")) {
            int trials = (args.length > 1) ? Integer.parseInt(args[1]) : 10;
            runBatch(trials);
        } else {
            runSingleVerbose();
        }
    }

    private static SimulationEngine buildEngine() {
        SimulationConfig config = SimulationConfig.loadOrDefault(CONFIG_FILE, "/simulation.properties");
        SimulationEngine engine = new SimulationEngine(config);
        engine.setSilent(true);
        engine.initialize();
        try {
            new RuleRepository(engine.getRuleEngine(), RULES_FILE, "/rules.txt").loadActive();
        } catch (IOException | RuleParseException e) {
            System.err.println("Warning: could not load active rules: " + e.getMessage());
        }
        return engine;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH MODE
    // ─────────────────────────────────────────────────────────────────────────

    private static void runBatch(int trials) {
        int surviveAll3 = 0;
        int predExtinct = 0, herbExtinct = 0, plantExtinct = 0;
        long sumPred = 0, sumHerb = 0, sumPlant = 0;
        long sumPredBirths = 0, sumHerbBirths = 0, sumPlantBirths = 0;
        long sumPredDeaths = 0, sumHerbDeaths = 0, sumPlantDeaths = 0;
        long sumSuccessHunts = 0;

        int totalSteps = SimulationConfig.loadOrDefault(CONFIG_FILE, "/simulation.properties").maxSteps();

        for (int t = 0; t < trials; t++) {
            SimulationEngine engine = buildEngine();
            World world = engine.getWorld();
            Statistics stats = engine.getStatistics();

            for (int step = 1; step <= totalSteps; step++) {
                engine.step();
            }

            int pred = world.countAliveByType("Predator");
            int herb = world.countAliveByType("Herbivore");
            int plant = world.countAliveByType("Plant");

            if (pred > 0 && herb > 0 && plant > 0) surviveAll3++;
            if (pred == 0) predExtinct++;
            if (herb == 0) herbExtinct++;
            if (plant == 0) plantExtinct++;

            sumPred += pred; sumHerb += herb; sumPlant += plant;
            sumPredBirths += stats.getPredatorBirths();
            sumHerbBirths += stats.getHerbivoreBirths();
            sumPlantBirths += stats.getPlantBirths();
            sumPredDeaths += stats.getPredatorDeaths();
            sumHerbDeaths += stats.getHerbivoreDeaths();
            sumPlantDeaths += stats.getPlantDeaths();
            sumSuccessHunts += stats.getSuccessfulHunts();
        }

        System.out.println("==========================================================");
        System.out.printf("  BATCH RESULT -- %d trials x %d steps%n", trials, totalSteps);
        System.out.println("==========================================================");
        System.out.printf("  Survive-all-3 @end : %d/%d  (%.0f%%)%n",
                surviveAll3, trials, 100.0 * surviveAll3 / trials);
        System.out.printf("  Extinct @end       : Pred %d/%d   Herb %d/%d   Plant %d/%d%n",
                predExtinct, trials, herbExtinct, trials, plantExtinct, trials);
        System.out.printf("  Avg final pop      : Pred %.1f   Herb %.1f   Plant %.1f%n",
                (double) sumPred / trials, (double) sumHerb / trials, (double) sumPlant / trials);
        System.out.printf("  Avg births         : Pred %.1f   Herb %.1f   Plant %.1f%n",
                (double) sumPredBirths / trials, (double) sumHerbBirths / trials, (double) sumPlantBirths / trials);
        System.out.printf("  Avg deaths         : Pred %.1f   Herb %.1f   Plant %.1f%n",
                (double) sumPredDeaths / trials, (double) sumHerbDeaths / trials, (double) sumPlantDeaths / trials);
        System.out.printf("  Avg successful hunts: %.1f%n", (double) sumSuccessHunts / trials);
        System.out.println("==========================================================");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SINGLE VERBOSE MODE
    // ─────────────────────────────────────────────────────────────────────────

    private static void runSingleVerbose() {
        SimulationEngine engine = buildEngine();
        int totalSteps = engine.getConfig().maxSteps();

        System.out.println("==========================================================");
        System.out.println("  ECOSYSTEM VALIDATION RUN -- " + totalSteps + " steps");
        System.out.println("==========================================================");
        System.out.printf("  Predators: %d   Herbivores: %d   Plants: %d%n",
                engine.getConfig().initialPredators(), engine.getConfig().initialHerbivores(), engine.getConfig().initialPlants());
        System.out.println();

        Statistics stats = engine.getStatistics();
        World world = engine.getWorld();

        int snapSuccessHunts = 0, snapFailedHunts = 0, snapPlantsConsumed = 0;
        int snapPredBirths = 0, snapHerbBirths = 0, snapPlantBirths = 0;
        int snapPredDeaths = 0, snapHerbDeaths = 0, snapPlantDeaths = 0;

        for (int step = 1; step <= totalSteps; step++) {
            engine.step();

            if (step % REPORT_EVERY == 0) {
                int livePred = world.countAliveByType("Predator");
                int liveHerb = world.countAliveByType("Herbivore");
                int livePlant = world.countAliveByType("Plant");

                int dSuccessHunts = stats.getSuccessfulHunts() - snapSuccessHunts;
                int dFailedHunts = stats.getFailedHunts() - snapFailedHunts;
                int dPlantsConsumed = stats.getPlantsConsumed() - snapPlantsConsumed;
                int dPredBirths = stats.getPredatorBirths() - snapPredBirths;
                int dHerbBirths = stats.getHerbivoreBirths() - snapHerbBirths;
                int dPlantBirths = stats.getPlantBirths() - snapPlantBirths;
                int dPredDeaths = stats.getPredatorDeaths() - snapPredDeaths;
                int dHerbDeaths = stats.getHerbivoreDeaths() - snapHerbDeaths;
                int dPlantDeaths = stats.getPlantDeaths() - snapPlantDeaths;

                System.out.println("----------------------------------------------------------");
                System.out.printf("  Step %d (clock=%d)%n", step, engine.getTimeStep());
                System.out.println("----------------------------------------------------------");
                System.out.printf("  Predators:  %3d    Herbivores:  %3d    Plants:  %3d%n", livePred, liveHerb, livePlant);
                System.out.printf("  Temperature: %.1f    Food level: %d%n",
                        world.getEnvironment().getTemperature(), world.getEnvironment().getFoodLevel());
                System.out.println();
                System.out.printf("  Successful hunts:  %4d    Failed hunts:     %4d%n", dSuccessHunts, dFailedHunts);
                System.out.printf("  Plants consumed:   %4d    Plant births:     %4d%n", dPlantsConsumed, dPlantBirths);
                System.out.printf("  Predator births:   %4d    Herbivore births: %4d%n", dPredBirths, dHerbBirths);
                System.out.printf("  Predator deaths:   %4d    Herbivore deaths: %4d    Plant deaths: %4d%n",
                        dPredDeaths, dHerbDeaths, dPlantDeaths);
                System.out.println();

                if (livePred == 0 && liveHerb == 0) {
                    System.out.println("  *** ECOSYSTEM COLLAPSED: all animals extinct ***");
                    System.out.println("  Stopping early.");
                    System.out.println();
                    break;
                }
                if (livePred == 0) System.out.println("  *** WARNING: Predators extinct ***");
                if (liveHerb == 0) System.out.println("  *** WARNING: Herbivores extinct ***");

                snapSuccessHunts = stats.getSuccessfulHunts();
                snapFailedHunts = stats.getFailedHunts();
                snapPlantsConsumed = stats.getPlantsConsumed();
                snapPredBirths = stats.getPredatorBirths();
                snapHerbBirths = stats.getHerbivoreBirths();
                snapPlantBirths = stats.getPlantBirths();
                snapPredDeaths = stats.getPredatorDeaths();
                snapHerbDeaths = stats.getHerbivoreDeaths();
                snapPlantDeaths = stats.getPlantDeaths();
            }
        }

        System.out.println("==========================================================");
        System.out.println("  FINAL SUMMARY");
        System.out.println("==========================================================");
        System.out.printf("  Live population:  Predators %d   Herbivores %d   Plants %d%n",
                world.countAliveByType("Predator"), world.countAliveByType("Herbivore"), world.countAliveByType("Plant"));
        System.out.println();
        System.out.printf("  Total successful hunts:  %d%n", stats.getSuccessfulHunts());
        System.out.printf("  Total failed hunts:      %d%n", stats.getFailedHunts());
        System.out.printf("  Total plants consumed:   %d%n", stats.getPlantsConsumed());
        System.out.println();
        System.out.printf("  Predator  births/deaths: %d / %d%n", stats.getPredatorBirths(), stats.getPredatorDeaths());
        System.out.printf("  Herbivore births/deaths: %d / %d%n", stats.getHerbivoreBirths(), stats.getHerbivoreDeaths());
        System.out.printf("  Plant     births/deaths: %d / %d%n", stats.getPlantBirths(), stats.getPlantDeaths());
        System.out.println("==========================================================");
    }
}
