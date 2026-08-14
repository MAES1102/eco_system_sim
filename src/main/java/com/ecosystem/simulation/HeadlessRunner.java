package com.ecosystem.simulation;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.simulation.SimulationEngine;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless (no GUI) ecosystem validator.
 *
 * <p>Two modes:</p>
 * <ul>
 *   <li><b>single</b> (default): one 200-step run with a per-25-step diagnostic
 *       report. Usage: {@code HeadlessRunner [pred herb plant]}</li>
 *   <li><b>batch</b>: many independent 200-step runs with a single aggregate
 *       summary (survival rate, per-species extinction counts, average final
 *       populations, average births/deaths, average successful hunts). This is
 *       the mode used for systematic parameter-combination search.
 *       Usage: {@code HeadlessRunner batch <trials> [pred herb plant]}</li>
 * </ul>
 *
 * <p>All population counts are read directly from the World entity list
 * ({@code world.countAliveByType}), not from Statistics (which tracks events).</p>
 */
public class HeadlessRunner {

    private static final int TOTAL_STEPS  = 200;
    private static final int REPORT_EVERY = 25;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("batch")) {
            int trials         = (args.length > 1) ? Integer.parseInt(args[1]) : 10;
            int initPredators  = (args.length > 2) ? Integer.parseInt(args[2]) : 3;
            int initHerbivores = (args.length > 3) ? Integer.parseInt(args[3]) : 15;
            int initPlants     = (args.length > 4) ? Integer.parseInt(args[4]) : 40;
            runBatch(trials, initPredators, initHerbivores, initPlants);
        } else {
            int initPredators  = (args.length > 0) ? Integer.parseInt(args[0]) : 3;
            int initHerbivores = (args.length > 1) ? Integer.parseInt(args[1]) : 15;
            int initPlants     = (args.length > 2) ? Integer.parseInt(args[2]) : 40;
            runSingleVerbose(initPredators, initHerbivores, initPlants);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH MODE — aggregate over many trials
    // ─────────────────────────────────────────────────────────────────────────

    private static void runBatch(int trials, int p0, int h0, int l0) {
        int surviveAll3 = 0;
        int predExtinct = 0, herbExtinct = 0, plantExtinct = 0;
        long sumPred = 0, sumHerb = 0, sumPlant = 0;
        long sumPredBirths = 0, sumHerbBirths = 0, sumPlantBirths = 0;
        long sumPredDeaths = 0, sumHerbDeaths = 0, sumPlantDeaths = 0;
        long sumSuccessHunts = 0;

        for (int t = 0; t < trials; t++) {
            SimulationEngine engine = new SimulationEngine(50, 50);
            engine.setSilent(true);
            engine.initialize(createInitialEntities(p0, h0, l0));
            World world = engine.getWorld();
            Statistics stats = engine.getStatistics();

            for (int step = 1; step <= TOTAL_STEPS; step++) {
                engine.step();
            }

            int pred  = world.countAliveByType("Predator");
            int herb  = world.countAliveByType("Herbivore");
            int plant = world.countAliveByType("Plant");

            if (pred > 0 && herb > 0 && plant > 0) surviveAll3++;
            if (pred == 0)  predExtinct++;
            if (herb == 0)  herbExtinct++;
            if (plant == 0) plantExtinct++;

            sumPred += pred;  sumHerb += herb;  sumPlant += plant;
            sumPredBirths  += stats.getPredatorBirths();
            sumHerbBirths  += stats.getHerbivoreBirths();
            sumPlantBirths += stats.getPlantBirths();
            sumPredDeaths  += stats.getPredatorDeaths();
            sumHerbDeaths  += stats.getHerbivoreDeaths();
            sumPlantDeaths += stats.getPlantDeaths();
            sumSuccessHunts += stats.getSuccessfulHunts();
        }

        System.out.println("==========================================================");
        System.out.printf("  BATCH RESULT — %d trials × %d steps (init %d/%d/%d)%n",
                trials, TOTAL_STEPS, p0, h0, l0);
        System.out.println("==========================================================");
        System.out.printf("  Survive-all-3 @200 : %d/%d  (%.0f%%)%n",
                surviveAll3, trials, 100.0 * surviveAll3 / trials);
        System.out.printf("  Extinct @200       : Pred %d/%d   Herb %d/%d   Plant %d/%d%n",
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
    // SINGLE VERBOSE MODE — one run, per-window diagnostics
    // ─────────────────────────────────────────────────────────────────────────

    private static void runSingleVerbose(int initPredators, int initHerbivores, int initPlants) {
        System.out.println("==========================================================");
        System.out.println("  ECOSYSTEM VALIDATION RUN — " + TOTAL_STEPS + " steps");
        System.out.println("==========================================================");
        System.out.println("Initial population:");
        System.out.printf("  Predators: %d   Herbivores: %d   Plants: %d%n",
                initPredators, initHerbivores, initPlants);
        System.out.println();

        SimulationEngine engine = new SimulationEngine(50, 50);
        engine.setSilent(true);

        engine.initialize(createInitialEntities(initPredators, initHerbivores, initPlants));

        Statistics stats = engine.getStatistics();
        World world = engine.getWorld();

        int snapSuccessHunts = 0, snapFailedHunts = 0, snapPlantsConsumed = 0;
        int snapPredBirths = 0, snapHerbBirths = 0, snapPlantBirths = 0;
        int snapPredDeaths = 0, snapHerbDeaths = 0, snapPlantDeaths = 0;

        for (int step = 1; step <= TOTAL_STEPS; step++) {
            engine.step();

            if (step % REPORT_EVERY == 0) {
                int livePred  = world.countAliveByType("Predator");
                int liveHerb  = world.countAliveByType("Herbivore");
                int livePlant = world.countAliveByType("Plant");

                int dSuccessHunts   = stats.getSuccessfulHunts()  - snapSuccessHunts;
                int dFailedHunts    = stats.getFailedHunts()      - snapFailedHunts;
                int dPlantsConsumed = stats.getPlantsConsumed()   - snapPlantsConsumed;
                int dPredBirths     = stats.getPredatorBirths()   - snapPredBirths;
                int dHerbBirths     = stats.getHerbivoreBirths()  - snapHerbBirths;
                int dPlantBirths    = stats.getPlantBirths()      - snapPlantBirths;
                int dPredDeaths     = stats.getPredatorDeaths()   - snapPredDeaths;
                int dHerbDeaths     = stats.getHerbivoreDeaths()  - snapHerbDeaths;
                int dPlantDeaths    = stats.getPlantDeaths()      - snapPlantDeaths;

                System.out.println("----------------------------------------------------------");
                System.out.printf("  Step %d%n", step);
                System.out.println("----------------------------------------------------------");
                System.out.printf("  Predators:  %3d    Herbivores:  %3d    Plants:  %3d%n",
                        livePred, liveHerb, livePlant);
                System.out.println();
                System.out.printf("  Successful hunts:  %4d    Failed hunts:     %4d%n",
                        dSuccessHunts, dFailedHunts);
                System.out.printf("  Plants consumed:   %4d    Plant births:     %4d%n",
                        dPlantsConsumed, dPlantBirths);
                System.out.printf("  Predator births:   %4d    Herbivore births: %4d%n",
                        dPredBirths, dHerbBirths);
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

                snapSuccessHunts   = stats.getSuccessfulHunts();
                snapFailedHunts    = stats.getFailedHunts();
                snapPlantsConsumed = stats.getPlantsConsumed();
                snapPredBirths     = stats.getPredatorBirths();
                snapHerbBirths     = stats.getHerbivoreBirths();
                snapPlantBirths    = stats.getPlantBirths();
                snapPredDeaths     = stats.getPredatorDeaths();
                snapHerbDeaths     = stats.getHerbivoreDeaths();
                snapPlantDeaths    = stats.getPlantDeaths();
            }
        }

        System.out.println("==========================================================");
        System.out.println("  FINAL SUMMARY (cumulative over all steps)");
        System.out.println("==========================================================");
        System.out.printf("  Live population:  Predators %d   Herbivores %d   Plants %d%n",
                world.countAliveByType("Predator"),
                world.countAliveByType("Herbivore"),
                world.countAliveByType("Plant"));
        System.out.println();
        System.out.printf("  Total successful hunts:  %d%n", stats.getSuccessfulHunts());
        System.out.printf("  Total failed hunts:      %d%n", stats.getFailedHunts());
        System.out.printf("  Total plants consumed:   %d%n", stats.getPlantsConsumed());
        System.out.println();
        System.out.printf("  Predator  births/deaths: %d / %d%n",
                stats.getPredatorBirths(), stats.getPredatorDeaths());
        System.out.printf("  Herbivore births/deaths: %d / %d%n",
                stats.getHerbivoreBirths(), stats.getHerbivoreDeaths());
        System.out.printf("  Plant     births/deaths: %d / %d%n",
                stats.getPlantBirths(), stats.getPlantDeaths());
        System.out.println("==========================================================");
    }

    /**
     * Creates the initial population with configurable counts.
     * Entity parameters (energy, speed, attack/defense, growthRate) are unchanged.
     */
    private static List<Entity> createInitialEntities(int predCount, int herbCount, int plantCount) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < predCount; i++) {
            entities.add(new Predator(rnd(50), rnd(50), 80, 2.0, 7));
        }
        for (int i = 0; i < herbCount; i++) {
            entities.add(new Herbivore(rnd(50), rnd(50), 50, 1.5, 12));
        }
        for (int i = 0; i < plantCount; i++) {
            entities.add(new Plant(rnd(50), rnd(50), 50, 2.0));
        }
        return entities;
    }

    private static int rnd(int max) {
        return (int) (Math.random() * max);
    }
}
