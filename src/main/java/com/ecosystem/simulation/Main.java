package com.ecosystem.simulation;

import com.ecosystem.simulation.gui.MainWindow;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main entry point for the Ecosystem Simulation.
 * Launches the integrated desktop application with GUI.
 * 
 * @author Ecosystem Simulation Team
 * @version 1.0
 */
public class Main {
    
    public static void main(String[] args) {
        // Launch the integrated desktop application
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);
                mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainWindow.toFront();
                mainWindow.requestFocus();
            }
        });
    }
}