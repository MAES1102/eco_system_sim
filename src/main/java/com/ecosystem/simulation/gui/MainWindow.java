package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.rules.RuleEngine;
import com.ecosystem.simulation.rules.RuleParseException;
import com.ecosystem.simulation.rules.RuleRepository;
import com.ecosystem.simulation.simulation.SimulationConfig;
import com.ecosystem.simulation.simulation.SimulationEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Main window for the integrated ecosystem simulation application.
 * Integrates viewer, statistics, controls, rule editor, and event log in one window.
 */
public class MainWindow extends JFrame {

    private static final Path RULES_FILE = Path.of("config", "rules.txt");
    private static final Path CONFIG_FILE = Path.of("config", "simulation.properties");

    private ViewerPanel viewerPanel;
    private StatisticsPanel statisticsPanel;
    private ControlPanel controlPanel;
    private RuleEditorPanel ruleEditorPanel;
    private ActiveRulesPanel activeRulesPanel;
    private EventLogPanel eventLogPanel;

    private SimulationEngine simulationEngine;
    private RuleRepository ruleRepository;
    private Timer simulationTimer;
    private Timer viewerTimer;
    private Timer statisticsTimer;

    private boolean isPaused;
    private long stepDelay;

    public MainWindow() {
        setTitle("Ecosystem Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SimulationConfig config = SimulationConfig.loadOrDefault(CONFIG_FILE, "/simulation.properties");

        simulationEngine = new SimulationEngine(config);

        viewerPanel = new ViewerPanel(simulationEngine.getWorld());
        statisticsPanel = new StatisticsPanel();
        controlPanel = new ControlPanel();
        activeRulesPanel = new ActiveRulesPanel();
        eventLogPanel = new EventLogPanel();

        simulationEngine.setSimulationEventListener(eventLogPanel);

        simulationEngine.initialize();

        ruleRepository = new RuleRepository(simulationEngine.getRuleEngine(), RULES_FILE, "/rules.txt");
        try {
            ruleRepository.loadActive();
        } catch (IOException | RuleParseException e) {
            System.err.println("Warning: could not load active rules: " + e.getMessage());
        }

        ruleEditorPanel = new RuleEditorPanel(ruleRepository);
        ruleEditorPanel.setOnChanged(() -> {
            refreshActiveRules();
            eventLogPanel.logEvent("Rules updated (" + ruleRepository.getEngine().getRuleCount() + " active)");
        });

        setupControlActions();

        setLayout(new BorderLayout());

        add(viewerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());

        JPanel middlePanel = new JPanel(new GridLayout(1, 3, 5, 5));
        middlePanel.setPreferredSize(new Dimension(600, 460));
        middlePanel.add(statisticsPanel);
        middlePanel.add(ruleEditorPanel);
        middlePanel.add(activeRulesPanel);
        rightPanel.add(middlePanel, BorderLayout.NORTH);

        rightPanel.add(eventLogPanel, BorderLayout.CENTER);

        add(rightPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        refreshActiveRules();

        simulationEngine.getRuleEngine().setRuleExecutionListener(new RuleEngine.RuleExecutionListener() {
            @Override
            public void onRuleExecuted(String ruleDescription, String entityType) {
                eventLogPanel.logRuleTriggered(ruleDescription, entityType);
            }
        });

        setMinimumSize(new Dimension(800, 600));
        setResizable(true);

        isPaused = true;
        stepDelay = 1000;

        startTimers();
    }

    private void setupControlActions() {
        controlPanel.setStartActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { startSimulation(); }
        });
        controlPanel.setPauseActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { pauseSimulation(); }
        });
        controlPanel.setResetActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { resetSimulation(); }
        });
        controlPanel.setSpeedActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { changeSpeed(controlPanel.getSelectedSpeed()); }
        });
    }

    private void startTimers() {
        simulationTimer = new Timer((int) stepDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isPaused) {
                    simulationEngine.step();
                }
            }
        });

        viewerTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { viewerPanel.refresh(); }
        });

        statisticsTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { updateStatistics(); }
        });

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

        SimulationConfig config = SimulationConfig.loadOrDefault(CONFIG_FILE, "/simulation.properties");
        simulationEngine = new SimulationEngine(config);
        simulationEngine.setSimulationEventListener(eventLogPanel);
        simulationEngine.initialize();

        ruleRepository = new RuleRepository(simulationEngine.getRuleEngine(), RULES_FILE, "/rules.txt");
        try {
            ruleRepository.loadActive();
        } catch (IOException | RuleParseException e) {
            System.err.println("Warning: could not load active rules: " + e.getMessage());
        }
        simulationEngine.getRuleEngine().setRuleExecutionListener(new RuleEngine.RuleExecutionListener() {
            @Override
            public void onRuleExecuted(String ruleDescription, String entityType) {
                eventLogPanel.logRuleTriggered(ruleDescription, entityType);
            }
        });

        viewerPanel.setWorld(simulationEngine.getWorld());
        isPaused = true;
        controlPanel.setRunningState(false);
        refreshActiveRules();

        eventLogPanel.logEvent("Simulation reset");

        startTimers();
    }

    private void changeSpeed(String speed) {
        stopTimers();

        switch (speed) {
            case "0.5x": stepDelay = 2000; break;
            case "1x": stepDelay = 1000; break;
            case "2x": stepDelay = 500; break;
            case "5x": stepDelay = 200; break;
            default: stepDelay = 1000;
        }

        startTimers();
    }

    private void stopTimers() {
        if (simulationTimer != null) simulationTimer.stop();
        if (viewerTimer != null) viewerTimer.stop();
        if (statisticsTimer != null) statisticsTimer.stop();
    }

    private void updateStatistics() {
        statisticsPanel.updateStatistics(simulationEngine.getWorld());
        statisticsPanel.updateTimeStep(simulationEngine.getTimeStep());
    }

    private void refreshActiveRules() {
        activeRulesPanel.updateRules(simulationEngine.getRuleEngine().getRules());
    }

    @Override
    public void dispose() {
        stopTimers();
        super.dispose();
    }
}
