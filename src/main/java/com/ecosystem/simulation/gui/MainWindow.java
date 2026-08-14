package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.simulation.SimulationEngine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window for the integrated ecosystem simulation application.
 * Integrates viewer, statistics, controls, and rule builder in one window.
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class MainWindow extends JFrame {
    
    private ViewerPanel viewerPanel;
    private StatisticsPanel statisticsPanel;
    private ControlPanel controlPanel;
    RuleBuilderPanel ruleBuilderPanel;
    ActiveRulesPanel activeRulesPanel;
    EventLogPanel eventLogPanel;
    
    private SimulationEngine simulationEngine;
    private Timer simulationTimer;
    private Timer viewerTimer;
    private Timer statisticsTimer;
    
    private boolean isPaused;
    private long stepDelay;
    
    public MainWindow() {
        setTitle("Ecosystem Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create simulation engine
        simulationEngine = new SimulationEngine(50, 50);

        // Create panels — eventLogPanel must exist before being passed to the engine
        viewerPanel = new ViewerPanel(simulationEngine.getWorld());
        statisticsPanel = new StatisticsPanel();
        controlPanel = new ControlPanel();
        ruleBuilderPanel = new RuleBuilderPanel();
        activeRulesPanel = new ActiveRulesPanel();
        eventLogPanel = new EventLogPanel();

        // Wire the listener now that eventLogPanel is constructed
        simulationEngine.setSimulationEventListener(eventLogPanel);

        // Initialize with demo entities
        List<com.ecosystem.simulation.entities.Entity> entities = createInitialEntities();
        simulationEngine.initialize(entities);

        try {
            simulationEngine.getRuleEngine().loadRules("src/main/resources/rules.txt");
        } catch (Exception e) {
            System.err.println("Warning: Could not load rules: " + e.getMessage());
        }
        
        // Setup control panel actions
        setupControlActions();
        
        // Setup rule builder action
        ruleBuilderPanel.setCreateActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ruleBuilderPanel.createRule()) {
                    // Success dialog is shown by RuleBuilderPanel.createRule() itself.
                    // Reload rules into the engine so the new rule takes effect immediately.
                    try {
                        simulationEngine.getRuleEngine().loadRules("src/main/resources/rules.txt");
                        // Refresh active rules display
                        refreshActiveRules();
                        // Log rule creation event
                        eventLogPanel.logEvent("New rule created");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainWindow.this, "Failed to reload rules: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                // On failure, RuleBuilderPanel.createRule() already shows a
                // specific validation message, so no generic dialog is needed here.
            }
        });
        
        // Layout
        setLayout(new BorderLayout());
        
        // Center: Viewer (expands to use available space)
        add(viewerPanel, BorderLayout.CENTER);
        
        // Right side panel for statistics, rule builder, and active rules
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Top of right panel: Statistics | Rule Builder | Active Rules
        JPanel middlePanel = new JPanel(new GridLayout(1, 3, 5, 5));
        // Height increased from 430 → 460 to give the RuleBuilderPanel enough room
        // for its compact banner + four form groups + preview + two-row button area.
        middlePanel.setPreferredSize(new Dimension(600, 460));
        middlePanel.add(statisticsPanel);
        middlePanel.add(ruleBuilderPanel);
        middlePanel.add(activeRulesPanel);
        rightPanel.add(middlePanel, BorderLayout.NORTH);
        
        // Bottom of right panel: Event Log
        rightPanel.add(eventLogPanel, BorderLayout.CENTER);
        
        add(rightPanel, BorderLayout.EAST);
        
        // Bottom: Controls
        add(controlPanel, BorderLayout.SOUTH);
        
        // Initialize active rules display
        refreshActiveRules();
        
        // Set up rule execution listener for event logging
        simulationEngine.getRuleEngine().setRuleExecutionListener(new RuleEngine.RuleExecutionListener() {
            @Override
            public void onRuleExecuted(String ruleDescription, String entityType) {
                eventLogPanel.logRuleTriggered(ruleDescription, entityType);
            }
        });
        
        // Set minimum size to ensure all components are visible
        setMinimumSize(new Dimension(800, 600));
        
        // Open maximized by default
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Keep window resizable
        setResizable(true);
        
        // Initialize state
        isPaused = true;
        stepDelay = 1000;  // Default 1x speed
        
        // Start timers
        startTimers();
    }
    
    private void setupControlActions() {
        controlPanel.setStartActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSimulation();
            }
        });
        
        controlPanel.setPauseActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseSimulation();
            }
        });
        
        controlPanel.setResetActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetSimulation();
            }
        });
        
        controlPanel.setSpeedActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeSpeed(controlPanel.getSelectedSpeed());
            }
        });
    }
    
    private void startTimers() {
        // Simulation timer
        simulationTimer = new Timer((int) stepDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    simulationEngine.step();
                }
            }
        });
        
        // Viewer timer (500ms refresh)
        viewerTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewerPanel.refresh();
            }
        });
        
        // Statistics timer (500ms refresh)
        statisticsTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatistics();
            }
        });
        
        // Start simulation timer
        simulationTimer.start();
        viewerTimer.start();
        statisticsTimer.start();
    }
    
    private void startSimulation() {
        isPaused = false;
        controlPanel.setRunningState(true);
    }
    
    private void pauseSimulation() {
        isPaused = true;
        controlPanel.setRunningState(false);
    }
    
    private void resetSimulation() {
        stopTimers();
        
        // Create new simulation engine and re-wire the existing event log panel
        simulationEngine = new SimulationEngine(50, 50);
        simulationEngine.setSimulationEventListener(eventLogPanel);
        List<com.ecosystem.simulation.entities.Entity> entities = createInitialEntities();
        simulationEngine.initialize(entities);
        
        try {
            simulationEngine.getRuleEngine().loadRules("src/main/resources/rules.txt");
        } catch (Exception e) {
            System.err.println("Warning: Could not load rules: " + e.getMessage());
        }
        
        viewerPanel.setWorld(simulationEngine.getWorld());
        isPaused = true;
        controlPanel.setRunningState(false);
        refreshActiveRules();
        
        // Log simulation reset event
        eventLogPanel.logEvent("Simulation reset");
        
        startTimers();
    }
    
    private void changeSpeed(String speed) {
        stopTimers();
        
        switch (speed) {
            case "0.5x":
                stepDelay = 2000;
                break;
            case "1x":
                stepDelay = 1000;
                break;
            case "2x":
                stepDelay = 500;
                break;
            case "5x":
                stepDelay = 200;
                break;
            default:
                stepDelay = 1000;
        }
        
        startTimers();
    }
    
    private void stopTimers() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        if (viewerTimer != null) {
            viewerTimer.stop();
        }
        if (statisticsTimer != null) {
            statisticsTimer.stop();
        }
    }
    
    private void updateStatistics() {
        statisticsPanel.updateStatistics(simulationEngine.getWorld());
        statisticsPanel.updateTimeStep(simulationEngine.getTimeStep());
    }
    
    private void refreshActiveRules() {
        activeRulesPanel.updateRules(simulationEngine.getRuleEngine().getRules());
    }
    
    private List<com.ecosystem.simulation.entities.Entity> createInitialEntities() {
        List<com.ecosystem.simulation.entities.Entity> entities = new ArrayList<>();
        
        // Create 3 predators with random positions
        for (int i = 0; i < 3; i++) {
            Predator predator = new Predator(
                randomPosition(50), randomPosition(50), 80, 2.0, 7
            );
            entities.add(predator);
        }
        
        // Create 15 herbivores with random positions
        for (int i = 0; i < 15; i++) {
            Herbivore herbivore = new Herbivore(
                randomPosition(50), randomPosition(50), 50, 1.5, 12
            );
            entities.add(herbivore);
        }
        
        // Create 40 plants with random positions
        for (int i = 0; i < 40; i++) {
            Plant plant = new Plant(
                randomPosition(50), randomPosition(50), 50, 2.0
            );
            entities.add(plant);
        }
        
        return entities;
    }
    
    private int randomPosition(int max) {
        return (int)(Math.random() * max);
    }
    
    @Override
    public void dispose() {
        stopTimers();
        super.dispose();
    }
}