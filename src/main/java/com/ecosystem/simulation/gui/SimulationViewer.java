package com.ecosystem.simulation.gui;

import com.ecosystem.simulation.entities.Herbivore;
import com.ecosystem.simulation.entities.Plant;
import com.ecosystem.simulation.entities.Predator;
import com.ecosystem.simulation.simulation.World;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main window for viewing the ecosystem simulation in real time.
 * Displays entities as colored circles and refreshes every 500ms.
 */
public class SimulationViewer extends JFrame {
    
    private WorldPanel worldPanel;
    private Timer refreshTimer;
    
    public SimulationViewer(World world) {
        setTitle("Ecosystem Simulation Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        worldPanel = new WorldPanel(world);
        
        add(worldPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Start auto-refresh timer (500ms)
        startRefreshTimer();
    }
    
    private void startRefreshTimer() {
        refreshTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worldPanel.repaint();
            }
        });
        refreshTimer.start();
    }
    
    public void stop() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
    
    public void setWorld(World world) {
        worldPanel.setWorld(world);
        pack();
    }
    
    /**
     * Main method to launch the viewer with a demo world.
     * Creates a sample world with entities for demonstration.
     */
    public static void main(String[] args) {
        // Create demo world
        World demoWorld = new World(50, 50);
        
        // Add demo entities
        for (int i = 0; i < 3; i++) {
            Predator predator = new Predator(
                i * 10 + 5, i * 10 + 5, 50, 2.0, 7
            );
            demoWorld.addEntity(predator);
        }
        
        for (int i = 0; i < 10; i++) {
            Herbivore herbivore = new Herbivore(
                i * 5 + 2, i * 5 + 2, 40, 1.5, 5
            );
            demoWorld.addEntity(herbivore);
        }
        
        for (int i = 0; i < 20; i++) {
            Plant plant = new Plant(
                i * 3 + 1, i * 3 + 1, 30, 2.0
            );
            demoWorld.addEntity(plant);
        }
        
        // Create and show viewer
        SimulationViewer viewer = new SimulationViewer(demoWorld);
        viewer.setVisible(true);
    }
}