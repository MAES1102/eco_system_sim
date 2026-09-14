package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.simulation.World;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that displays real-time simulation statistics.
 * Shows population counts and current time step.
 */
public class StatisticsPanel extends JPanel {

    // Same hues WorldPanel uses for predator/herbivore/plant shapes, darkened
    // for legible text on a white background (pure Color.GREEN especially is
    // low-contrast as text, even though it reads fine as a filled shape).
    private static final Color PREDATOR_COLOR = Color.RED.darker();
    private static final Color HERBIVORE_COLOR = Color.GREEN.darker();
    private static final Color PLANT_COLOR = Color.BLUE.darker();

    private JLabel predatorLabel;
    private JLabel herbivoreLabel;
    private JLabel plantLabel;
    private JLabel totalLabel;
    private JLabel timeStepLabel;

    public StatisticsPanel() {
        setLayout(new GridLayout(5, 1, 5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Statistics"),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));

        predatorLabel = new JLabel("Predators: 0");
        herbivoreLabel = new JLabel("Herbivores: 0");
        plantLabel = new JLabel("Plants: 0");
        totalLabel = new JLabel("Total Population: 0");
        timeStepLabel = new JLabel("Time Step: 0");

        predatorLabel.setForeground(PREDATOR_COLOR);
        herbivoreLabel.setForeground(HERBIVORE_COLOR);
        plantLabel.setForeground(PLANT_COLOR);

        Font bold = predatorLabel.getFont().deriveFont(Font.BOLD);
        predatorLabel.setFont(bold);
        herbivoreLabel.setFont(bold);
        plantLabel.setFont(bold);
        totalLabel.setFont(bold);

        add(predatorLabel);
        add(herbivoreLabel);
        add(plantLabel);
        add(totalLabel);
        add(timeStepLabel);
    }
    
    /**
     * Refreshes the population labels using the live entity list from the World.
     *
     * <p>Using {@link World#countAliveByType} instead of
     * {@link com.ecosystem.simulation.statistics.Statistics#getPopulation} is
     * intentional: the Statistics population map is an event-counter that is
     * incremented on births and decremented on natural-death removals, but it
     * is <em>never decremented</em> when a plant is consumed by a herbivore
     * (because {@code Herbivore.graze()} calls {@code world.removeEntity()}
     * directly, bypassing {@code SimulationEngine.removeDeadEntities()}).
     * Over time that mismatch causes the statistics counter to report hundreds
     * of phantom plants.  The World list is always authoritative.</p>
     *
     * @param world the live simulation world
     */
    public void updateStatistics(World world) {
        int predators = world.countAliveByType("Predator");
        int herbivores = world.countAliveByType("Herbivore");
        int plants = world.countAliveByType("Plant");
        int total = predators + herbivores + plants;

        predatorLabel.setText("Predators: " + predators);
        herbivoreLabel.setText("Herbivores: " + herbivores);
        plantLabel.setText("Plants: " + plants);
        totalLabel.setText("Total Population: " + total);
    }
    
    public void updateTimeStep(int timeStep) {
        timeStepLabel.setText("Time Step: " + timeStep);
    }
}