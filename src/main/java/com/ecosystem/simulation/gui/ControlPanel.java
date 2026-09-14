package com.ecosystem.simulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel with simulation control buttons and speed selector.
 * Allows starting, pausing, and resetting the simulation.
 */
public class ControlPanel extends JPanel {
    
    private JButton startButton;
    private JButton pauseButton;
    private JButton resetButton;
    private JButton settingsButton;
    private JComboBox<String> speedCombo;

    private ActionListener startActionListener;
    private ActionListener pauseActionListener;
    private ActionListener resetActionListener;
    private ActionListener settingsActionListener;
    private ActionListener speedActionListener;

    public ControlPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Controls"),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));

        // Create components
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        resetButton = new JButton("Reset");
        settingsButton = new JButton("Settings...");
        settingsButton.setToolTipText("Edit population/species/environment settings without touching a file.");

        String[] speeds = {"0.5x", "1x", "2x", "5x"};
        speedCombo = new JComboBox<>(speeds);
        speedCombo.setSelectedIndex(1);  // Default to 1x

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(settingsButton);
        
        // Speed panel
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        speedPanel.add(new JLabel("Speed:"));
        speedPanel.add(speedCombo);
        
        add(buttonPanel, BorderLayout.CENTER);
        add(speedPanel, BorderLayout.SOUTH);
    }
    
    public void setStartActionListener(ActionListener listener) {
        if (startActionListener != null) {
            startButton.removeActionListener(startActionListener);
        }
        startActionListener = listener;
        startButton.addActionListener(listener);
    }
    
    public void setPauseActionListener(ActionListener listener) {
        if (pauseActionListener != null) {
            pauseButton.removeActionListener(pauseActionListener);
        }
        pauseActionListener = listener;
        pauseButton.addActionListener(listener);
    }
    
    public void setResetActionListener(ActionListener listener) {
        if (resetActionListener != null) {
            resetButton.removeActionListener(resetActionListener);
        }
        resetActionListener = listener;
        resetButton.addActionListener(listener);
    }
    
    public void setSettingsActionListener(ActionListener listener) {
        if (settingsActionListener != null) {
            settingsButton.removeActionListener(settingsActionListener);
        }
        settingsActionListener = listener;
        settingsButton.addActionListener(listener);
    }

    public void setSpeedActionListener(ActionListener listener) {
        if (speedActionListener != null) {
            speedCombo.removeActionListener(speedActionListener);
        }
        speedActionListener = listener;
        speedCombo.addActionListener(listener);
    }
    
    public String getSelectedSpeed() {
        return (String) speedCombo.getSelectedItem();
    }
    
    public void setRunningState(boolean running) {
        startButton.setEnabled(!running);
        pauseButton.setEnabled(running);
    }
}