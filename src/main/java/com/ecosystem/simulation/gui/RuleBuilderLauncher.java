package com.ecosystem.simulation.gui;

import javax.swing.SwingUtilities;

/**
 * Launcher class for the Rule Builder GUI.
 */
public class RuleBuilderLauncher {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RuleBuilderWindow window = new RuleBuilderWindow();
                window.setVisible(true);
            }
        });
    }
}