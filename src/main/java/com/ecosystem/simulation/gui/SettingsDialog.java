package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.simulation.SimulationConfig;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Point-and-click editor for every value in {@code config/simulation.properties} —
 * the customization the user asked for that does not require writing anything.
 * Every spinner below mirrors one existing {@link SimulationConfig} getter, so
 * this dialog exposes nothing the model does not already genuinely consume;
 * "Apply &amp; Reset" writes a fresh properties file and hands control back to
 * the caller to reload it, reusing the same {@code SimulationConfig.loadOrDefault}
 * + reset path the app already uses, rather than inventing a second way to
 * apply configuration.
 */
public class SettingsDialog extends JDialog {

    private final Path targetFile;
    private final Map<String, JSpinner> spinners = new LinkedHashMap<>();
    private boolean applied = false;

    public SettingsDialog(Frame owner, SimulationConfig config, Path targetFile) {
        super(owner, "Simulation Settings", true);
        this.targetFile = targetFile;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("World", buildWorldTab(config));
        tabs.addTab("Population", buildPopulationTab(config));
        tabs.addTab("Predator", buildPredatorTab(config));
        tabs.addTab("Herbivore", buildHerbivoreTab(config));
        tabs.addTab("Plant", buildPlantTab(config));
        tabs.addTab("Control", buildControlTab(config));

        JButton applyBtn = new JButton("Apply & Reset");
        JButton cancelBtn = new JButton("Cancel");
        applyBtn.setToolTipText("Writes these values to config/simulation.properties and resets the simulation with them.");
        cancelBtn.setToolTipText("Closes without changing anything.");
        applyBtn.addActionListener(e -> doApply());
        cancelBtn.addActionListener(e -> dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttonRow.add(cancelBtn);
        buttonRow.add(applyBtn);

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(buttonRow, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(420, 420));
        pack();
        setLocationRelativeTo(owner);
    }

    /** True if "Apply & Reset" completed successfully (the caller should reload config and reset). */
    public boolean wasApplied() {
        return applied;
    }

    private JPanel buildWorldTab(SimulationConfig c) {
        JPanel panel = newTabPanel(6);
        addIntField(panel, "world.width", "World width", c.worldWidth(), 10, 500, 1);
        addIntField(panel, "world.height", "World height", c.worldHeight(), 10, 500, 1);
        addIntField(panel, "environment.foodLevel", "Environment food level", c.environmentFoodLevel(), 0, 1000, 1);
        addIntField(panel, "environment.maxFoodLevel", "Environment max food level", c.environmentMaxFoodLevel(), 0, 2000, 1);
        addIntField(panel, "environment.foodRegenRate", "Environment food regen rate", c.environmentFoodRegenRate(), 0, 100, 1);
        addDoubleField(panel, "environment.temperature", "Environment temperature", c.environmentTemperature(), -50, 60, 0.5);
        return panel;
    }

    private JPanel buildPopulationTab(SimulationConfig c) {
        JPanel panel = newTabPanel(3);
        addIntField(panel, "population.predator", "Initial predators", c.initialPredators(), 0, 1000, 1);
        addIntField(panel, "population.herbivore", "Initial herbivores", c.initialHerbivores(), 0, 1000, 1);
        addIntField(panel, "population.plant", "Initial plants", c.initialPlants(), 0, 1000, 1);
        return panel;
    }

    private JPanel buildPredatorTab(SimulationConfig c) {
        JPanel panel = newTabPanel(8);
        addIntField(panel, "predator.energy", "Starting energy", c.predatorEnergy(), 1, 1000, 1);
        addDoubleField(panel, "predator.speed", "Speed", c.predatorSpeed(), 0.1, 20.0, 0.1);
        addIntField(panel, "predator.attackPower", "Attack power", c.predatorAttackPower(), 1, 100, 1);
        addIntField(panel, "predator.visionRange", "Vision range", c.predatorVisionRange(), 1, 200, 1);
        addIntField(panel, "predator.maxAge", "Max age", c.predatorMaxAge(), 1, 10000, 1);
        addIntField(panel, "predator.reproductionCooldown", "Reproduction cooldown", c.predatorReproductionCooldown(), 0, 1000, 1);
        addIntField(panel, "predator.energyConsumption", "Energy consumption / tick", c.predatorEnergyConsumption(), 0, 100, 1);
        addIntField(panel, "predator.cap", "Population cap", c.predatorCap(), 0, 10000, 1);
        return panel;
    }

    private JPanel buildHerbivoreTab(SimulationConfig c) {
        JPanel panel = newTabPanel(8);
        addIntField(panel, "herbivore.energy", "Starting energy", c.herbivoreEnergy(), 1, 1000, 1);
        addDoubleField(panel, "herbivore.speed", "Speed", c.herbivoreSpeed(), 0.1, 20.0, 0.1);
        addIntField(panel, "herbivore.defensePower", "Defense power", c.herbivoreDefensePower(), 1, 100, 1);
        addIntField(panel, "herbivore.visionRange", "Vision range", c.herbivoreVisionRange(), 1, 200, 1);
        addIntField(panel, "herbivore.maxAge", "Max age", c.herbivoreMaxAge(), 1, 10000, 1);
        addIntField(panel, "herbivore.reproductionCooldown", "Reproduction cooldown", c.herbivoreReproductionCooldown(), 0, 1000, 1);
        addIntField(panel, "herbivore.energyConsumption", "Energy consumption / tick", c.herbivoreEnergyConsumption(), 0, 100, 1);
        addIntField(panel, "herbivore.cap", "Population cap", c.herbivoreCap(), 0, 10000, 1);
        return panel;
    }

    private JPanel buildPlantTab(SimulationConfig c) {
        JPanel panel = newTabPanel(5);
        addIntField(panel, "plant.energy", "Starting energy", c.plantEnergy(), 1, 1000, 1);
        addDoubleField(panel, "plant.growthRate", "Growth rate", c.plantGrowthRate(), 0.1, 50.0, 0.1);
        addIntField(panel, "plant.maxAge", "Max age", c.plantMaxAge(), 1, 10000, 1);
        addIntField(panel, "plant.reproductionCooldown", "Reproduction cooldown", c.plantReproductionCooldown(), 0, 1000, 1);
        addIntField(panel, "plant.cap", "Population cap", c.plantCap(), 0, 10000, 1);
        return panel;
    }

    private JPanel buildControlTab(SimulationConfig c) {
        JPanel panel = newTabPanel(1);
        addIntField(panel, "simulation.maxSteps", "Max steps (headless runs)", c.maxSteps(), 1, 1000000, 1);
        return panel;
    }

    private static JPanel newTabPanel(int rowCount) {
        JPanel panel = new JPanel(new GridLayout(rowCount, 2, 6, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return panel;
    }

    /** Adds one labeled whole-number spinner and remembers it under {@code key} for {@link #doApply()}. */
    private void addIntField(JPanel panel, String key, String label, int current, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current, min, max, step));
        spinners.put(key, spinner);
        panel.add(new JLabel(label));
        panel.add(spinner);
    }

    /** Adds one labeled decimal spinner and remembers it under {@code key} for {@link #doApply()}. */
    private void addDoubleField(JPanel panel, String key, String label, double current, double min, double max, double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current, min, max, step));
        spinners.put(key, spinner);
        panel.add(new JLabel(label));
        panel.add(spinner);
    }

    private void doApply() {
        Properties out = new Properties();
        for (Map.Entry<String, JSpinner> e : spinners.entrySet()) {
            out.setProperty(e.getKey(), e.getValue().getValue().toString());
        }
        try {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream os = Files.newOutputStream(targetFile)) {
                out.store(os, "Ecosystem Simulation settings -- written by the in-app Settings dialog");
            }
            applied = true;
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save settings: " + ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
