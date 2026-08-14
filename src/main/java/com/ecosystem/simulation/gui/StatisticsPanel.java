package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.simulation.World;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that displays real-time simulation statistics.
 * Shows population counts and current time step.
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class StatisticsPanel extends JPanel {
    
    private JLabel predatorLabel;
    private JLabel herbivoreLabel;
    private JLabel plantLabel;
    private JLabel totalLabel;
    private JLabel timeStepLabel;
    
    public StatisticsPanel() {
        setLayout(new GridLayout(5, 1, 5, 5));
        setBorder(BorderFactory.createTitledBorder("Statistics"));
        
        predatorLabel = new JLabel("Predators: 0");
        herbivoreLabel = new JLabel("Herbivores: 0");
        plantLabel = new JLabel("Plants: 0");
        totalLabel = new JLabel("Total Population: 0");
        timeStepLabel = new JLabel("Time Step: 0");
        
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